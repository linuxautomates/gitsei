package io.levelops.integrations.github_actions.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubActionsEnrichedWorkflowRun.GithubActionsEnrichedWorkflowRunBuilder.class)
public class GithubActionsEnrichedWorkflowRun {

    @JsonProperty("workflow_run")
    GithubActionsWorkflowRun workflowRun; // enriched

    @JsonProperty("jobs")
    List<GithubActionsWorkflowRunJob> jobs; // enriched
}
