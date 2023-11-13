package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Builder
@Value
@JsonDeserialize(builder = JobRun.JobRunBuilder.class)
public class JobRun {
    @JsonProperty("id")
    private final String id;
    @JsonProperty("name")
    private final String name;
    @JsonProperty("organization")
    private final String organization;
    @JsonProperty("pipeline")
    private final String pipeline;
    @JsonProperty("result")
    private final String result;
    @JsonProperty("state")
    private final String state;
    @JsonProperty("type")
    private final String type;
    @JsonProperty("actions")
    private final List<Action> actions;
    @JsonProperty("causes")
    private final List<Cause> causes;
    @JsonProperty("description")
    private final String description;
    @JsonProperty("durationInMillis")
    private final Long durationInMillis;
    @JsonProperty("enQueueTime")
    private final String enQueueTime;
    @JsonProperty("endTime")
    private final String endTime;
    @JsonProperty("estimatedDurationInMillis")
    private final Long estimatedDurationInMillis;

    @JsonProperty("replayable")
    private final boolean replayable;

    @JsonProperty("runSummary")
    private final String runSummary;
    @JsonProperty("startTime")
    private final String startTime;

    @JsonProperty("_links")
    private final JobRunLinks links;

    @JsonProperty("child_job_runs")
    private final List<JobRun> childJobRuns;

    @JsonProperty("stages")
    private final List<Node> stages;

    @JsonProperty("log")
    private final UUID log;

    @JsonProperty("job_normalized_full_name")
    @JsonAlias("qualified_name")
    private final String jobNormalizedFullName;
}
