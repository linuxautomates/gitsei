package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GithubWebhookEnrichmentRequests.IssueEventRequest.class, name = "issue_event"),
        @JsonSubTypes.Type(value = GithubWebhookEnrichmentRequests.PREventRequest.class, name = "pr_event"),
        @JsonSubTypes.Type(value = GithubWebhookEnrichmentRequests.ProjectCardEventRequest.class, name = "project_card_event"),
        @JsonSubTypes.Type(value = GithubWebhookEnrichmentRequests.ProjectColumnEventRequest.class, name = "project_column_event"),
        @JsonSubTypes.Type(value = GithubWebhookEnrichmentRequests.ProjectEventRequest.class, name = "project_event"),
        @JsonSubTypes.Type(value = GithubWebhookEnrichmentRequests.PushEventRequest.class, name = "push_event")
})
public interface GithubWebhookEnrichmentRequest {
}
