package io.levelops.internal_api.services;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Log4j2
@Service
public class DedupeAggMessagesService {
    private final boolean enableAggDedupe;
    private final RedisConnectionFactory connectionFactory;
    private final String LOCK_PREFIX = "AGG_DEDUPE_LOCK_";
    private final Integer tokenRefreshLockExpirySeconds;

    @Autowired
    public DedupeAggMessagesService(
            RedisConnectionFactory connectionFactory,
            @Value("${ENABLE_AGG_DEDUP:true}") boolean enableAggDedupe,
            @Value("${AGG_DEDUP_LOCK_EXPIRY_SECONDS:14400}") Integer tokenRefreshLockExpirySeconds) {
        this.enableAggDedupe = enableAggDedupe;
        this.connectionFactory = connectionFactory;
        this.tokenRefreshLockExpirySeconds = tokenRefreshLockExpirySeconds;
    }

    public boolean shouldRunAgg(String tenantId, String integrationId) {
        if (!enableAggDedupe) {
            return true;
        }
        String lockKey = LOCK_PREFIX + tenantId + "_" + integrationId;
        try (var redis = connectionFactory.getConnection()) {
            Boolean success = redis.set(
                    lockKey.getBytes(StandardCharsets.UTF_8),
                    "dummy".getBytes(StandardCharsets.UTF_8),
                    Expiration.from(tokenRefreshLockExpirySeconds, TimeUnit.SECONDS),
                    RedisStringCommands.SetOption.ifAbsent());
            log.info("Lock acquire status for tenant {} integration {} is {}", tenantId, integrationId, success);
            return BooleanUtils.isNotFalse(success);
        }
    }
}
