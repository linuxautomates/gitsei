package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.eclipse.egit.github.core.Repository;

import java.util.List;
import java.util.Set;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubWebhookEnrichmentRequests.GithubWebhookEnrichmentRequestsBuilder.class)
public class GithubWebhookEnrichmentRequests {

    @JsonProperty("requests")
    List<GithubWebhookEnrichmentRequest> requests;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = IssueEventRequest.IssueEventRequestBuilder.class)
    @JsonTypeInfo(
            use=JsonTypeInfo.Id.NAME,
            property="type",
            defaultImpl = IssueEventRequest.class,
            visible = true)
    @JsonTypeName("issue_event")
    public static class IssueEventRequest implements GithubWebhookEnrichmentRequest {

        @JsonProperty("repo_id")
        String repoId;

        @JsonProperty("issue_number")
        Integer issueNumber;

        @JsonProperty("webhook_id")
        String webhookId;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PREventRequest.PREventRequestBuilder.class)
    @JsonTypeInfo(
            use=JsonTypeInfo.Id.NAME,
            property="type",
            defaultImpl = PREventRequest.class,
            visible = true)
    @JsonTypeName("pr_event")
    public static class PREventRequest implements GithubWebhookEnrichmentRequest {

        @JsonProperty("repo_id")
        String repoId;

        @JsonProperty("pr_number")
        Integer prNumber;

        @JsonProperty("pr_merge_commit_sha")
        String prMergeCommitSha;

        @JsonProperty("repo_owner")
        String repoOwner;

        @JsonProperty("webhook_id")
        String webhookId;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ProjectColumnEventRequest.ProjectColumnEventRequestBuilder.class)
    @JsonTypeInfo(
            use=JsonTypeInfo.Id.NAME,
            property="type",
            defaultImpl = ProjectColumnEventRequest.class,
            visible = true)
    @JsonTypeName("project_column_event")
    public static class ProjectColumnEventRequest implements GithubWebhookEnrichmentRequest {

        @JsonProperty("column_id")
        String columnId;

        @JsonProperty("include_archived")
        Boolean includeArchived;

        @JsonProperty("webhook_id")
        String webhookId;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ProjectEventRequest.ProjectEventRequestBuilder.class)
    @JsonTypeInfo(
            use=JsonTypeInfo.Id.NAME,
            property="type",
            defaultImpl = ProjectEventRequest.class,
            visible = true)
    @JsonTypeName("project_event")
    public static class ProjectEventRequest implements GithubWebhookEnrichmentRequest {

        @JsonProperty("project_id")
        String projectId;

        @JsonProperty("include_archived")
        Boolean includeArchived;

        @JsonProperty("webhook_id")
        String webhookId;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PushEventRequest.PushEventRequestBuilder.class)
    @JsonTypeInfo(
            use=JsonTypeInfo.Id.NAME,
            property="type",
            defaultImpl = PushEventRequest.class,
            visible = true)
    @JsonTypeName("push_event")
    public static class PushEventRequest implements GithubWebhookEnrichmentRequest {

        @JsonProperty("repository")
        Repository repository;

        @JsonProperty("commit_shas")
        Set<String> commitShas;

        @JsonProperty("event_time")
        Long eventTime;

        @JsonProperty("webhook_id")
        String webhookId;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ProjectCardEventRequest.ProjectCardEventRequestBuilder.class)
    @JsonTypeInfo(
            use=JsonTypeInfo.Id.NAME,
            property="type",
            defaultImpl = ProjectCardEventRequest.class,
            visible = true)
    @JsonTypeName("project_card_event")
    public static class ProjectCardEventRequest implements GithubWebhookEnrichmentRequest {

        @JsonProperty("webhook_id")
        String webhookId;
    }
}
