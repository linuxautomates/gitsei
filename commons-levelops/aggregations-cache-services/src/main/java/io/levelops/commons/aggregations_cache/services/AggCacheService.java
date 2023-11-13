package io.levelops.commons.aggregations_cache.services;

import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Log4j2
@Builder
public class AggCacheService {
    private static final String REDIS_KEY_FORMAT = "jira_%s_%s_%s_%s";
    private static final Long CACHE_TTL_VALUE_DEFAULT = 60L;
    private static final TimeUnit CACHE_TTL_UNIT_DEFAULT = TimeUnit.MINUTES;

    private final Boolean enabled;
    private final RedisConnectionFactory redisConnectionFactory;

    private String buildKey(String company, String misc, List<String> integrationIds, String hash) {
        String key = String.format(REDIS_KEY_FORMAT, company, misc,
                integrationIds.stream().sorted().collect(Collectors.joining(",")), hash);
        log.debug("key = {}", key);
        return key;
    }

    public void saveQueryData(String company, String misc, List<String> integrationIds, String queryHash, String data)
            throws IOException {
        saveQueryData(company, misc, integrationIds, queryHash, data, CACHE_TTL_VALUE_DEFAULT, CACHE_TTL_UNIT_DEFAULT);
    }

    public void saveQueryData(String company, String misc, List<String> integrationIds, String queryHash, String data, Long cacheTTLValue, TimeUnit cacheTTLUnit)throws IOException {
        if (!enabled) {
            log.debug("Caching flag is disabled.");
            return;
        }
        if(misc == null)
            misc = "";
        Validate.notBlank(company, "company cannot be null or empty!");
        Validate.notNull(integrationIds, "integrationIds cannot be null!");
        Validate.notNull(queryHash, "query cannot be null!");
        Validate.notNull(data, "data cannot be null!");
        Validate.notNull(cacheTTLValue, "cacheTTLValue cannot be null!");
        Validate.notNull(cacheTTLUnit, "cacheTTLUnit cannot be null!");

        String key = buildKey(company, misc, integrationIds, queryHash);
        try (RedisConnection redis = redisConnectionFactory.getConnection()) {
            Boolean success = redis.stringCommands().setEx(key.getBytes(UTF_8), cacheTTLUnit.toSeconds(cacheTTLValue), data.getBytes(UTF_8));
            if (!Boolean.TRUE.equals(success)) {
                throw new IOException(String.format("Failed to save cache, company %s, integrationid %s, query %s",
                        company, integrationIds, queryHash));
            }
        }
    }

    public Optional<String> getQueryData(String company, String misc, List<String> integrationIds, String queryHash) {
        if (!enabled) {
            log.debug("Caching flag is disabled.");
            return Optional.empty();
        }
        if(misc == null)
            misc = "";
        Validate.notBlank(company, "company cannot be null or empty!");
        Validate.notNull(integrationIds, "integrationIds cannot be null!");
        Validate.notNull(queryHash, "queryhash cannot be null!");

        String key = buildKey(company, misc, integrationIds, queryHash);
        try (RedisConnection redis = redisConnectionFactory.getConnection()) {
            byte[] data = redis.stringCommands().get(key.getBytes(UTF_8));
            return (data == null) ? Optional.empty() : Optional.of(new String(data, UTF_8));
        }
    }
}
