package io.levelops.controlplane.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.controlplane.discovery.AgentRegistryService;
import io.levelops.controlplane.discovery.DefaultAgentRegistryService;
import io.levelops.controlplane.discovery.LocalLockRegistry;
import io.levelops.controlplane.discovery.RedisAgentRegistryService;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.clients.EventsRESTClient;
import io.levelops.ingestion.merging.IngestionResultMergingService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.support.locks.ExpirableLockRegistry;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class BaseConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return DefaultObjectMapper.get()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Bean
    public OkHttpClient okHttpClient(@Value("${READ_TIMEOUT_SECONDS:60}") int readTimeoutSeconds,
                                     @Value("${WRITE_TIMEOUT_SECONDS:60}") int writeTimeoutSeconds) {
        return new OkHttpClient().newBuilder()
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public EventsClient eventsClient(OkHttpClient client, ObjectMapper mapper, @Value("${EVENTS_API_URL:http://events-api}") String apiBaseUrl) {
        return new EventsRESTClient(client, mapper, apiBaseUrl);
    }

    @Bean
    public IngestionResultMergingService ingestionResultMergingService() {
        return new IngestionResultMergingService();
    }

    @Bean
    @Profile("!inMemoryCache")
    public JedisConnectionFactory jedisFactory(final @Value("${REDIS_HOST}") String redisHost, final @Value("${REDIS_PORT}") Integer redisPort){
        return new JedisConnectionFactory(new RedisStandaloneConfiguration(redisHost, redisPort));
    }

    @Bean
    @Profile("!inMemoryCache")
    public ExpirableLockRegistry redisLockRegistry(final @Value("${REDIS_LOCK_REGISTRY_KEY:cp_lock_registry_}") String registryKey, 
                                                    final @Value("${REDIS_LOCK_TIMEOUT_MILLIS}") Integer lockTimeOut,
                                                    final RedisConnectionFactory factory){
        return new RedisLockRegistry(factory, registryKey, lockTimeOut);
    }

    @Bean("redisAgentRegistry")
    @Profile("!inMemoryCache")
    public AgentRegistryService agentRegistryService(
                                                    final ObjectMapper mapper,
                                                    final @Value("${agents.registry.timeout_sec:600}") Integer timeoutInSeconds,
                                                    final JedisConnectionFactory redis,
                                                    final ExpirableLockRegistry expirableLockRegistry){
        return new RedisAgentRegistryService(mapper, timeoutInSeconds, redis, expirableLockRegistry);
    }

    @Bean(value = "localAgentRegistry")
    @Profile("inMemoryCache")
    public AgentRegistryService agentRegistryServiceLocal(final @Value("${agents.registry.timeout_sec:600}") Integer timeoutInSeconds){
        return new DefaultAgentRegistryService(timeoutInSeconds);
    }

    @Bean
    @Profile("inMemoryCache")
    public ExpirableLockRegistry localLockRegistry(){
        return new LocalLockRegistry();
    }
}
