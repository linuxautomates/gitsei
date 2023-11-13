package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CICDJobRunDetails.CICDJobRunDetailsBuilder.class)
public class CICDJobRunDetails {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("cicd_job_run_id")
    private final UUID cicdJobRunId;

    @JsonProperty("gcs_path")
    private final String gcsPath;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}
