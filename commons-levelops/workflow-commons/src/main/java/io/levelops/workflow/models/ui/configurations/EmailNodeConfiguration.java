package io.levelops.workflow.models.ui.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = EmailNodeConfiguration.EmailNodeConfigurationBuilder.class)
public class EmailNodeConfiguration implements NodeConfiguration {

    @JsonProperty("comm_channel")
    String commChannel; // "EMAIL"

    @JsonProperty("message_template_id")
    String messageTemplateId;

    @JsonProperty("additional_info")
    String additionalInfo;

    @JsonProperty("target_emails")
    List<String> targetEmails;

}