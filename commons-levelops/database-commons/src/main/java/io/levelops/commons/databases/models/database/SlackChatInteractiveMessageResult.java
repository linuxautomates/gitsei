package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackChatInteractiveMessageResult.SlackChatInteractiveMessageResultBuilder.class)
public class SlackChatInteractiveMessageResult {
    @JsonProperty("success")
    Boolean success;

    @JsonProperty("total_recipients")
    Integer totalRecipients;

    @JsonProperty("notifications")
    List<Notification> notifications;
}
