package io.levelops.internal_api.config;

import io.levelops.notification.services.SlackQuestionnaireCacheService;
import io.levelops.notification.services.SlackWorkItemCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;

import java.util.concurrent.TimeUnit;

@Configuration
public class RedisConfig {
    @Bean
    public RedisConnectionFactory redisConnectionFactory(@Value("${REDIS_HOST}") String redisHost,
                                                         @Value("${REDIS_PORT:6379}") int redisPort) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new JedisConnectionFactory(config);
    }

    @Bean
    public SlackQuestionnaireCacheService slackQuestionnaireCacheService(RedisConnectionFactory redisConnectionFactory) {
        SlackQuestionnaireCacheService slackQuestionnaireCacheService = SlackQuestionnaireCacheService.builder().redisConnectionFactory(redisConnectionFactory).build();
        return slackQuestionnaireCacheService;
    }

    @Bean
    public SlackWorkItemCacheService slackWorkItemCacheService(RedisConnectionFactory redisConnectionFactory) {
        SlackWorkItemCacheService slackWorkItemCacheService = SlackWorkItemCacheService.builder().redisConnectionFactory(redisConnectionFactory).build();
        return slackWorkItemCacheService;
    }

    @Bean("tokenRefreshLockRegistry")
    public LockRegistry tokenRefreshLockRegistry(RedisConnectionFactory connectionFactory,
                                                 @Value("${TOKEN_REFRESH_LOCK_EXPIRY_SECONDS:600}") Integer tokenRefreshLockExpirySeconds) {
        return new RedisLockRegistry(connectionFactory, "lock_token_refresh", TimeUnit.SECONDS.toMillis(tokenRefreshLockExpirySeconds));
    }
}
