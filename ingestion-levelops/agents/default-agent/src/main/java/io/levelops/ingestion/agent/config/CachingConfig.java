package io.levelops.ingestion.agent.config;

import io.levelops.ingestion.agent.services.RedisIngestionCachingService;
import io.levelops.ingestion.services.IngestionCachingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

@Configuration
public class CachingConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(@Value("${REDIS_HOST}") String redisHost,
                                                         @Value("${REDIS_PORT:6379}") int redisPort) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new JedisConnectionFactory(config);
    }

    @Bean
    public IngestionCachingService ingestionCachingService(
            RedisConnectionFactory redisConnectionFactory,
            @Value("${INGESTION_CACHE_ENABLED:true}") boolean cacheEnabled,
            @Value("${INGESTION_CACHE_TTL_MINUTES:2880}") long cacheTtlMinutes) {
        return new RedisIngestionCachingService(redisConnectionFactory, cacheEnabled, cacheTtlMinutes);
    }

}
