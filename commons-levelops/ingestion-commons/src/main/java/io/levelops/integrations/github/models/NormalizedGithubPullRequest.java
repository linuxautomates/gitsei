package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = NormalizedGithubPullRequest.NormalizedGithubPullRequestBuilder.class)
public class NormalizedGithubPullRequest {

    @JsonProperty("id")
    String id;
    @JsonProperty("number")
    Integer number;
    @JsonProperty("state")
    String state;
    @JsonProperty("locked")
    Boolean locked;
    @JsonProperty("title")
    String title;
    @JsonProperty("body")
    String body;
    @JsonProperty("assignee")
    String assignee;
    @JsonProperty("assignees")
    List<String> assignees;
    @JsonProperty("reviewers")
    List<String> reviewers;
    @JsonProperty("labels")
    List<String> labels;
    @JsonProperty("milestone")
    String milestone;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("updated_at")
    Date updatedAt;
    @JsonProperty("closed_at")
    Date closedAt;
    @JsonProperty("merged_at")
    Date mergedAt;
}
