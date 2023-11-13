package io.levelops.api.config;

import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.notification.services.SlackQuestionnaireCacheService;
import io.levelops.notification.services.SlackWorkItemCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import java.time.Duration;

@Configuration
public class RedisConfig {
    @Bean
    public RedisConnectionFactory redisConnectionFactory(@Value("${REDIS_HOST}") String redisHost,
                                                         @Value("${REDIS_PORT}") int redisPort) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new JedisConnectionFactory(config);
    }

    @Bean
    public SlackQuestionnaireCacheService slackQuestionnaireCacheService(RedisConnectionFactory redisConnectionFactory) {
        return SlackQuestionnaireCacheService.builder().redisConnectionFactory(redisConnectionFactory).build();
    }

    @Bean
    public SlackWorkItemCacheService slackWorkItemCacheService(RedisConnectionFactory redisConnectionFactory) {
        return SlackWorkItemCacheService.builder().redisConnectionFactory(redisConnectionFactory).build();
    }

    @Bean
    public AggCacheService aggCacheService(@Value("${AGG_CACHE_ENABLED:true}") Boolean enabled,
                                           RedisConnectionFactory redisConnectionFactory) {
        return AggCacheService.builder().redisConnectionFactory(redisConnectionFactory)
                .enabled(enabled).build();
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
          .withCacheConfiguration("dashboards",
            RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues())
          .withCacheConfiguration("auth",
            RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .disableCachingNullValues());
    }
}