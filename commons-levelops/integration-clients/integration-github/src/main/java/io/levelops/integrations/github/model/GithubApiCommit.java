package io.levelops.integrations.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.github.models.GithubCommitFile;
import io.levelops.integrations.github.models.GithubCommitStats;
import io.levelops.integrations.github.models.GithubCommitUser;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubUser;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubApiCommit.GithubApiCommitBuilder.class)
public class GithubApiCommit {
    @JsonProperty("url")
    String url;
    @JsonProperty("sha")
    String sha;
    @JsonProperty("node_id")
    String nodeId;
    @JsonProperty("commit")
    Commit commit;
    @JsonProperty("author")
    GithubUser author;
    @JsonProperty("committer")
    GithubUser committer;
    @JsonProperty("files")
    List<GithubCommitFile> files;

    // Added to support PR commits
    @JsonProperty("stats")
    GithubCommitStats stats;

    // Added to support PR commits
    @JsonProperty("branch")
    String branch;

    // Returned when using commit search
    @JsonProperty("repository")
    GithubRepository repository;

    /*
        "parents": [
            {
                "url": "https://api.github.com/repos/octocat/Hello-World/commits/6dcb09b5b57875f334f61aebed695e2e4193db5e",
                "sha": "6dcb09b5b57875f334f61aebed695e2e4193db5e"
            }
        ]
    */

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Commit.CommitBuilder.class)
    public static class Commit {
        @JsonProperty("author")
        GithubCommitUser author;
        @JsonProperty("committer")
        GithubCommitUser committer;
        @JsonProperty("message")
        String message;
        @JsonProperty("comment_count")
        Integer commentCount;
        /*
            "tree": {
                "url": "https://api.github.com/repos/octocat/Hello-World/tree/6dcb09b5b57875f334f61aebed695e2e4193db5e",
                "sha": "6dcb09b5b57875f334f61aebed695e2e4193db5e"
            },
            "verification": {
                "verified": false,
                "reason": "unsigned",
                "signature": null,
                "payload": null
        }
        */
    }

}
