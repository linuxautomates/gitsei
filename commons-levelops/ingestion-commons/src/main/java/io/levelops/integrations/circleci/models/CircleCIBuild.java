package io.levelops.integrations.circleci.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CircleCIBuild.CircleCIBuildBuilder.class)
public class CircleCIBuild {

    // enriched
    @JsonProperty("all_commit_details")
    List<CircleCIScmInfo> scmInfoList;

    // enriched
    @JsonProperty("steps")
    List<CircleCIBuildStep> steps;

    @JsonProperty("vcs_url")
    String vcsUrl;

    @JsonProperty("vcs_type")
    String vcsType;

    @JsonProperty("build_url")
    String buildUrl;

    @JsonProperty("build_num")
    int buildNumber;

    @JsonProperty("branch")
    String branch;

    @JsonProperty("vcs_revision")
    String vcsRevision;

    @JsonProperty("committer_name")
    String committerName;

    @JsonProperty("committer_email")
    String committerEmail;

    @JsonProperty("commit_message")
    String commitMessage;

    @JsonProperty("subject")
    String subject;

    @JsonProperty("body")
    String body;

    @JsonProperty("why")
    String why;

    @JsonProperty("dont_build")
    String dontBuild;

    @JsonProperty("queued_at")
    Date queuedAt;

    @JsonProperty("start_time")
    Date startTime;

    @JsonProperty("stop_time")
    Date stopTime;

    @JsonProperty("build_time_millis")
    Long buildTimeMillis;

    @JsonProperty("username")
    String username;

    @JsonProperty("reponame")
    String repoName;

    @JsonProperty("lifecycle")
    String lifecycle;

    @JsonProperty("outcome")
    String outcome;

    @JsonProperty("status")
    String status;

    @JsonProperty("retry_of")
    int retryOf;

    @JsonProperty("workflows")
    CircleCIBuildWorkflow workflows;

    @JsonProperty("repo")
    String repo;

    @JsonProperty("user_name")
    String user;

    public String getModifiedSlug() { return vcsType + "/" + user + "/" + repo; }

    public String getSlug() {
        return vcsType + "/" + username + "/" + repoName;
    }
}
