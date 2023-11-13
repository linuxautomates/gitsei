package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Repository.RepositoryBuilder.class)
public class Repository {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("url")
    String url;

    @JsonProperty("project")
    Project project;

    @JsonProperty("defaultBranch")
    String defaultBranch;

    @JsonProperty("size")
    Long size;

    @JsonProperty("remoteUrl")
    String remoteUrl;

    @JsonProperty("sshUrl")
    String sshUrl;

    @JsonProperty("webUrl")
    String webUrl;

    @JsonProperty("isDisabled")
    Boolean isDisabled;

    @JsonProperty("commits")
    List<Commit> commits; // enriched.

    @JsonProperty("pullRequests")
    List<PullRequest> pullRequests; // enriched.
}
