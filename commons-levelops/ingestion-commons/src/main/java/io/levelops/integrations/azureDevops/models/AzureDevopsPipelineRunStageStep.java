package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AzureDevopsPipelineRunStageStep.AzureDevopsPipelineRunStageStepBuilder.class)
public class AzureDevopsPipelineRunStageStep {

    @JsonProperty("id")
    UUID id;

    @JsonProperty("parentId")
    UUID parentId;

    @JsonProperty("type")
    String type;

    @JsonProperty("name")
    String name;

    @JsonProperty("startTime")
    Date startTime;

    @JsonProperty("finishTime")
    Date finishTime;

    @JsonProperty("state")
    String state;

    @JsonProperty("result")
    String result;

    @JsonProperty("lastModified")
    Date lastModified;

    @JsonProperty("order")
    Integer order;

    @JsonProperty("log")
    Log log;

    @JsonProperty("steps")
    List<AzureDevopsPipelineRunStageStep> steps;

    @JsonProperty("stepLogs")
    String stepLogs;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = AzureDevopsPipelineRunStageStep.Log.LogBuilder.class)
    public static class Log {
        @JsonProperty("url")
        String url;
    }
}