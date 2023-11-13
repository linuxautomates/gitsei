package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.eclipse.egit.github.core.Repository;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubWebhookEvent.GithubWebhookEventBuilder.class)
public class GithubWebhookEvent {

    @JsonProperty("zen")
    String zen;

    @JsonProperty("hook_id")
    Integer hookId;

    @JsonProperty("ref")
    String ref;

    @JsonProperty("ref_type")
    String refType;

    @JsonProperty("action")
    String action;

    @JsonProperty("sender")
    GithubCreator sender;

    @JsonProperty("issue")
    GithubIssue issue;

    @JsonProperty("comment")
    GithubWebhookComment comment;

    @JsonProperty("repository")
    Repository repository;

    @JsonProperty("pull_request")
    GithubPullRequest pullRequest;

    @JsonProperty("project")
    GithubProject project;

    @JsonProperty("project_column")
    GithubProjectColumn projectColumn;

    @JsonProperty("project_card")
    GithubProjectCard projectCard;

    @JsonProperty("changes")
    GithubWebhookChanges changes;

    @JsonProperty("organization")
    GithubOrganization organization;

    @JsonProperty("commits")
    List<GithubWebhookCommit> commits;

    @Value
    @Builder(toBuilder = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonDeserialize(builder = GithubWebhookCommit.GithubWebhookCommitBuilder.class)
    public static class GithubWebhookCommit {
        @JsonProperty("id")
        String sha;
        @JsonProperty("timestamp")
        String timestamp;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonDeserialize(builder = GithubWebhookComment.GithubWebhookCommentBuilder.class)
    public static class GithubWebhookComment {
        @JsonProperty("id")
        String id;
        @JsonProperty("body")
        String body;
        @JsonProperty("user")
        GithubUser user;
        @JsonProperty("created_at")
        Date createdAt;
        @JsonProperty("updated_at")
        Date updatedAt;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GithubWebhookChanges.GithubWebhookChangesBuilder.class)
    public static class GithubWebhookChanges {
        
        @JsonProperty("column_id")
        Column columnId;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = Column.ColumnBuilder.class)
        public static class Column {

            @JsonProperty("from")
            String from;

        }
    }
}
