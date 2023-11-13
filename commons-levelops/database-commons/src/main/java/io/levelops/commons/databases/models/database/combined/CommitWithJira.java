package io.levelops.commons.databases.models.database.combined;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ZendeskWithJira.ZendeskWithJiraBuilder.class)
public class CommitWithJira {
    @JsonProperty("id")
    String id;

    @JsonProperty("repo_id")
    String repoId;

    @JsonProperty("committer")
    String committer;

    @JsonProperty("jira_keys")
    List<String> jiraKeys;

    @JsonProperty("commit_sha")
    String commitSha;

    @JsonProperty("author")
    String author; //name from git author field

    @JsonProperty("commit_url")
    String commitUrl;

    @JsonProperty("message")
    String message; //commit msg

    @JsonProperty("additions")
    Integer additions;

    @JsonProperty("deletions")
    Integer deletions;

    @JsonProperty("files_ct")
    Integer filesCt;

    @JsonProperty("committed_at")
    Long committedAt;
}
