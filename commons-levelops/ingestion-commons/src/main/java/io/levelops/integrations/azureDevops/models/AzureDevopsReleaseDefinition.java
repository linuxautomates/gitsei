package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AzureDevopsReleaseDefinition.AzureDevopsReleaseDefinitionBuilder.class)
public class AzureDevopsReleaseDefinition {

    @JsonProperty("source")
    String source;

    @JsonProperty("revision")
    Integer revision;

    @JsonProperty("description")
    String description;

    @JsonProperty("createdOn")
    String createdOn;

    @JsonProperty("modifiedOn")
    String modifiedOn;

    @JsonProperty("isDeleted")
    Boolean isDeleted;

    @JsonProperty("id")
    Integer id;

    @JsonProperty("name")
    String name;

    @JsonProperty("path")
    String path;

    @JsonProperty("url")
    String url;

    @JsonProperty("_links")
    Link links;

//    @JsonProperty("lastRelease")

    @JsonProperty("variables")
    Map<String, Configuration.Variable> variables;
    @JsonProperty("variableGroups")
    List<Integer> variableGroups;

    @JsonProperty("project")
    Project project;

//    @JsonProperty("artifacts")
//    @JsonProperty("triggers")
//    @JsonProperty("tags")
//    @JsonProperty("properties")
}
