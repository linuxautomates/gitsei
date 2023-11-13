package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = JobRunLinks.JobRunLinksBuilder.class)
public class JobRunLinks {
    @JsonProperty("prevRun")
    private final Link prevRun;
    @JsonProperty("parent")
    private final Link parent;
    @JsonProperty("tests")
    private final Link tests;
    @JsonProperty("nodes")
    private final Link nodes;
    @JsonProperty("log")
    private final Link log;
    @JsonProperty("self")
    private final Link self;
    @JsonProperty("blueTestSummary")
    private final Link blueTestSummary;
    @JsonProperty("actions")
    private final Link actions;
    @JsonProperty("steps")
    private final Link steps;
    @JsonProperty("artifacts")
    private final Link artifacts;
    @JsonProperty("changeSet")
    private final Link changeSet;
}
