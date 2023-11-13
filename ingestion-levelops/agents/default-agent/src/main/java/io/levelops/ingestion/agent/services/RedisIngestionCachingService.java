package io.levelops.ingestion.agent.services;

import io.levelops.ingestion.services.IngestionCachingService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

@Log4j2
public class RedisIngestionCachingService implements IngestionCachingService {

    private static final String REDIS_KEY_FORMAT = "ingestion_data_%s_%s_%s";

    private final RedisConnectionFactory redisConnectionFactory;
    private final boolean cacheEnabled;
    private final long cacheTtlMinutes;

    public RedisIngestionCachingService(RedisConnectionFactory redisConnectionFactory,
                                        boolean cacheEnabled,
                                        long cacheTtlMinutes) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.cacheEnabled = cacheEnabled && cacheTtlMinutes > 0;
        this.cacheTtlMinutes = cacheTtlMinutes;
        log.info("Redis ingestion cache: enabled={}, ttl={}min", this.cacheEnabled, cacheTtlMinutes);
    }

    @Override
    public boolean isEnabled() {
        return cacheEnabled;
    }

    private String buildRedisKey(String company, String integrationId, String objectKey) {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        Validate.notBlank(objectKey, "objectKey cannot be null or empty.");
        return String.format(REDIS_KEY_FORMAT, company, integrationId, objectKey);
    }

    public void write(String company, String integrationId, String objectKey, String objectValue) throws IOException {
        if (!cacheEnabled) {
            return;
        }
        String redisKey = buildRedisKey(company, integrationId, objectKey);
        try (RedisConnection redis = redisConnectionFactory.getConnection()) {
            log.debug("Storing value in Redis cache for company={}, integration={}, objectKey={}", company, integrationId, objectKey);
            Boolean success = redis.stringCommands().setEx(
                    redisKey.getBytes(UTF_8),
                    TimeUnit.MINUTES.toSeconds(cacheTtlMinutes),
                    objectValue.getBytes(UTF_8));
            if(!Boolean.TRUE.equals(success)) {
                throw new IOException(String.format("Failed to cache value for company=%s, integration=%s, objectKey=%s", company, integrationId, objectKey));
            }
        }
    }

    public Optional<String> read(String company, String integrationId, String objectKey) {
        if (!cacheEnabled) {
            return Optional.empty();
        }
        String redisKey = buildRedisKey(company, integrationId, objectKey);
        try (RedisConnection redis = redisConnectionFactory.getConnection()) {
            byte[] data = redis.stringCommands().get(redisKey.getBytes(UTF_8));
            return (data == null) ? Optional.empty() : Optional.ofNullable(StringUtils.trimToNull(new String(data, UTF_8)));
        }
    }

}
