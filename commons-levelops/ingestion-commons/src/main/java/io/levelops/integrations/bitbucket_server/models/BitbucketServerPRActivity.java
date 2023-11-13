package io.levelops.integrations.bitbucket_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketServerPRActivity.BitbucketServerPRActivityBuilder.class)
public class BitbucketServerPRActivity {

    @JsonProperty("id")
    Integer id;

    @JsonProperty("createdDate")
    Long createdDate;

    @JsonProperty("user")
    BitbucketServerUser user;

    @JsonProperty("action")
    String action;

    @JsonProperty("commit")
    Commit commit;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Commit.CommitBuilder.class)
    public static class Commit {

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

        @JsonProperty("parents")
        List<BitbucketServerCommit> parentCommits;
    }
}
