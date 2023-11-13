package io.levelops.notification.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.SlackChatInteractiveMessageResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackNotificationResult.SlackNotificationResultBuilder.class)
public class SlackNotificationResult {

    @JsonProperty("message_result")
    SlackChatInteractiveMessageResult messageResult;

    @JsonProperty("job_id")
    String jobId;

    @JsonProperty("sync")
    public boolean isSync() {
        return jobId == null && messageResult != null;
    }
}
