package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Team.TeamBuilder.class)
public class Team {

    @JsonProperty("id")
    String id;

    @JsonProperty("description")
    String description;

    @JsonProperty("identity")
    String identity;

    @JsonProperty("identityUrl")
    String identityUrl;

    @JsonProperty("name")
    String name;

    @JsonProperty("projectId")
    String projectId;

    @JsonProperty("projectName")
    String projectName;

    @JsonProperty("url")
    String url;

    @JsonProperty("teamSetting")
    TeamSetting teamSetting;
}
