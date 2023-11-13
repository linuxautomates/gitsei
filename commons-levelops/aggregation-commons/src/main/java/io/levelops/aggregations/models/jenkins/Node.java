package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Builder
@Value
@JsonDeserialize(builder = Node.NodeBuilder.class)
public class Node {
    @JsonProperty("id")
    private final String id;

    @JsonProperty("displayName")
    private final String displayName;
    @JsonProperty("displayDescription")
    private final String displayDescription;

    @JsonProperty("result")
    private final String result;
    @JsonProperty("state")
    private final String state;
    @JsonProperty("durationInMillis")
    private final Long durationInMillis;
    @JsonProperty("startTime")
    private final String startTime;
    @JsonProperty("type")
    private final String type;

    @JsonProperty("actions")
    private final List<Action> actions;

    @JsonProperty("edges")
    private final List<Edge> edges;

    @JsonProperty("log")
    private final UUID log;

    @JsonProperty("steps")
    private List<Step> steps;

    @JsonProperty("child_job_runs")
    private final List<JobRun> childJobRuns;

    @JsonProperty("firstParent")
    private final String firstParent;
    @JsonProperty("restartable")
    private final Boolean restartable;

    @JsonProperty("_links")
    private final JobRunLinks links;
}
