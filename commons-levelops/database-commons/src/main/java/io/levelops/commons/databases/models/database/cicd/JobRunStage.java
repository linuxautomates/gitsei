package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.util.Set;
import java.util.UUID;

@Value()
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@JsonDeserialize(builder = JobRunStage.JobRunStageBuilderImpl.class)
public class JobRunStage extends JobRunSegment {
    @NonNull
    @JsonProperty("cicd_job_run_id")
    private UUID ciCdJobRunId;
    @JsonProperty("stage_id")
    private String stageId;
    @Default
    @JsonProperty("type")
    private SegmentType type = SegmentType.CICD_STAGE;
    @JsonProperty("state")
    private String state;
    @NonNull
    @JsonProperty("child_job_runs")
    private Set<UUID> childJobRuns;

    // required by lombok
    @JsonPOJOBuilder(withPrefix = "")
    static final class JobRunStageBuilderImpl extends JobRunStage.JobRunStageBuilder<JobRunStage, JobRunStage.JobRunStageBuilderImpl> {
    }

}