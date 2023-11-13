package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraTransitions.JiraTransitionsBuilder.class)
public class JiraTransitions {

    @JsonProperty("transitions")
    List<JiraTransition> transitions;
}
