package io.levelops.integrations.droneci.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DroneCIEnrichRepoData.DroneCIEnrichRepoDataBuilder.class)
public class DroneCIEnrichRepoData {
    @JsonProperty("id")
    Long id;

    @JsonProperty("user_id")
    Long userId;

    @JsonProperty("namespace")
    String namespace;

    @JsonProperty("name")
    String name;

    @JsonProperty("slug")
    String slug;

    @JsonProperty("scm")
    String scm;

    @JsonProperty("git_http_url")
    String gitHttpUrl;

    @JsonProperty("git_ssh_url")
    String gitSshUrl;

    @JsonProperty("link")
    String link;

    @JsonProperty("default_branch")
    String defaultBranch;

    @JsonProperty("private")
    Boolean isPrivate;

    @JsonProperty("visibility")
    String visibility;

    @JsonProperty("active")
    Boolean active;

    @JsonProperty("config_path")
    String configPath;

    @JsonProperty("trusted")
    Boolean trusted;

    @JsonProperty("protected")
    Boolean isProtected;

    @JsonProperty("ignore_forks")
    Boolean ignoreForks;

    @JsonProperty("ignore_pull_requests")
    Boolean ignorePullRequests;

    @JsonProperty("auto_cancel_pull_requests")
    Boolean autoCancelPullRequests;

    @JsonProperty("auto_cancel_pushes")
    Boolean autoCancelPushes;

    @JsonProperty("auto_cancel_running")
    Boolean autoCancelRunning;

    @JsonProperty("timeout")
    Long timeout;

    @JsonProperty("counter")
    Long counter;

    @JsonProperty("synced")
    Long synced;

    @JsonProperty("created")
    Long created;

    @JsonProperty("updated")
    Long updated;

    @JsonProperty("version")
    Long version;

    @JsonProperty("archived")
    Boolean archived;

    @JsonProperty
    List<DroneCIBuild> builds;
}
