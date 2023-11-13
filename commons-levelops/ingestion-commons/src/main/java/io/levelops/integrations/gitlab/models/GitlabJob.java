package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabJob.GitlabJobBuilder.class)
public class GitlabJob {

    @JsonProperty("id")
    String id;
    @JsonProperty("status")
    String status;
    @JsonProperty("stage")
    String stage;
    @JsonProperty("name")
    String name;
    @JsonProperty("ref")
    String ref;
    @JsonProperty("tag")
    boolean tag;
    @JsonProperty("coverage")
    String coverage;
    @JsonProperty("allow_failure")
    boolean allowFailure;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("started_at")
    Date startedAt;
    @JsonProperty("finished_at")
    Date finishedAt;
    @JsonProperty("duration")
    float duration;
    @JsonProperty("user")
    GitlabUser user;
    @JsonProperty("commit")
    GitlabCommit commit;
    @JsonProperty("pipeline")
    GitlabPipeline pipeline;
    @JsonProperty("web_url")
    String web_url;
    @JsonProperty("artifacts")
    List<Artifacts> artifacts;
    @JsonProperty("runner")
    Runner runner;
    @JsonProperty("artifacts_expire_at")
    Date artifactsExpireAt;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Artifacts.ArtifactsBuilder.class)
    public static class Artifacts {
        @JsonProperty("file_type")
        String file_type;
        @JsonProperty("size")
        int size;
        @JsonProperty("filename")
        String filename;
        @JsonProperty("file_format")
        String fileFormat;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Runner.RunnerBuilder.class)
    public static class Runner {
        @JsonProperty("id")
        String id;
        @JsonProperty("description")
        String description;
        @JsonProperty("ip_address")
        String ipAddress;
        @JsonProperty("active")
        boolean active;
        @JsonProperty("is_shared")
        boolean isShared;
        @JsonProperty("name")
        String name;
        @JsonProperty("online")
        boolean online;
        @JsonProperty("status")
        String status;
    }
}
