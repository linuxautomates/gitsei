package io.levelops.integrations.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.integrations.github.models.GithubPullRequest;
import io.levelops.integrations.github.models.GithubTag;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubData.GithubDataBuilder.class)
public class GithubData implements ControllerIngestionResult {

    @JsonProperty("pull_request")
    GithubPullRequest pullRequest;

    @JsonProperty("tags")
    List<GithubTag> githubTags;
}
