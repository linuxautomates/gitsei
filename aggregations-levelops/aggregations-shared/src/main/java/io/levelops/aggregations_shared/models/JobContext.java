package io.levelops.aggregations_shared.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobInstanceUpdate;
import io.levelops.aggregations_shared.utils.IngestionResultPayloadUtils;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.GcsDataResultWithDataType;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobInstancePayload;
import io.levelops.commons.etl.models.JobMetadata;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.etl.models.job_progress.FileProgressDetail;
import io.levelops.commons.etl.models.job_progress.StageProgressDetail;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.gcs.models.GcsDataResult;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JobContext.JobContextBuilder.class)
public class JobContext {
    static final int PROGRESS_DETAIL_BATCH_SIZE = 10;
    // Concatenated jobId and instance id
    @NonNull
    @JsonProperty("job_instance_id")
    JobInstanceId jobInstanceId;

    @NonNull
    @JsonProperty("tenant_id")
    String tenantId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("integration_type")
    String integrationType;

    @NonNull
    @JsonProperty("job_scheduled_start_time")
    Date jobScheduledStartTime;

    @NonNull
    @JsonProperty("etl_processor_name")
    String etlProcessorName;

    @JsonProperty("timeout_in_minutes")
    Long timeoutInMinutes;

    @JsonProperty("is_full")
    Boolean isFull;

    @JsonProperty("reprocessing_requested")
    Boolean reprocessingRequested;

    @NonNull
    @JsonProperty("job_type")
    JobType jobType;

    @JsonProperty("gcs_records")
    List<GcsDataResultWithDataType> gcsRecords;

    @JsonProperty("stage_progress_map")
    @NonFinal
    @Builder.Default
    Map<String, Integer> stageProgressMap = new HashMap<>();

    @JsonProperty("stage_progress_detail_map")
    @NonFinal
    Map<String, StageProgressDetail> stageProgressDetailMap;

    @JsonIgnore
    @NonFinal
    @Getter(AccessLevel.NONE)
    HashMap<String, JobDTO> ingestionJobIdToDto;

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    JobInstanceDatabaseService jobInstanceDatabaseService;

    @JsonIgnore
    @NonFinal
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    int batchedProgressUpdateCount;

    public JobDTO getIngestionJobDto(String jobId, ControlPlaneService controlPlaneService) throws IngestionServiceException {
        // TODO call control plane service and populate all these in one go instead of multiple api calls
        if (ingestionJobIdToDto == null) {
            ingestionJobIdToDto = new HashMap<>();
        }

        if (!ingestionJobIdToDto.containsKey(jobId)) {
//            populateAllIngestionJobIdToDto();
            JobDTO ingestionJob = controlPlaneService.getJob(jobId);
            ingestionJobIdToDto.put(jobId, ingestionJob);
        }
        return ingestionJobIdToDto.get(jobId);
    }

    public Optional<Integer> getStageProgress(String stage) {
        // TODO: Does it make sense to allow this for generic jobs?
        ensureNotGenericJob("Cannot get stage progress for generic job");
        return Optional.ofNullable(stageProgressMap.getOrDefault(stage, null));
    }

    private void ensureNotGenericJob(String errorMessage) {
        if (jobType.isGeneric()) {
            throw new RuntimeException("Job type is generic: " + errorMessage);
        }
    }

