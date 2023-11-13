package io.levelops.integrations.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.github.models.GithubUser;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubApiIssueEvent.GithubApiIssueEventBuilder.class)
public class GithubApiIssueEvent {

    @JsonProperty("id")
    Long id;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("commit_id")
    String commitId;

    @JsonProperty("event")
    String event;

    @JsonProperty("url")
    String url;

    @JsonProperty("actor")
    GithubUser actor;

    // region event=assigned,unassigned
    @JsonProperty("assignee")
    GithubUser assignee;
    @JsonProperty("assigner")
    GithubUser assigner;
    //endregion

    // region event=commented,reviewed
    @JsonProperty("body")
    String body; // comment or review body
    // endregion

    // region event=labeled,unlabeled
    @JsonProperty("label")
    Label label;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Label.LabelBuilder.class)
    public static class Label {
        @JsonProperty("name")
        String name;
        @JsonProperty("color")
        String color;
    }
    //endregion

    // region event=locked
    @JsonProperty("lock_reason")
    String lockReason;
    //endregion

    // region event=reviewed
    @JsonProperty("state")
    String state;
    @JsonProperty("user")
    GithubUser user;
    @JsonProperty("pull_request_url")
    String pullRequestUrl;
    @JsonProperty("html_url")
    String htmlUrl;
    @JsonProperty("submitted_at")
    String submittedAt;
    //endregion

    // region event=review_request_removed
    @JsonProperty("review_requester")
    GithubUser reviewRequester;
    @JsonProperty("requested_reviewer")
    GithubUser requestedReviewer;
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
    Map<String, Object> milestone;
    //endregion

    // region event=cross-referenced
    @JsonProperty("source")
    Map<String, Object> source;
    //endregion

    // region event=committed
    @JsonProperty("author")
    GithubUser author;
    @JsonProperty("committer")
    GithubUser committer;
    // Map<String, Object> tree;
    @JsonProperty("message")
    String message;
    // List<Object> parents;
    // Map<String, Object> verification;
    // endregion
}
