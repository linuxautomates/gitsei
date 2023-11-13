package io.levelops.integrations.github_actions.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubActionsWorkflowRunJob.GithubActionsWorkflowRunJobBuilder.class)
public class GithubActionsWorkflowRunJob {

    @JsonProperty("id")
    Long id;

    @JsonProperty("run_id")
    Long runId;

    @JsonProperty("workflow_name")
    String workflowName;

    @JsonProperty("head_branch")
    String headBranch;

    @JsonProperty("run_url")
    String runUrl;

    @JsonProperty("run_attempt")
    Long runAttempt;

    @JsonProperty("node_id")
    String nodeId;

    @JsonProperty("head_sha")
    String headSha;

    @JsonProperty("url")
    String url;

    @JsonProperty("html_url")
    String htmlUrl;

    @JsonProperty("status")
    String status;

    @JsonProperty("conclusion")
    String conclusion;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("started_at")
    Date startedAt;

    @JsonProperty("completed_at")
    Date completedAt;

    @JsonProperty("name")
    String name;

    @JsonProperty("steps")
    List<GithubActionsWorkflowRunJobStep> steps;

    @JsonProperty("check_run_url")
    String checkRunUrl;

    @JsonProperty("labels")
    List<String> labels;

    @JsonProperty("runner_id")
    String runnerId;

    @JsonProperty("runner_name")
    String runnerName;

    @JsonProperty("runner_group_id")
    Long runnerGroupId;

    @JsonProperty("runner_group_name")
    String runnerGroupName;
}
