package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AzureDevopsReleaseStep.AzureDevopsReleaseStepBuilder.class)
public class AzureDevopsReleaseStep {
    @JsonProperty("id")
    Long id;
    @JsonProperty("name")
    String name;
    @JsonProperty("dateStarted")
    String dateStarted;
    @JsonProperty("dateEnded")
    String dateEnded;
    @JsonProperty("startTime")
    String startTime;
    @JsonProperty("finishTime")
    String finishTime;
    @JsonProperty("status")
    String status;
    @JsonProperty("agentName")
    String agentName;
    @JsonProperty("logUrl")
    String logUrl;
    @JsonProperty("stepLogs")
    String stepLogs;
}
