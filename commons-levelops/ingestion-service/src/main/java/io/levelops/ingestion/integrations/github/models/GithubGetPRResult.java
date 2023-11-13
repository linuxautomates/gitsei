package io.levelops.ingestion.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.integrations.github.models.GithubPullRequest;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubGetPRResult.GithubGetPRResultBuilder.class)
public class GithubGetPRResult implements ControllerIngestionResult {

    @JsonProperty("pull_request")
    GithubPullRequest pullRequest;
}
