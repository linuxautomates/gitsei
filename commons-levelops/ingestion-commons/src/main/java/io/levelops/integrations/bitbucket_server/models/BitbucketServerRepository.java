package io.levelops.integrations.bitbucket_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder =BitbucketServerRepository.BitbucketServerRepositoryBuilder.class)
public class BitbucketServerRepository {

    @JsonProperty("slug")
    String slug;

    @JsonProperty("id")
    Integer id;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("hierarchyId")
    String hierarchyId;

    @JsonProperty("scmId")
    String scmId;

    @JsonProperty("state")
    String state;

    @JsonProperty("statusMessage")
    String statusMessage;

    @JsonProperty("forkable")
    Boolean forkable;

    @JsonProperty("project")
    BitbucketServerProject project;

    @JsonProperty("public")
    Boolean isPublic;

    @JsonProperty("links")
    BitbucketServerLink links;

    @JsonProperty("defaultBranch")
    String defaultBranch;

    @JsonProperty("commits")
    List<BitbucketServerCommit> commits; //enriched

    @JsonProperty("pullRequests")
    List<BitbucketServerPullRequest> pullRequests; //enriched
}

