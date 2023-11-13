package io.levelops.integrations.sonarqube.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Action.ActionBuilder.class)
public class Action {

    @JsonProperty("rename")
    Boolean rename;

    @JsonProperty("setAsDefault")
    Boolean setAsDefault;

    @JsonProperty("copy")
    Boolean copy;

    @JsonProperty("associateProjects")
    Boolean associateProjects;

    @JsonProperty("delete")
    Boolean delete;

    @JsonProperty("manageConditions")
    Boolean manageConditions;

    @JsonProperty("create")
    Boolean create;


}