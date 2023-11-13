package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Builder
@Value
@JsonDeserialize(builder = Step.StepBuilder.class)
public class Step {
    @JsonProperty("id")
    private final String id;

    @JsonProperty("displayName")
    private final String displayName;

    @JsonProperty("displayDescription")
    private final String displayDescription;

    @JsonProperty("durationInMillis")
    private final Long durationInMillis;

    @JsonProperty("result")
    private final String result;

    @JsonProperty("state")
    private final String state;

    @JsonProperty("startTime")
    private final String startTime;

    @JsonProperty("input")
    private final String input;

    @JsonProperty("log")
    private final UUID log;

    @JsonProperty("actions")
    private final List<Action> actions;

    @JsonProperty("_links")
    private final JobRunLinks links;
}
