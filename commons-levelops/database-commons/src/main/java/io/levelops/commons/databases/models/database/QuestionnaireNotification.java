package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = QuestionnaireNotification.QuestionnaireNotificationBuilder.class)
public class QuestionnaireNotification {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("questionnaire_id")
    private final UUID questionnaireId;

    @JsonProperty(value = "mode")
    private final NotificationMode mode;

    @JsonProperty("recipient")
    private final String recipient;

    @JsonProperty(value = "reference_id")
    private final String referenceId;

    @JsonProperty("channel_id")
    private final String channelId;

    @JsonProperty("url")
    private final String url;

    @JsonProperty("created_at")
    private Instant createdAt;
}
