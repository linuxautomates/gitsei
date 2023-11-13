package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabPipeline.GitlabPipelineBuilder.class)
public class GitlabPipeline {
    @JsonProperty("id")
    String pipelineId;
    @JsonProperty("path_with_namespace")
    String pathWithNamespace;
    @JsonProperty("project_id")
    String projectId;
    @JsonProperty("project_name")
    String projectName;
    @JsonProperty("http_url_to_repo")
    String httpUrlToRepo;
    @JsonProperty("status")
    String status;
    @JsonProperty("ref")
    String ref;
    @JsonProperty("sha")
    String sha;
    @JsonProperty("web_url")
    String webUrl;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("updated_at")
    Date updatedAt;
    @JsonProperty("before_sha")
    String beforeSha;
    @JsonProperty("tag")
    String tag;
    @JsonProperty("yaml_errors")
    Object yamlErrors;
    @JsonProperty("started_at")
    Date startedAt;
    @JsonProperty("finished_at")
    Date finishedAt;
    @JsonProperty("committed_at")
    Date committedAt;
    @JsonProperty("duration")
    Integer duration;
    @JsonProperty("queued_duration")
    Integer queuedDuration;
    @JsonProperty("coverage")
    Object coverage;
    @JsonProperty("user")
    GitlabUser user;
    @JsonProperty("jobs")
    List<GitlabJob> jobs; //enriched
    @JsonProperty("variables")
    List<GitlabVariable> variables; //enriched
    @JsonProperty("test_report")
    GitlabTestReport testReport; //enriched
}
