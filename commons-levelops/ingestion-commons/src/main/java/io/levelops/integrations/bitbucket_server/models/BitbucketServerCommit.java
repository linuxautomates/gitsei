package io.levelops.integrations.bitbucket_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketServerCommit.BitbucketServerCommitBuilder.class)
public class BitbucketServerCommit {

    @JsonProperty("id")
    String id;

    @JsonProperty("displayId")
    String displayId;

    @JsonProperty("author")
    BitbucketServerUser author;

    @JsonProperty("authorTimestamp")
    Long authorTimestamp;

    @JsonProperty("committer")
    BitbucketServerUser committer;

    @JsonProperty("committerTimestamp")
    Long committerTimestamp;

    @JsonProperty("message")
    String message;

    @JsonProperty("projectName")
    String projectName;

    @JsonProperty("repoName")
    String repoName;

    @JsonProperty("commitUrl")
    String commitUrl;

    @JsonProperty("additions")
    Integer additions;

    @JsonProperty("deletions")
    Integer deletions;

    @JsonProperty("files")
    List<BitbucketServerFile> files;

    @JsonProperty("parents")
    List<Parent> parents;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Parent.ParentBuilder.class)
    private static class Parent {

        @JsonProperty("id")
        String id;

        @JsonProperty("displayId")
        String displayId;
    }
}
