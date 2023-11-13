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
public class SlackQuestionnaireCacheService {
    private static final String REDIS_KEY_FORMAT = "%s_%s_questionnaire_slack_message";
    private static final long CACHE_TTL_IN_SECONDS = TimeUnit.DAYS.toSeconds(2);

    private final RedisConnectionFactory redisConnectionFactory;

    private String buildKey(String company, UUID questionnaireId) {
        String key = String.format(REDIS_KEY_FORMAT, company, questionnaireId.toString());
        log.info("key = {}", key);
        return key;
    }

    public void saveQuestionnaireMessage(String company, UUID questionnaireId, String message) throws IOException {
        Validate.notBlank(company, "company cannot be null or empty!");
        Validate.notNull(questionnaireId, "questionnaireId cannot be null!");
        Validate.notBlank(message, "message cannot be null or empty!");

        String key = buildKey(company, questionnaireId);
        try (RedisConnection redis = redisConnectionFactory.getConnection()) {
            Boolean success = redis.stringCommands().setEx(key.getBytes(UTF_8), CACHE_TTL_IN_SECONDS, message.getBytes(UTF_8));
            if(!Boolean.TRUE.equals(success)) {
                throw new IOException(String.format("Failed to save cache, company {}, questionnaireId {}", company, questionnaireId));
            }
        }
    }

    public Optional<String> getQuestionnaireMessage(String company, UUID questionnaireId) {
        Validate.notBlank(company, "company cannot be null or empty!");
        Validate.notNull(questionnaireId, "questionnaireId cannot be null!");

        String key = buildKey(company, questionnaireId);
        try (RedisConnection redis = redisConnectionFactory.getConnection()) {
            byte[] data = redis.stringCommands().get(key.getBytes(UTF_8));
            return (data == null) ? Optional.empty() : Optional.ofNullable(new String(data, UTF_8));
        }
    }
}
