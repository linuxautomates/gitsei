package io.levelops.integrations.bitbucket_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketServerPullRequest.BitbucketServerPullRequestBuilder.class)
public class BitbucketServerPullRequest {

    @JsonProperty("id")
    Integer id;

    @JsonProperty("version")
    Integer version;

    @JsonProperty("title")
    String title;

    @JsonProperty("description")
    String description;

    @JsonProperty("state")
    String state;

    @JsonProperty("open")
    Boolean open;

    @JsonProperty("closed")
    Boolean closed;

    @JsonProperty("createdDate")
    Long createdDate;

    @JsonProperty("updatedDate")
    Long updatedDate;

    @JsonProperty("fromRef")
    RepoRef fromRef;

    @JsonProperty("toRef")
    RepoRef toRef;

    @JsonProperty("locked")
    Boolean locked;

    @JsonProperty("author")
    PRUser author;

    @JsonProperty("reviewers")
    List<PRUser> reviewers;

    @JsonProperty("participants")
    List<PRUser> participants;

    @JsonProperty("links")
    BitbucketServerLink links;

    @JsonProperty("properties")
    PRProperties properties;

    @JsonProperty("commits")
    List<BitbucketServerCommit> prCommits;

    @JsonProperty("activities")
    List<BitbucketServerPRActivity> activities;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RepoRef.RepoRefBuilder.class)
    public static class RepoRef {

        @JsonProperty("id")
        String id;

        @JsonProperty("displayId")
        String displayId;

        @JsonProperty("latestCommit")
        String latestCommit;

        @JsonProperty("repository")
        BitbucketServerRepository repository;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PRUser.PRUserBuilder.class)
    public static class PRUser {

        @JsonProperty("user")
        BitbucketServerUser user;

        @JsonProperty("role")
        String role;

        @JsonProperty("approved")
        Boolean approved;

        @JsonProperty("status")
        String status;

        @JsonProperty("lastReviewedCommit")
        String lastReviewedCommit;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PRProperties.PRPropertiesBuilder.class)
    private static class PRProperties {

        @JsonProperty("mergeResult")
        MergeResult mergeResult;

        @JsonProperty("resolvedTaskCount")
        Integer resolvedTaskCount;

        @JsonProperty("openTaskCount")
        Integer openTaskCount;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = MergeResult.MergeResultBuilder.class)
        private static class MergeResult {

            @JsonProperty("outcome")
            String outcome;

            @JsonProperty("current")
            Boolean current;
        }
    }
}


