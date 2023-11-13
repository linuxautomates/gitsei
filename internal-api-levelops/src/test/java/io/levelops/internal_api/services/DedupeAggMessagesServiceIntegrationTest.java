package io.levelops.internal_api.services;

import org.junit.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class DedupeAggMessagesServiceIntegrationTest {
    @Test
    public void integrationTest() throws InterruptedException {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("127.0.0.1", 6379);
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(config);
        connectionFactory.afterPropertiesSet();
        var connection = connectionFactory.getConnection();
        connection.del("AGG_DEDUPE_LOCK_sid_1a".getBytes(StandardCharsets.UTF_8));
        connection.del("AGG_DEDUPE_LOCK_sid_1b".getBytes(StandardCharsets.UTF_8));
        var service = new DedupeAggMessagesService(connectionFactory, true, 2);
        assertThat(service.shouldRunAgg("sid", "1a")).isEqualTo(true);
        assertThat(service.shouldRunAgg("sid", "1a")).isEqualTo(false);
        Thread.sleep(2000);
        assertThat(service.shouldRunAgg("sid", "1a")).isEqualTo(true);
        assertThat(service.shouldRunAgg("sid", "1b")).isEqualTo(true);
        assertThat(service.shouldRunAgg("sid", "1a")).isEqualTo(false);
        assertThat(service.shouldRunAgg("sid", "1b")).isEqualTo(false);
        assertThat(service.shouldRunAgg("sid", "1a")).isEqualTo(false);
        assertThat(service.shouldRunAgg("sid", "1b")).isEqualTo(false);
        assertThat(service.shouldRunAgg("sid", "1a")).isEqualTo(false);
    }
}