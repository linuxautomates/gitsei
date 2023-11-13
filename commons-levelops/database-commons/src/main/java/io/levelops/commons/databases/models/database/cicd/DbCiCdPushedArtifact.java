package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbCiCdPushedArtifact.DbCiCdPushedArtifactBuilder.class)
public class DbCiCdPushedArtifact {

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
    List<Artifact> artifacts;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Artifact.ArtifactBuilder.class)
    public static class Artifact {

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

    @JsonProperty("created_at")
    Instant createdAt;

    public static DbCiCdPushedArtifact fromCiCdPushedArtifacts(CiCdPushedArtifacts ciCdPushedArtifacts) {
        return DbCiCdPushedArtifact.builder()
                .integrationId(ciCdPushedArtifacts.getIntegrationId())
                .repository(ciCdPushedArtifacts.getRepository())
                .jobName(ciCdPushedArtifacts.getJobName())
                .jobRunNumber(ciCdPushedArtifacts.getJobRunNumber())
                .artifacts(ciCdPushedArtifacts.getArtifacts().stream().map(pushedArtifact ->
                        Artifact.builder()
                                .name(pushedArtifact.getName())
                                .location(pushedArtifact.getLocation())
                                .tag(pushedArtifact.getTag())
                                .digest(pushedArtifact.getDigest())
                                .type(pushedArtifact.getType())
                                .artifactCreatedAt(pushedArtifact.getArtifactCreatedAt())
                                .build()
                        ).collect(Collectors.toList()))
                .build();
    }
}
