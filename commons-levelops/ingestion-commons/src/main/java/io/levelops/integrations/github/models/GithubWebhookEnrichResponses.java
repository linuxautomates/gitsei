package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

public interface GithubWebhookEnrichResponses {

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = IssueEventResponse.IssueEventResponseBuilder.class)
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type",
            defaultImpl = IssueEventResponse.class,
            visible = true,
            include = JsonTypeInfo.As.EXISTING_PROPERTY)
    @JsonTypeName("GITHUB_ISSUE")
    class IssueEventResponse implements GithubWebhookEnrichResponse {

        @JsonProperty("events")
        List<GithubIssueEvent> events;

        @JsonProperty("issue_number")
        Integer issueNumber;

        @JsonProperty("repo_id")
        String repoId;

        @JsonProperty("webhook_id")
        String webhookId;

        @JsonProperty("error")
        String error;

        @Override
        @JsonProperty("type")
        public GithubEventType getType() {
            return GithubEventType.GITHUB_ISSUE;
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PREventResponse.PREventResponseBuilder.class)
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type",
            defaultImpl = PREventResponse.class,
            visible = true,
            include = JsonTypeInfo.As.EXISTING_PROPERTY)
    @JsonTypeName("GITHUB_PR")
    class PREventResponse implements GithubWebhookEnrichResponse {

        @JsonProperty("pr_number")
        Integer prNumber;

        @JsonProperty("pr_reviews")
        List<GithubReview> reviews;

        @JsonProperty("pr_commits")
        List<GithubCommit> commits;

        @JsonProperty("patches")
        List<String> patches;

        @JsonProperty("repo_id")
        String repoId;

        @JsonProperty("repo_owner")
        String repoOwner;

        @JsonProperty("webhook_id")
        String webhookId;

        @JsonProperty("error")
        String error;

        @Override
        @JsonProperty("type")
        public GithubEventType getType() {
            return GithubEventType.GITHUB_PR;
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ProjectColumnEventResponse.ProjectColumnEventResponseBuilder.class)
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type",
            defaultImpl = ProjectColumnEventResponse.class,
            visible = true,
            include = JsonTypeInfo.As.EXISTING_PROPERTY)
    @JsonTypeName("GITHUB_PROJECT_COLUMN")
    class ProjectColumnEventResponse implements GithubWebhookEnrichResponse {

        @JsonProperty("column_id")
        String columnId;

        @JsonProperty("cards")
        List<GithubProjectCard> projectCards;

        @JsonProperty("webhook_id")
        String webhookId;

        @JsonProperty("error")
        String error;

        @Override
        @JsonProperty("type")
        public GithubEventType getType() {
            return GithubEventType.GITHUB_PROJECT_COLUMN;
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ProjectEventResponse.ProjectEventResponseBuilder.class)
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type",
            defaultImpl = ProjectEventResponse.class,
            visible = true,
            include = JsonTypeInfo.As.EXISTING_PROPERTY)
    @JsonTypeName("GITHUB_PROJECT")
    class ProjectEventResponse implements GithubWebhookEnrichResponse {

        @JsonProperty("project")
        String projectId;

        @JsonProperty("columns")
        List<GithubProjectColumn> githubProjectColumns;

        @JsonProperty("webhook_id")
        String webhookId;

        @JsonProperty("error")
        String error;

        @Override
        @JsonProperty("type")
        public GithubEventType getType() {
            return GithubEventType.GITHUB_PROJECT;
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PushEventResponse.PushEventResponseBuilder.class)
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type",
            defaultImpl = PushEventResponse.class,
            visible = true,
            include = JsonTypeInfo.As.EXISTING_PROPERTY)
    @JsonTypeName("GITHUB_PUSH")
    class PushEventResponse implements GithubWebhookEnrichResponse {

        @JsonProperty("commits")
        List<GithubCommit> commits;

        @JsonProperty("repo_id")
        String repoId;

        @JsonProperty("event_time")
        Long eventTime;

        @JsonProperty("webhook_id")
        String webhookId;

        @JsonProperty("error")
        String error;

        @Override
        @JsonProperty("type")
        public GithubEventType getType() {
            return GithubEventType.GITHUB_PUSH;
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ProjectCardEventResponse.ProjectCardEventResponseBuilder.class)
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type",
            defaultImpl = ProjectCardEventResponse.class,
            visible = true,
            include = JsonTypeInfo.As.EXISTING_PROPERTY)
    @JsonTypeName("GITHUB_PROJECT_CARD")
    class ProjectCardEventResponse implements GithubWebhookEnrichResponse {

        @JsonProperty("webhook_id")
        String webhookId;

        @JsonProperty("error")
        String error;

        @Override
        @JsonProperty("type")
        public GithubEventType getType() {
            return GithubEventType.GITHUB_PROJECT_CARD;
        }
    }
}
