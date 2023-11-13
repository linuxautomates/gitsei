package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CiCdPushedArtifacts.CiCdPushedArtifactsBuilder.class)
public class CiCdPushedArtifacts {
    @JsonProperty("id")
    String id;
    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("repository")
    String repository;

    @JsonProperty("job_name")
    String jobName;

    @JsonProperty("job_run_number")
    Long jobRunNumber;

    @JsonProperty("artifacts")
    List<CiCdPushedArtifact> artifacts;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CiCdPushedArtifact.CiCdPushedArtifactBuilder.class)
    public static class CiCdPushedArtifact {

        @JsonProperty("name")
        String name;

        @JsonProperty("location")
        String location;

        @JsonProperty("tag")
        String tag;

        @JsonProperty("digest")
        String digest;

        @JsonProperty("type")
        String type;

        @JsonProperty("artifact_created_at")
        Date artifactCreatedAt;
    }
}