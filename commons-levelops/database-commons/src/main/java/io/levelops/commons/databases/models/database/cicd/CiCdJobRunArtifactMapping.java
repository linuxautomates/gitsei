package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a mapping between 2 CI/CD Job Runs sharing a relation through a CI/CD artifact.
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CiCdJobRunArtifactMapping.CiCdJobRunArtifactMappingBuilder.class)
public class CiCdJobRunArtifactMapping {

    @JsonProperty("id")
    String id;

    /**
     * Source job run
     */
    @JsonProperty("cicd_job_run_id1")
    UUID cicdJobRunId1;

    /**
     * Target job run
     */
    @JsonProperty("cicd_job_run_id2")
    UUID cicdJobRunId2;

    @JsonProperty("created_at")
    Instant createdAt;

    // TODO consider adding more details on the correlation e.g. which artifacts?

}
