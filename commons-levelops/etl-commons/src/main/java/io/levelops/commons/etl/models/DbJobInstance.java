package io.levelops.commons.etl.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.etl.models.job_progress.StageProgressDetail;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.mutable.MutableInt;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJobInstance.DbJobInstanceBuilder.class)
public class DbJobInstance {
    @JsonIgnore
    public static final String SCHEDULER_CREATED_TAG = "scheduler_created";
    @JsonIgnore
    public static final String MANUAL_CREATED_TAG = "manually_created";

    @JsonProperty("id")
    UUID id;

    @JsonProperty("job_definition_id")
    UUID jobDefinitionId;

    @JsonProperty("instance_id")
    Integer instanceId;

    @JsonProperty("worker_id")
    String workerId;

    @JsonProperty("status")
    JobStatus status;

    @JsonProperty("scheduled_start_time")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant scheduledStartTime;

    @JsonProperty("start_time")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant startTime;

    @JsonProperty("priority")
    JobPriority priority;

    @JsonProperty("attempt_max")
    Integer attemptMax;

    @JsonProperty("attempt_count")
    Integer attemptCount;

    @JsonProperty("timeout_in_minutes")
    Long timeoutInMinutes;

    @JsonProperty("agg_processor_name")
    String aggProcessorName;

    @JsonProperty("last_heartbeat")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant lastHeartbeat;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant updatedAt;

    // This is persisted by a postgres trigger every time the status changes
    @JsonProperty("status_changed_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant statusChangedAt;

    @JsonProperty("metadata")
    JobMetadata metadata;

    @JsonProperty("progress")
    Map<String, Integer> progress;

    @JsonProperty("progress_details")
    Map<String, StageProgressDetail> progressDetails;

    @JsonProperty("payload")
    JobInstancePayload payload;

    @JsonProperty("payload_gcs_filename")
    String payloadGcsFilename;

    @JsonProperty("isFull")
    Boolean isFull;

    @JsonProperty("is_reprocessing")
    Boolean isReprocessing;

    @JsonProperty("tags")
    Set<String> tags;

    public JobInstanceId getJobInstanceId() {
        return JobInstanceId.builder()
                .jobDefinitionId(jobDefinitionId)
                .instanceId(instanceId)
                .build();
    }

    @JsonIgnore
    public boolean isSchedulerCreated() {
        return tags != null && tags.contains(SCHEDULER_CREATED_TAG);
    }

    @JsonProperty("payload_full_ingestion_job_ids")
    public Set<String> getFullIngestionJobIds() {
        if (payload != null) {
            return payload.getIngestionJobStatusMap().values().stream()
                    .filter(JobInstancePayload.IngestionJobStatus::getIsComplete)
                    .map(JobInstancePayload.IngestionJobStatus::getIngestionJobId)
                    .collect(Collectors.toSet());
        }
        else {
            return null;
        }
    }

    @JsonProperty("stage_progress_summary")
    public Map<String, Map<String, Integer>> getStageProgressSummary() {
        if (progressDetails != null) {
            return progressDetails.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> {
                String stage = entry.getKey();
                MutableInt totalSuccess = new MutableInt(0);
                MutableInt totalFailure = new MutableInt(0);
                entry.getValue().getFileProgressMap().values().forEach(f -> {
                            totalSuccess.add(f.getEntityProgressDetail().getSuccessful());
                            totalFailure.add(f.getEntityProgressDetail().getFailed());
                        }
                );
                return Map.of("total_success", totalSuccess.toInteger(), "total_failed", totalFailure.toInteger());
            }));
        }
        return null;
    }
}