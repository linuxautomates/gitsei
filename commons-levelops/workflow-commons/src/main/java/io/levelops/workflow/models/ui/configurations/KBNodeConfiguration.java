package io.levelops.workflow.models.ui.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class KBNodeConfiguration implements NodeConfiguration {

    @JsonProperty("best_practices_id")
    String bestPracticesId;

    @JsonProperty("comm_channel")
    String commChannel; // "EMAIL" or "SLACK"

    @JsonProperty("message_template_id")
    String messageTemplateId;

    @JsonProperty("additional_info")
    String additionalInfo;

    @JsonProperty("target_emails")
    List<String> targetEmails;

}
