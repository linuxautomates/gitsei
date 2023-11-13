package io.levelops.integrations.github_actions.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.github.models.GithubCommit;
import io.levelops.integrations.github.models.GithubPullRequest;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubUser;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubActionsWorkflowRun.GithubActionsWorkflowRunBuilder.class)
public class GithubActionsWorkflowRun {
    @JsonProperty("id")
    Long id;

    @JsonProperty("name")
    String name;

    @JsonProperty("workflow_name")
    String workflowName;

    @JsonProperty("node_id")
    String nodeId;

    @JsonProperty("head_branch")
    String headBranch;

    @JsonProperty("head_sha")
    String headSha;

    @JsonProperty("path")
    String path;

    @JsonProperty("display_title")
    String displayTitle;

    @JsonProperty("run_number")
    Long runNumber;

    @JsonProperty("event")
    String event;

    @JsonProperty("status")
    String status;

    @JsonProperty("conclusion")
    String conclusion;

    @JsonProperty("workflow_id")
    Long workflowId;

    @JsonProperty("check_suite_id")
    Long checkSuiteId;

    @JsonProperty("check_suite_node_id")
    String checkSuiteNodeId;

    @JsonProperty("url")
    String url;

    @JsonProperty("html_url")
    String htmlUrl;

    @JsonProperty("pull_requests")
    List<GithubPullRequest> pullRequests;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    @JsonProperty("actor")
    GithubUser actor;

    @JsonProperty("run_attempt")
    Long runAttempt;

    @JsonProperty("referenced_workflows")
    List<GithubActionsWorkflow> referencedWorkflows;

    @JsonProperty("run_started_at")
    Date runStartedAt;

    @JsonProperty("triggering_actor")
    GithubUser triggeringActor;

    @JsonProperty("jobs_url")
    String jobsUrl;

    @JsonProperty("logs_url")
    String logsUrl;

    @JsonProperty("check_suite_url")
    String checkSuiteUrl;

    @JsonProperty("artifacts_url")
    String artifactsUrl;

    @JsonProperty("cancel_url")
    String cancelUrl;

    @JsonProperty("rerun_url")
    String rerunUrl;

    @JsonProperty("previous_attempt_url")
    String previousAttemptUrl;

    @JsonProperty("workflow_url")
    String workflowUrl;

    @JsonProperty("head_commit")
    GithubCommit headCommit;

    @JsonProperty("repository")
    GithubRepository repository;

    @JsonProperty("head_repository")
    GithubRepository headRepository;
}