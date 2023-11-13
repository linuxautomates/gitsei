package io.levelops.integrations.github_actions.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubActionsWorkflowRunJobStep.GithubActionsWorkflowRunJobStepBuilder.class)
public class GithubActionsWorkflowRunJobStep {

    @JsonProperty("name")
    String name;

    @JsonProperty("status")
    String status;

    @JsonProperty("conclusion")
    String conclusion;

    @JsonProperty("number")
    Long number;

    @JsonProperty("started_at")
    Date startedAt;

    @JsonProperty("completed_at")
    Date completedAt;
}
