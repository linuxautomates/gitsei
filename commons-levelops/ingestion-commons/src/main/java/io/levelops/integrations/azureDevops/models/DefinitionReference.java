package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DefinitionReference.DefinitionReferenceBuilder.class)
public class DefinitionReference {

    @JsonProperty("createdDate")
    String createdDate;

    @JsonProperty("id")
    int id;

    @JsonProperty("name")
    String name;

    @JsonProperty("path")
    String path;

    @JsonProperty("project")
    Project project;

    @JsonProperty("queueStatus")
    String queueStatus;

    @JsonProperty("revision")
    int revision;

    @JsonProperty("type")
    String type;

    @JsonProperty("uri")
    String uri;

    @JsonProperty("url")
    String url;
}
