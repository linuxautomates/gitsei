package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraTransition.JiraTransitionBuilder.class)
public class JiraTransition {

    @JsonProperty("id")
    String id;
    @JsonProperty("name")
    String name;
    @JsonProperty("hasScreen")
    Boolean hasScreen;
    @JsonProperty("isGlobal")
    Boolean isGlobal;
    @JsonProperty("isInitial")
    Boolean isInitial;
    @JsonProperty("isAvailable")
    Boolean isAvailable;
    @JsonProperty("isConditional")
    Boolean isConditional;
    // To
}
