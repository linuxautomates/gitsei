package io.levelops.integrations.bitbucket_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketServerEnrichedProjectData.BitbucketServerEnrichedProjectDataBuilder.class)
public class BitbucketServerEnrichedProjectData {

    @JsonProperty("project")
    BitbucketServerProject project;

    @JsonProperty("repository")
    BitbucketServerRepository repository;

    @JsonProperty("commits")
    List<BitbucketServerCommit> commits;

    @JsonProperty("pullRequests")
    List<BitbucketServerPullRequest> pullRequests;

    @JsonProperty("tags")
    List<BitbucketServerTag> tags;
}
