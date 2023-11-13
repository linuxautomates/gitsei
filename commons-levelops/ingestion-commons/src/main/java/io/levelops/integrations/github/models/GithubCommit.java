package io.levelops.integrations.github.models;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubCommit.GithubCommitBuilder.class)
public class GithubCommit implements Serializable {

    @JsonProperty("sha")
    private String sha;

    @JsonProperty("url")
    private String url;

    @JsonProperty("author")
    private GithubUser author;

    @JsonProperty("committer")
    private GithubUser committer;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private Date timestamp;

//    private List<Commit> parentCommits;

    @JsonProperty("git_author")
    private GithubCommitUser gitAuthor;

    @JsonProperty("git_committer")
    private GithubCommitUser gitCommitter;

    // N/A for PR commits
    @JsonProperty("stats")
    private GithubCommitStats stats;

    //   N/A for PR commits
    @JsonProperty("files")
    private List<GithubCommitFile> files;

    @JsonProperty("branch")
    private String branch;

    @JsonIgnore
    public Optional<List<GithubCommitFile>> getFiles() {
        return Optional.ofNullable(files);
    }
}
