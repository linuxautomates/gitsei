package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AzureDevopsRelease.AzureDevopsReleaseBuilder.class)
public class AzureDevopsRelease {
    @JsonProperty("id")
    Integer id;

    @JsonProperty("name")
    String name;

    @JsonProperty("status")
    String status;

    @JsonProperty("createdOn")
    String createdOn;

    @JsonProperty("modifiedOn")
    String modifiedOn;

    @JsonProperty("createdBy")
    IdentityRef createdBy;

    @JsonProperty("start_time")
    String startTime;

    @JsonProperty("finish_time")
    String finishTime;

    @JsonProperty("releaseDefinition")
    AzureDevopsReleaseDefinition definition;

    @JsonProperty("description")
    String description;

    @JsonProperty("reason")
    String reason;

    @JsonProperty("logsContainerUrl")
    String logsContainerUrl;

    @JsonProperty("url")
    String url;

    @JsonProperty("owner")
    IdentityRef owner;

    @JsonProperty("_links")
    Link links;

    @JsonProperty("tags")
    List<String> tags;

    @JsonProperty("environments")
    List<AzureDevopsReleaseEnvironment> environments;

    @JsonProperty("variables")
    Map<String, Configuration.Variable> variables;

    @JsonProperty("variableGroups")
    List<Configuration.VariableGroup> variableGroups;

    @JsonProperty("artifacts")
    List<Map<String, Object>> artifacts;

    @JsonProperty("stages")
    List<AzureDevopsReleaseEnvironment> stages;
}
