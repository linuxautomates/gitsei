package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Build.BuildBuilder.class)
public class Build {

    @JsonProperty("_links")
    Link link;

    @JsonProperty("properties")
    Property properties;

    @JsonProperty("tags")
    List<String> tags;

    @JsonProperty("validationResults")
    List<BuildRequestValidationResult> validationResults;

    @JsonProperty("plans")
    List<PlanReference> plans;

    @JsonProperty("triggerInfo")
    TriggerInfo triggerInfo;

    @JsonProperty("id")
    int id;

    @JsonProperty("buildNumber")
    String buildNumber;

    @JsonProperty("status")
    String status;

    @JsonProperty("result")
    String result;

    @JsonProperty("queueTime")
    String queueTime;

    @JsonProperty("startTime")
    String startTime;

    @JsonProperty("finishTime")
    String finishTime;

    @JsonProperty("url")
    String url;

    @JsonProperty("definition")
    DefinitionReference definition;

    @JsonProperty("buildNumberRevision")
    int buildNumberRevision;

    @JsonProperty("project")
    Project project;

    @JsonProperty("uri")
    String uri;

    @JsonProperty("sourceBranch")
    String sourceBranch;

    @JsonProperty("sourceVersion")
    String sourceVersion;

    @JsonProperty("queue")
    AgentPoolQueue queue;

    @JsonProperty("priority")
    String priority;

    @JsonProperty("reason")
    String reason;

    @JsonProperty("requestedFor")
    IdentityRef requestedFor;

    @JsonProperty("requestedBy")
    IdentityRef requestedBy;

    @JsonProperty("lastChangedDate")
    String lastChangedDate;

    @JsonProperty("lastChangedBy")
    IdentityRef lastChangedBy;

    @JsonProperty("orchestrationPlan")
    PlanReference orchestrationPlan;

    @JsonProperty("logs")
    BuildLog logs;

    @JsonProperty("repository")
    BuildRepository repository;

    @JsonProperty("keepForever")
    Boolean keepForever;

    @JsonProperty("retainedByRelease")
    Boolean retainedByRelease;

    @JsonProperty("triggeredByBuild")
    Build triggeredByBuild;
}
