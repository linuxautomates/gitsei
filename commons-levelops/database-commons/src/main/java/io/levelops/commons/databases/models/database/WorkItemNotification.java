package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemNotification.WorkItemNotificationBuilder.class)
public class WorkItemNotification {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("work_item_id")
    private final UUID workItemId;

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
