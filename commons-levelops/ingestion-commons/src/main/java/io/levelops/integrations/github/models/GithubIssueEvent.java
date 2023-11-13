package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubIssueEvent.GithubIssueEventBuilder.class)
public class GithubIssueEvent {

     @JsonProperty("id")
     Long id;

     @JsonProperty("created_at")
     Date createdAt;

     // only using Events API
     @JsonProperty("issue")
     GithubIssue issue;

     @JsonProperty("commit_id")
     String commitId;

     @JsonProperty("event")
     String event;

     @JsonProperty("url")
     String url;

     @JsonProperty("actor")
     String actor;

     // region event=assigned
     @JsonProperty("assignee")
     String assignee;
     @JsonProperty("assigner")
     String assigner;
     //endregion

     // region event=commented,reviewed
     @JsonProperty("body")
     String body; // comment or review body
     // endregion

     // region event=labeled,unlabeled
     @JsonProperty("label")
     String label;
     //endregion

     // region event=locked
     @JsonProperty("lock_reason")
     String lockReason;
     //endregion

     // region event=reviewed
     @JsonProperty("state")
     String state;
     @JsonProperty("user")
     String user;
     @JsonProperty("pull_request_url")
     String pullRequestUrl;
     @JsonProperty("html_url")
     String htmlUrl;
     @JsonProperty("submitted_at")
     String submittedAt;
     //endregion

     // region event=review_request_removed
     @JsonProperty("review_requester")
     String reviewRequester;
     @JsonProperty("requested_reviewer")
     String requestedReviewer;
     //endregion

     // region event=review_dismissed
     @JsonProperty("dismissed_review")
     Map<String, Object> dismissedReview;
     //endregion

     // region event=renamed
     @JsonProperty("rename")
     Map<String, Object> rename;
     //endregion

     // region event=added_to_project, moved_columns_in_project, removed_from_project, converted_note_to_issue
     // Map<String, Object> projectCard;
     @JsonProperty("project_id")
     String projectId;
     @JsonProperty("project_url")
     String projectUrl;
     @JsonProperty("column_name")
     String columnName;
     @JsonProperty("previous_column_name")
     String previousColumnName; // for move
     //endregion

     // region event=milestoned,demilestoned
     @JsonProperty("milestone")
     String milestone;
     //endregion

     // region event=cross-referenced
     @JsonProperty("source")
     Map<String, Object> source;
     //endregion

     // region event=committed
     @JsonProperty("author")
     String author;
     @JsonProperty("committer")
     String committer;
     // Map<String, Object> tree;
     @JsonProperty("message")
     String message;
     // List<Object> parents;
     // Map<String, Object> verification;
     // endregion
}
