package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BuildRepository.BuildRepositoryBuilder.class)
public class BuildRepository {

    @JsonProperty("checkoutSubmodules")
    boolean checkoutSubmodules;

    @JsonProperty("clean")
    String clean;

    @JsonProperty("defaultBranch")
    String defaultBranch;

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("properties")
    String properties;

    @JsonProperty("rootFolder")
    String rootFolder;

    @JsonProperty("type")
    String type;

    @JsonProperty("url")
    String url;
}