    public Boolean setStageProgressMap(String stage, int progress, JobInstanceDatabaseService jobInstanceDatabaseService) {
        ensureNotGenericJob("Cannot set stage progress for generic job");
        stageProgressMap.put(stage, progress);
        try {
            jobInstanceDatabaseService.update(jobInstanceId, DbJobInstanceUpdate.builder()
                    .progress(stageProgressMap)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("Failed to persist stage progress map to db for job instance {}", jobInstanceId, e);
            return false;
        }
    }

    private void addStageToProgressDetailInternal(String stageName) {
        if (!stageProgressDetailMap.containsKey(stageName)) {
            stageProgressDetailMap.put(stageName, StageProgressDetail.builder()
                    .fileProgressMap(new HashMap<>())
                    .build());
        }
    }

    private Boolean persistStageProgressDetailMap(JobInstanceDatabaseService jobInstanceDatabaseService) throws JsonProcessingException {
        return jobInstanceDatabaseService.update(jobInstanceId, DbJobInstanceUpdate.builder()
                .progressDetails(stageProgressDetailMap)
                .build());
    }

    public Boolean addStageToProgressDetail(String stageName, JobInstanceDatabaseService jobInstanceDatabaseService) throws JsonProcessingException {
        ensureNotGenericJob("Cannot add stage to progress detail for generic job");
        addStageToProgressDetailInternal(stageName);
        return persistStageProgressDetailMap(jobInstanceDatabaseService);
    }

    public Boolean batchUpdateProgressDetail(
            String stageName,
            int fileIndex,
            FileProgressDetail fileProgressDetail,
            JobInstanceDatabaseService jobInstanceDatabaseService
    ) {
        ensureNotGenericJob("Cannot batch update progress detail for generic job");
        try {
            addStageToProgressDetailInternal(stageName);
            stageProgressDetailMap.get(stageName).getFileProgressMap().put(fileIndex, fileProgressDetail);
            batchedProgressUpdateCount++;
            if (batchedProgressUpdateCount >= PROGRESS_DETAIL_BATCH_SIZE) {
                return flushProgressDetailUpdates();
            } else {
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to update job progress details");
            return false;
        }
    }

    public Boolean flushProgressDetailUpdates() throws JsonProcessingException {
        Boolean updated = persistStageProgressDetailMap(jobInstanceDatabaseService);
        if (!updated) {
            log.error("Unable to update job progress detail");
        }
        batchedProgressUpdateCount = 0;
        return updated;
    }

    /**
     * NOTE: prefer to use batchUpdateProgressDetail where possible to reduce load on the DB
     */
    public Boolean updateProgressDetail(
            String stageName,
            int fileIndex,
            FileProgressDetail fileProgressDetail,
            JobInstanceDatabaseService jobInstanceDatabaseService
    ) throws JsonProcessingException {
        ensureNotGenericJob("Cannot update progress detail for generic job");
        try {
            addStageToProgressDetailInternal(stageName);
            stageProgressDetailMap.get(stageName).getFileProgressMap().put(fileIndex, fileProgressDetail);
            Boolean updated = persistStageProgressDetailMap(jobInstanceDatabaseService);
            if (!updated) {
                log.error("Unable to update job progress detail");
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update job progress details");
            return false;
        }
    }

    @JsonIgnore
    public JobMetadata getMetadata() {
        if (jobInstanceDatabaseService == null) {
            throw new UnsupportedOperationException("Metadata accessors not available for this JobContext");
        }
        DbJobInstance instance = jobInstanceDatabaseService.get(jobInstanceId).orElseThrow();
        return instance.getMetadata() != null ? instance.getMetadata() : JobMetadata.builder().build();
    }

    public void setMetadata(JobMetadata metadata) {
        if (jobInstanceDatabaseService == null) {
            throw new UnsupportedOperationException("Metadata accessors not available for this JobContext");
        }
        try {
            jobInstanceDatabaseService.update(jobInstanceId, DbJobInstanceUpdate.builder()
                    .metadata(metadata)
                    .build());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The scheduler sends jobContexts without a payload because the payload can get pretty large in some
     * circumstances. This method is called to rehydrate the payload from the DB or GCS.
     */
    public JobContext withRehydratedPayload(
            JobInstanceDatabaseService jobInstanceDatabaseService,
            IngestionResultPayloadUtils ingestionResultPayloadUtils) throws IngestionServiceException, IOException {
        if (jobType.isGeneric() || gcsRecords != null) {
            return this;
        }
        JobInstancePayload payload = null;
        DbJobInstance instance = jobInstanceDatabaseService.get(jobInstanceId).orElseThrow();
        if (Objects.nonNull(instance.getPayload())) {
            log.info("Payload is present in DB for job instance {}", instance.getJobInstanceId());
            payload = instance.getPayload();
        } else if (StringUtils.isNotEmpty(instance.getPayloadGcsFilename())) {
            log.info("Payload is present in GCS for job instance {}", instance.getJobInstanceId());
            Optional<JobInstancePayload> downloadedPayload = ingestionResultPayloadUtils.downloadPayloadFromGcs(instance.getPayloadGcsFilename());
            if (downloadedPayload.isEmpty()) {
                log.error("Failed to download payload from GCS for job instance {}, GCS file path: {}", instance.getJobInstanceId(), instance.getPayloadGcsFilename());
                payload = generateAndUploadPayload(ingestionResultPayloadUtils, instance);
            } else {
                payload = downloadedPayload.get();
            }
        } else {
            payload = generateAndUploadPayload(ingestionResultPayloadUtils, instance);
        }

        return this.toBuilder()
                .gcsRecords(payload.getGcsRecords())
                .build();
    }

    private JobInstancePayload generateAndUploadPayload(
            IngestionResultPayloadUtils ingestionResultPayloadUtils,
            DbJobInstance instance
    ) throws IngestionServiceException, JsonProcessingException {
        log.info("Generating payload for job instance {}", instance.getJobInstanceId());
        // Generate payload
        JobInstancePayload payload = ingestionResultPayloadUtils.generatePayloadForJobDefinition(
                jobInstanceId.getJobDefinitionId(), isFull);
        // Upload payload to GCS
        GcsDataResult gcsDataResult = ingestionResultPayloadUtils.uploadPayloadToGcs(payload, instance);
        // Update payload gcs file name in DB
        Boolean success = jobInstanceDatabaseService.update(instance.getJobInstanceId(), DbJobInstanceUpdate.builder()
                .payloadGcsFileName(gcsDataResult.getBlobId().getName())
                .build());
        if (!success) {
            log.error("Failed to update payload gcs file name for job instance {}, payload file path: {}",
                    instance.getJobInstanceId(), gcsDataResult.getBlobId().getName());
        }
        return payload;
    }

    public JobContext withMetadataAccessors(JobInstanceDatabaseService jobInstanceDatabaseService) {
        return this.toBuilder()
                .jobInstanceDatabaseService(jobInstanceDatabaseService)
                .build();
    }

}
