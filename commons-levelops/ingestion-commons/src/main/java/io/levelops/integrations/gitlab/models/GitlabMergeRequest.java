package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabMergeRequest.GitlabMergeRequestBuilder.class)
public class GitlabMergeRequest {
    @JsonProperty("id")
    String id;
    @JsonProperty("iid")
    String iid;
    @JsonProperty("project_id")
    long projectId;
    @JsonProperty("title")
    String title;
    @JsonProperty("description")
    String description;
    @JsonProperty("state")
    String state;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("updated_at")
    Date updatedAt;
    @JsonProperty("merged_by")
    GitlabUser mergedBy;
    @JsonProperty("merged_at")
    Date mergedAt;
    @JsonProperty("closed_by")
    GitlabUser closedBy;
    @JsonProperty("closed_at")
    Date closedAt;
    @JsonProperty("target_branch")
    String targetBranch;
    @JsonProperty("source_branch")
    String sourceBranch;
    @JsonProperty("user_notes_count")
    int userNotesCount;
    @JsonProperty("upvotes")
    int upVotes;
    @JsonProperty("downvotes")
    int downVotes;
    @JsonProperty("author")
    GitlabUser author;
    @JsonProperty("assignees")
    List<GitlabUser> assignees;
    @JsonProperty("assignee")
    GitlabUser assignee;
    @JsonProperty("source_project_id")
    String sourceProjectId;
    @JsonProperty("target_project_id")
    String targetProjectId;
    @JsonProperty("work_in_progress")
    boolean workInProgress;
    @JsonProperty("labels")
    List<String> labels;
    @JsonProperty("milestone")
    GitlabMilestone milestone;
    @JsonProperty("merge_when_pipeline_succeeds")
    boolean mergeWhenPipelineSucceeds;
    @JsonProperty("merge_status")
    String mergeStatus;
    @JsonProperty("sha")
    String sha;
    @JsonProperty("merge_commit_sha")
    String mergeCommitSha;
    @JsonProperty("squash_commit_sha")
    String squashCommitSha;
    @JsonProperty("commits")
    List<GitlabCommit> commits;//enriched
    @JsonProperty("state_events")
    List<GitlabStateEvent> stateEvents;//enriched
    @JsonProperty("events")
    List<GitlabEvent> mergeRequestEvents;//enriched
    @JsonProperty("changes")
    GitlabMergeRequestChanges mergeRequestChanges;//enriched
}
