package io.levelops.controlplane.discovery;

import com.amazonaws.services.kafkaconnect.model.InternalServerErrorException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.models.AgentHandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import redis.clients.jedis.Jedis;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.support.locks.ExpirableLockRegistry;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Slf4j
public class RedisAgentRegistryService implements AgentRegistryService {
    // private final JedisFactory redis;
    private static final String NS_KEY = "cp_ar_by_id";
    private static final String NS_CONTROLLER_KEY_PREFIX = "cp_ar_by_controller_";
    private static final String NS_LOGS_KEY = "ingestion_logs_agent";
    private static final String LOG_KEY="log";
    private static final String SATELLITE_AGENT="satellite";
    private static final Long VALUE_UPDATED = 0L;
    private static final Object AGENT_REGISTRY_LOCK_KEY = "agent_registry_write";
    private static final Object AGENT_LOGS_LOCK_KEY = "agent_logs_write";
    private static final boolean DO_NOT_INCLUDE_EXPIRED_AGENTS = false;
    private static final boolean INCLUDE_EXPIRED_AGENTS = true;
    private final Integer timeoutSec;
    private final RedisConnectionFactory redisFactory;
    private final ObjectMapper mapper;
    private final ScheduledExecutorService runner;
    private final ExpirableLockRegistry lockRegistry;

    public RedisAgentRegistryService(final ObjectMapper mapper, Integer timeoutSec, final RedisConnectionFactory redis, final ExpirableLockRegistry lockRegistry){
        this.redisFactory = redis;
        this.mapper = mapper;
        this.timeoutSec = timeoutSec;
        this.lockRegistry = lockRegistry;
        this.runner = Executors.newScheduledThreadPool(1);
        init(5, 5, TimeUnit.MINUTES);
    }

    public RedisAgentRegistryService(final ObjectMapper mapper, Integer timeoutSec, final RedisConnectionFactory redis, final ExpirableLockRegistry lockRegistry, 
            final Integer expirationCoreThreads, final Integer expirationDelay, final Integer expirationWait, final TimeUnit timeUnit){
        this.redisFactory = redis;
        this.mapper = mapper;
        this.timeoutSec = timeoutSec;
        this.lockRegistry = lockRegistry;
        this.runner = Executors.newScheduledThreadPool(expirationCoreThreads);
        init(expirationDelay, expirationWait, timeUnit);
    }

    public void init(final Integer expirationDelay, final Integer expirationWait, final TimeUnit timeUnit){
        runner.scheduleAtFixedRate( () -> {
            removeExpiredAgents();
        }, expirationDelay, expirationWait, timeUnit);
    }
    
    public List<RegisteredAgent> getAllAgents() {
       return getAllAgents(DO_NOT_INCLUDE_EXPIRED_AGENTS);
    };

