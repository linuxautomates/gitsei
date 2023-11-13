package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GithubWebhookEnrichResponses.IssueEventResponse.class, name = "GITHUB_ISSUE"),
        @JsonSubTypes.Type(value = GithubWebhookEnrichResponses.PREventResponse.class, name = "GITHUB_PR"),
        @JsonSubTypes.Type(value = GithubWebhookEnrichResponses.ProjectCardEventResponse.class, name = "GITHUB_PROJECT_CARD"),
        @JsonSubTypes.Type(value = GithubWebhookEnrichResponses.ProjectColumnEventResponse.class, name = "GITHUB_PROJECT_COLUMN"),
        @JsonSubTypes.Type(value = GithubWebhookEnrichResponses.ProjectEventResponse.class, name = "GITHUB_PROJECT"),
        @JsonSubTypes.Type(value = GithubWebhookEnrichResponses.PushEventResponse.class, name = "GITHUB_PUSH")
})
public interface GithubWebhookEnrichResponse {
    GithubEventType getType();

    String getWebhookId();

    String getError();
}
