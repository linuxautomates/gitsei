package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketRepository.BitbucketRepositoryBuilder.class)
public class BitbucketRepository {
    //region Data Members
    @JsonProperty("name")
    String name;

    @JsonProperty("scm")
    String scm; // "hg" or "git"

    @JsonProperty("website")
    String website;

    @JsonProperty("has_wiki")
    Boolean hasWiki;

    @JsonProperty("uuid")
    String uuid;

    @JsonProperty("fork_policy")
    String forkPolicy;

    @JsonProperty("project")
    BitbucketProject project;

    @JsonProperty("language")
    String language;

    @JsonProperty("created_on")
    Date createdOn;

    @JsonProperty("mainbranch")
    Branch mainbranch;

    @JsonProperty("full_name")
    String fullName;

    @JsonProperty("has_issues")
    Boolean hasIssues;

    @JsonProperty("owner")
    BitbucketUser owner;

    @JsonProperty("updated_on")
    Date updatedOn;

    @JsonProperty("size")
    Long size;

    @JsonProperty("type")
    String type;

    @JsonProperty("slug")
    String slug;

    @JsonProperty("is_private")
    Boolean isPrivate;

    @JsonProperty("description")
    String description;

    @JsonProperty("links")
    BitbucketLinks links;

    @JsonProperty("workspace_slug")
    String workspaceSlug;
    //endregion

    @JsonProperty("commits")
    List<BitbucketCommit> commits; //enriched

    @JsonProperty("tags")
    List<BitbucketTag> tags; //enriched

    @JsonProperty("pull_requests")
    List<BitbucketPullRequest> pullRequests; // enriched

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Branch.BranchBuilder.class)
    public static class Branch {

        @JsonProperty("type")
        String type; // e.g. "named_branch"

        @JsonProperty("name")
        String name;
    }
}