    public List<RegisteredAgent> getAllAgents(final boolean includeExpired) {
        try(var redisConnection = redisFactory.getConnection();){
            var redis = (Jedis) redisConnection.getNativeConnection();
            var values = redis.hgetAll(NS_KEY);
            var telemetryLogsMap = redis.hgetAll(NS_LOGS_KEY);
            return values.values().stream().map(entry -> {
                try {
                    return mapper.readValue(entry, RegisteredAgent.class);
                } catch (JsonProcessingException e) {
                    log.error("Unable to convert cache value into a RegisteredAgent object: {}", entry, e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .filter(entry -> includeExpired || !entry.getLastHeartbeat().isBefore(Instant.now().minus(timeoutSec, ChronoUnit.SECONDS)))
            .peek(agent-> restoreLogs(agent.getAgentHandle(),telemetryLogsMap))
            .toList();
        }
    };

    public void registerAgent(AgentHandle agentHandle) {
        if (agentHandle == null || StringUtils.isBlank(agentHandle.getAgentId())){
            throw new IllegalArgumentException("To register a new agent it can't be null nor its id can be null");
        }
        Lock lock = null;
        try{
            lock = lockRegistry.obtain(AGENT_REGISTRY_LOCK_KEY);
            // if the lock can't be acquired, it should wait for a short period of time
            log.debug("acquiring lock to register a new agent...");
            lock.lock();
            log.debug("lock acquired to register a new agent...");
            // 1. Check if the agent has already been registered  
            try(var redisConnection = redisFactory.getConnection();){
                var redis = (Jedis) redisConnection.getNativeConnection();
                var redisAgent = getAgentById(agentHandle.getAgentId());
                RegisteredAgent registeredAgent = null;
                if (redisAgent.isPresent()){
                    log.warn("There is already an agent registered with the id '{}': redis response (without telemetry)='{}'", agentHandle.getAgentId(), redisAgent.get()
                        .toBuilder()
                        .agentHandle(
                            redisAgent.get()
                                .getAgentHandle()
                                .toBuilder()
                                .telemetry(Map.of())
                                .build())
                        .build());
                    throw new IllegalArgumentException("An agent with the same id ('" + agentHandle.getAgentId() + "') already registered.");
                }
                String agentLogs = removeLogsFromAgentHandle(agentHandle);
                // 2. Build the registered agent object
                registeredAgent = RegisteredAgent.builder().agentHandle(agentHandle).lastHeartbeat(Instant.now()).build();
                String registeredAgentStr = null;
                try {
                    registeredAgentStr = mapper.writeValueAsString(registeredAgent);
                } catch (JsonProcessingException e) {
                    log.error("Unable to register agent, it won't be usable: {}", agentHandle, e);
                    throw new InternalServerErrorException("Unable to register agent due to an error converting the agent's data: " + e.getMessage());
                }
                var finalRegisteredAgent = registeredAgentStr;

                // 3. save the agent by id
                var result = redis.hset(NS_KEY, agentHandle.getAgentId(), finalRegisteredAgent);
                if (result == VALUE_UPDATED){
                    log.warn("There was already an entry in the registry for the agent with id '{}'. This could be an issue...", agentHandle.getAgentId());
                }

                // 4. update the agent registry by controllers
                agentHandle.getControllerNames().stream().forEach( controller -> {
                    redis.hset(NS_CONTROLLER_KEY_PREFIX + controller.toLowerCase(), agentHandle.getAgentId(), finalRegisteredAgent);
                });

                //5. persist Logs by AgentId
                if(Objects.nonNull(agentLogs)) {
                    var logsResult = redis.hset(NS_LOGS_KEY, agentHandle.getAgentId(), agentLogs);
                    if (!Objects.equals(logsResult, VALUE_UPDATED)) {
                        log.warn("Agent Logs update not successful for agent '{}', could be an issue", agentHandle.getAgentId());
                    }
                }
            }
        }
        finally{
            if (lock != null) {
                try{
                    lock.unlock();
                }
                catch(IllegalStateException e1){
                    log.error("Trying to release the lock...", e1);
                }
            }
        }
    };

    public Optional<RegisteredAgent> getAgentById(String agentId) {
        if (StringUtils.isBlank(agentId)){
            log.error("Empty agentId lookup...", agentId);
            return Optional.empty();
        }
        agentId = agentId.toLowerCase();
        try(var redisConnection = redisFactory.getConnection();){
            var redis = (Jedis) redisConnection.getNativeConnection();
            var value = redis.hget(NS_KEY, agentId);
            if (StringUtils.isBlank(value) || "nil".equalsIgnoreCase(value) || "null".equalsIgnoreCase(value)){
                log.warn("Agent with id '{}' not present... redis response='{}'", agentId, value);
                return Optional.empty();
            }
            try {
                var redisAgent = mapper.readValue(value, RegisteredAgent.class);
                Optional<RegisteredAgent> registeredAgent = Optional.of(redisAgent).filter(agent -> !isAgentExpired(agent));
                registeredAgent.ifPresent(agent->fetchLogsByAgentHandle(agent.getAgentHandle()));
                return registeredAgent;
            } catch (JsonProcessingException e) {
                log.error("Unable to unmarshall the registeredAgent object from the cache for the id: {}", agentId, e);
                return Optional.empty();
            }
        }
    };

    public Optional<RegisteredAgent> refreshHeartbeatAndTelemetry(AgentHandle agentHandle) {
        var agentOptional = getAgentById(agentHandle.getAgentId());
        if (!agentOptional.isPresent()){
            log.error("Agent with id '{}' not present in the registry. can't update heartbeat..", agentHandle.getAgentId());
            return Optional.empty();
        }
        String agentLogs = removeLogsFromAgentHandle(agentHandle);
        var registeredAgent = agentOptional.get().toBuilder()
                .agentHandle(agentOptional.get().getAgentHandle().toBuilder().telemetry(agentHandle.getTelemetry()).build())
                .lastHeartbeat(Instant.now())
                .build();
        Lock lock = null;
        try{
            lock = lockRegistry.obtain(AGENT_REGISTRY_LOCK_KEY);
            log.debug("acquiring lock to refresh hearbeat...");
            lock.lock();
            log.debug("lock acquired to refresh hearbeat...");
            try(var redisConnection = redisFactory.getConnection();){
                var redis = (Jedis) redisConnection.getNativeConnection();
                try {
                    var registeredAgentStr = mapper.writeValueAsString(registeredAgent);
                    var result = redis.hset(NS_KEY, agentHandle.getAgentId(), registeredAgentStr);
                    if (result != VALUE_UPDATED){
                        log.warn("Agent update not successful for agent '{}', could be an issue", agentHandle.getAgentId());
                    }

                    // 4. update the agent registry by controllers
                    agentHandle.getControllerNames().stream().forEach( controller -> {
                        redis.hset(NS_CONTROLLER_KEY_PREFIX + controller.toLowerCase(), agentHandle.getAgentId(), registeredAgentStr);
                    });

                    //5. persist Logs by AgentId
                    if(Objects.nonNull(agentLogs)) {
                        var logsResult = redis.hset(NS_LOGS_KEY, agentHandle.getAgentId(), agentLogs);
                        if (!Objects.equals(logsResult, VALUE_UPDATED)) {
                            log.warn("Agent Logs update not successful for agent '{}', could be an issue", agentHandle.getAgentId());
                        }
                    }

                    return Optional.of(registeredAgent);
                } catch (JsonProcessingException e) {
                    log.error("Unable to refresh agent: {}", agentHandle, e);
                    return Optional.empty();
                }
            }
        }
        finally{
            if (lock != null) {
                try{
                    lock.unlock();
                }
                catch(IllegalStateException e1){
                    log.error("Trying to release the lock...", e1);
                }
            }
        }
    };

    public List<RegisteredAgent> getAgentsByControllerName(String controllerNameRequest) {
        if (StringUtils.isBlank(controllerNameRequest)){
            log.warn("Agent lookup request by empty controller...", controllerNameRequest);
            return List.of();
        }
        var controllerName = controllerNameRequest.toLowerCase();
        try(var redisConnection = redisFactory.getConnection();){
            var redis = (Jedis) redisConnection.getNativeConnection();
            var values = redis.hgetAll(NS_CONTROLLER_KEY_PREFIX + controllerName);
            if (MapUtils.isEmpty(values)){
                return List.of();
            }
            var telemetryLogsMap = redis.hgetAll(NS_LOGS_KEY);
            return values.values().stream().map(value -> {
                        try {
                            return mapper.readValue(value, RegisteredAgent.class);
                        } catch (JsonProcessingException e) {
                            log.error("[{}] Unalbe to deserialize the registered agent {}", controllerName, value, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(agent -> !isAgentExpired(agent))
                    .peek(agent->restoreLogs(agent.getAgentHandle(),telemetryLogsMap))
                    .toList();
        }
    };

    public void clearAll() {
        Lock lock = null;
        try{
            lock = lockRegistry.obtain(AGENT_REGISTRY_LOCK_KEY);
            log.debug("Trying to acquire lock...");
            lock.lock();
            log.debug("Lock aquired");
            try(var redis = (Jedis)redisFactory.getConnection().getNativeConnection();){
                redis.del(NS_KEY);
                redis.keys(NS_CONTROLLER_KEY_PREFIX).stream().forEach(key -> redis.del(key));
                redis.del(NS_LOGS_KEY);
            }
        }
        finally{
            if (lock != null){
                try{
                    lock.unlock();
                }
                catch(IllegalStateException e1){
                    log.error("Trying to release the lock...", e1);
                }
            }
        }
    };

    private void removeExpiredAgents(){
        log.info("Checking for expired agents...");
        Lock lock = null;
        try{
            lock = lockRegistry.obtain(AGENT_REGISTRY_LOCK_KEY);
            log.debug("acquiring lock to remove expired agents...");
            if(!lock.tryLock()){
                log.info("Lock not acquired, skipping remove expired agents...");
                lock = null;
                return;
            }
            log.debug("lock acquired to remove expired agents...");
            // get all registered agents
            var agents = getAllAgents(INCLUDE_EXPIRED_AGENTS);
            log.info("Agents count: {}", agents.size());
            // calculate expired ids
            var expiredAgentIds = agents.stream().filter(agent -> isAgentExpired(agent))
                .map(entry -> entry.getAgentHandle().getAgentId())
                .toList();
            log.info("Expired agents count: {}", expiredAgentIds.size());
            // remove agents from the main registry
            try(var redisConnection = redisFactory.getConnection();){
                var redis = (Jedis) redisConnection.getNativeConnection();
                redis.hdel(NS_KEY, expiredAgentIds.toArray(new String[0]));
                // get all controller keys
                var byControllers = redis.keys(NS_CONTROLLER_KEY_PREFIX);
                // remove all expired agents from all the controller registries
                byControllers.stream().forEach(controllerKey -> {
                    redis.hdel(controllerKey, expiredAgentIds.toArray(new String[0]));
                });
                redis.hdel(NS_LOGS_KEY,expiredAgentIds.toArray(new String[0]));
            }
        }
        finally{
            if (lock != null) {
                try{
                    lock.unlock();
                }
                catch(IllegalStateException e1){
                    log.error("Trying to release the lock...", e1);
                }
            }
        }
    }

    private Boolean isAgentExpired(RegisteredAgent agent){
        return agent.getLastHeartbeat().isBefore(Instant.now().minus(timeoutSec, ChronoUnit.SECONDS));
    }

    private String removeLogsFromAgentHandle(AgentHandle agentHandle) {
        String agentType = agentHandle.getAgentType();
        if (!agentType.equals(SATELLITE_AGENT)) {
            return null;
        }
        return String.valueOf(agentHandle.getTelemetry().remove(LOG_KEY));
    }

    private void fetchLogsByAgentHandle(AgentHandle agentHandle) {
        String agentType = agentHandle.getAgentType();
        if (!agentType.equals(SATELLITE_AGENT)) {
            return;
        }
        try (var redisConnection = redisFactory.getConnection();) {
            var redis = (Jedis) redisConnection.getNativeConnection();
            String logs = redis.hget(NS_LOGS_KEY, agentHandle.getAgentId());
            agentHandle.getTelemetry().put(LOG_KEY, logs);
        }
    }

    private void restoreLogs(AgentHandle agentHandle,Map<String,String> telemetryLogsMap)
    {
        String agentType = agentHandle.getAgentType();
        if (!agentType.equals(SATELLITE_AGENT)) {
            return;
        }
        String logs = telemetryLogsMap.get(agentHandle.getAgentId());
        agentHandle.getTelemetry().put(LOG_KEY, logs);
    }
}
