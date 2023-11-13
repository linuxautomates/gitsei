package io.levelops.notification.services;

import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

@Log4j2
@Builder
public class SlackWorkItemCacheService {
    private static final String REDIS_KEY_FORMAT = "%s_%s_%s_wi_text_attachment_slack_message";
    private static final long CACHE_TTL_IN_SECONDS = TimeUnit.DAYS.toSeconds(2);

    private final RedisConnectionFactory redisConnectionFactory;

    private String buildKey(String company, UUID workItemId, UUID uploadId) {
        String key = String.format(REDIS_KEY_FORMAT, company, workItemId.toString(), uploadId.toString());
        log.info("key = {}", key);
        return key;
    }

    public void saveWITextAttachmentMessage(String company, UUID workItemId, UUID uploadId, String message) throws IOException {
        Validate.notBlank(company, "company cannot be null or empty!");
        Validate.notNull(workItemId, "workItemId cannot be null!");
        Validate.notNull(uploadId, "uploadId cannot be null!");

        String key = buildKey(company, workItemId, uploadId);
        try (RedisConnection redis = redisConnectionFactory.getConnection()) {
            Boolean success = redis.stringCommands().setEx(key.getBytes(UTF_8), CACHE_TTL_IN_SECONDS, message.getBytes(UTF_8));
            if(!Boolean.TRUE.equals(success)) {
                throw new IOException(String.format("Failed to save cache, company {}, workItemId {}, uploadId {}", company, workItemId, uploadId));
            }
        }
    }

    public Optional<String> getWITextAttachmentMessage(String company, UUID workItemId, UUID uploadId) {
        Validate.notBlank(company, "company cannot be null or empty!");
        Validate.notNull(workItemId, "workItemId cannot be null!");
        Validate.notNull(uploadId, "uploadId cannot be null!");

        String key = buildKey(company, workItemId, uploadId);
        try (RedisConnection redis = redisConnectionFactory.getConnection()) {
            byte[] data = redis.stringCommands().get(key.getBytes(UTF_8));
            return (data == null) ? Optional.empty() : Optional.ofNullable(new String(data, UTF_8));
        }
    }
}
