package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketCommit.BitbucketCommitBuilder.class)
public class BitbucketCommit {
    @JsonProperty("rendered")
    Rendered rendered;

    @JsonProperty("hash")
    String hash;

    @JsonProperty("repository")
    BitbucketRepoRef repository;

    @JsonProperty("author")
    CommitAuthor author;

    @JsonProperty("summary")
    BitbucketSummary summary;

    @JsonProperty("parents")
    List<BitbucketCommitRef> parents;

    @JsonProperty("date")
    Date date;

    @JsonProperty("message")
    String message;

    @JsonProperty("type")
    String type;

    @JsonProperty("workspace_slug")
    String workspaceSlug;

    @JsonProperty("repo_uuid")
    String repoUuid;

    @JsonProperty("links")
    BitbucketLinks links;

    @JsonProperty("diff_stats")
    List<BitbucketCommitDiffStat> diffStats;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Rendered.RenderedBuilder.class)
    public static final class Rendered {
        @JsonProperty("message")
        BitbucketSummary message;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CommitAuthor.CommitAuthorBuilder.class)
    public static class CommitAuthor {
        @JsonProperty("raw")
        String raw; // "Maxime Bellier <maxime@levelops.io>

        @JsonProperty("type")
        String type;
        
        @JsonProperty("user")
        BitbucketUser user;
    }
}
