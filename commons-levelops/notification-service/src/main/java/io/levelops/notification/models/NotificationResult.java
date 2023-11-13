package io.levelops.notification.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.MessageTemplate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = NotificationResult.NotificationResultBuilder.class)
public class NotificationResult {

    @JsonProperty("template_type")
    MessageTemplate.TemplateType templateType;

    @JsonProperty("job_id")
    String jobId; // for Slack

}
