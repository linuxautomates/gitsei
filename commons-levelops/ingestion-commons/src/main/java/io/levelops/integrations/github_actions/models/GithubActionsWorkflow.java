package io.levelops.integrations.github_actions.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubActionsWorkflow.GithubActionsWorkflowBuilder.class)
public class GithubActionsWorkflow {

    @JsonProperty("id")
    Long id;

    @JsonProperty("node_id")
    String nodeId;

    @JsonProperty("name")
    String name;

    @JsonProperty("path")
    String path;

    @JsonProperty("state")
    String state;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    @JsonProperty("url")
    String url;

}