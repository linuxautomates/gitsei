package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabCommit.GitlabCommitBuilder.class)
public class GitlabCommit {
    @JsonProperty("id")
    String id;
    @JsonProperty("short_id")
    String shortId;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("parent_ids")
    List<String> parentIds;
    @JsonProperty("title")
    String title;
    @JsonProperty("message")
    String message;
    @JsonProperty("author_name")
    String authorName;
    @JsonProperty("author_email")
    String authorEmail;
    @JsonProperty("authored_date")
    Date authoredDate;
    @JsonProperty("committer_name")
    String committerName;
    @JsonProperty("committer_email")
    String committerEmail;
    @JsonProperty("committed_date")
    Date committedDate;
    @JsonProperty("web_url")
    String webUrl;
    @JsonProperty("stats")
    GitlabCommitStat stats; //enriched
    @JsonProperty("changes")
    List<GitlabChange> changes;
    @JsonProperty("author_details")
    GitlabUser authorDetails;
    @JsonProperty("committer_details")
    GitlabUser committerDetails;
    @JsonProperty("ref_branch")
    String refBranch;
}
