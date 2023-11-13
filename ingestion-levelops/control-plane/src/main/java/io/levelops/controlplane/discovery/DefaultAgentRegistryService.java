package io.levelops.controlplane.discovery;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.levelops.ingestion.models.AgentHandle;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class DefaultAgentRegistryService implements AgentRegistryService{

    // TODO create a Table or put in redis !
    private final Map<String, RegisteredAgent> agents = Maps.newHashMap();
    private final int timeoutSec;

    @Autowired
    public DefaultAgentRegistryService(@Value("${agents.registry.timeout_sec:600}") int timeoutSec) {
        this.timeoutSec = timeoutSec;
    }

    public void registerAgent(AgentHandle agentHandle) {
        if (agents.containsKey(agentHandle.getAgentId())) {
            throw new IllegalArgumentException("Agent Id already taken");
        }
        agents.put(agentHandle.getAgentId(), RegisteredAgent.builder()
                .agentHandle(agentHandle)
                .lastHeartbeat(Instant.now())
                .build());
        removeExpiredAgents();
    }

    @Override
    public Optional<RegisteredAgent> refreshHeartbeatAndTelemetry(AgentHandle agentHandle) {
        Validate.notBlank(agentHandle.getAgentId(), "agentHandle.getAgentId() cannot be null or empty.");
        if (!agents.containsKey(agentHandle.getAgentId())) {
            return Optional.empty();
        }

        // get cached agent handle
        RegisteredAgent registeredAgent = agents.get(agentHandle.getAgentId());

        RegisteredAgent.RegisteredAgentBuilder builder = registeredAgent.toBuilder();

        // refresh heartbeat
        builder.lastHeartbeat(Instant.now());

        // refresh telemetry (if provided)
        if (MapUtils.isNotEmpty(agentHandle.getTelemetry())) {
            builder.agentHandle(registeredAgent.getAgentHandle().toBuilder()
                    .telemetry(agentHandle.getTelemetry())
                    .build());
        }

        // update cache
        RegisteredAgent updatedAgent = builder.build();
        agents.put(agentHandle.getAgentId(), updatedAgent);

        // clean-up any expired agent
        removeExpiredAgents();

        return Optional.of(updatedAgent);
    }

    public List<RegisteredAgent> getAllAgents() {
        removeExpiredAgents();
        return Lists.newArrayList(agents.values());
    }

    public Optional<RegisteredAgent> getAgentById(String agentId) {
        removeExpiredAgents();
        return Optional.ofNullable(agents.get(agentId));
    }

    public List<RegisteredAgent> getAgentsByControllerName(String controllerName) {
        removeExpiredAgents();
        return agents.values().stream()
                .filter(agent -> agent.getAgentHandle().getControllerNames().contains(controllerName))
                .collect(Collectors.toList());
    }

    private void removeExpiredAgents() {
        agents.values().removeIf(expiredPredicate(Instant.now(), timeoutSec));
    }

    public void clearAll() {
        agents.clear();
    }

    private static Predicate<RegisteredAgent> expiredPredicate(Instant now, int timeoutSec) {
        return (RegisteredAgent agent) -> agent.getLastHeartbeat()
                .isBefore(now.minus(timeoutSec, ChronoUnit.SECONDS));
    }
}
