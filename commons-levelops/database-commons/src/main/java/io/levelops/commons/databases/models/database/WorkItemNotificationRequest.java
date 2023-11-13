package io.levelops.commons.databases.models.database;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemNotificationRequest.WorkItemNotificationRequestBuilder.class)
public class WorkItemNotificationRequest {
    @JsonProperty("work_item_id")
    private final UUID workItemId;
    @JsonProperty("recipients")
    private final List<String> recipients;
    @JsonProperty("mode")
    private final NotificationMode mode;

    @JsonProperty("requestor_type")
    private final NotificationRequestorType requestorType;

    @JsonProperty("requestor_id")
    private final String requestorId;

    @JsonProperty("requestor_name")
    private final String requestorName;

    @JsonProperty("message")
    private final String message;
}
