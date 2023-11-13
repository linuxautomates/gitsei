package io.levelops.integrations.github_actions.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubActionsWorkflowPaginatedResponse.GithubActionsWorkflowPaginatedResponseBuilder.class)
public class GithubActionsWorkflowPaginatedResponse {
    @JsonProperty("total_count")
    Integer totalCount;

    @JsonProperty("workflows")
    List<GithubActionsWorkflow> workflows;
}
