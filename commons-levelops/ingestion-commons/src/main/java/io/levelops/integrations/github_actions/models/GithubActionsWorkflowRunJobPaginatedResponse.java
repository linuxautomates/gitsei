package io.levelops.integrations.github_actions.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubActionsWorkflowRunJobPaginatedResponse.GithubActionsWorkflowRunJobPaginatedResponseBuilder.class)
public class GithubActionsWorkflowRunJobPaginatedResponse {

    @JsonProperty("total_count")
    Integer totalCount;

    @JsonProperty("jobs")
    List<GithubActionsWorkflowRunJob> jobs;
}
