package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuperBuilder(toBuilder = true)
public class NotificationItem {
    @JsonProperty("integration_application")
    private String integrationApplication;
    @JsonProperty("integration_url")
    private String integrationUrl;
    @JsonProperty("sender_email")
    private String senderEmail;
    @JsonProperty("target_email")
    private String targetEmail;
    @JsonProperty("message_template_id")
    private String notificationTemplateId;
    @JsonProperty("assignment_message")
    private String notificationMessage;
    @JsonProperty("additional_info")
    private String additionalInfo;
    @JsonProperty("artifact")
    private String artifact;
    @JsonProperty("work_item_id")
    private String workItemId;
}