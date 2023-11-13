package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubPullRequestSearchResult.GithubPullRequestSearchResultBuilder.class)
public class GithubPullRequestSearchResult {
    @JsonProperty("url")
    String url;
    @JsonProperty("repository_url")
    String repositoryUrl;
    @JsonProperty("html_url")
    String htmlUrl;
    @JsonProperty("id")
    String id;
    @JsonProperty("number")
    String number;
    @JsonProperty("title")
    String title;
    @JsonProperty("user")
    GithubUser user;
    @JsonProperty("state")
    String state;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("updated_at")
    Date updatedAt;
    @JsonProperty("closed_at")
    Date closedAt;
    @JsonProperty("pull_request")
    PullRequest pullRequest;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PullRequest.PullRequestBuilder.class)
    public static class PullRequest {
        @JsonProperty("merged_at")
        Date mergedAt;
    }
}
