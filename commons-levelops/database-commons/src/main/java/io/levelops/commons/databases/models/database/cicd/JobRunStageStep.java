package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JobRunStageStep.JobRunStageStepBuilder.class)
public class JobRunStageStep {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("cicd_job_run_stage_id")
    private UUID cicdJobRunStageId;

    @JsonProperty("step_id")
    private String stepId;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("display_description")
    private String displayDescription;

    @JsonProperty("start_time")
    private Instant startTime;

    @JsonProperty("result")
    private String result;

    @JsonProperty("state")
    private String state;

    @JsonProperty("duration")
    private Integer duration;

    @JsonProperty("gcs_path")
    private final String gcsPath;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}
