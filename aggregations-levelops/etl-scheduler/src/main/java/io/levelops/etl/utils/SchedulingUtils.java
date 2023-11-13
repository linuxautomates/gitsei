package io.levelops.etl.utils;

import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.aggregations_shared.utils.IngestionResultPayloadUtils;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.GcsDataResultWithDataType;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.utils.MapUtils;
import io.levelops.etl.parameter_suppliers.JobDefinitionParameterSupplier;
import io.levelops.etl.parameter_suppliers.JobDefinitionParameterSupplier.ShouldTakeFull;
import io.levelops.etl.parameter_suppliers.JobDefinitionParameterSupplierRegistry;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.levelops.commons.etl.models.DbJobInstance.MANUAL_CREATED_TAG;
import static io.levelops.commons.etl.models.DbJobInstance.SCHEDULER_CREATED_TAG;

@Log4j2
@Service
public class SchedulingUtils {
    private final JobDefinitionParameterSupplierRegistry registry;
    private final JobInstanceDatabaseService jobInstanceDatabaseService;
    private final IngestionResultPayloadUtils ingestionResultPayloadUtils;

    public SchedulingUtils(
            JobInstanceDatabaseService jobInstanceDatabaseService,
            JobDefinitionParameterSupplierRegistry registry,
            IngestionResultPayloadUtils ingestionResultPayloadUtils) {
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
        this.registry = registry;
        this.ingestionResultPayloadUtils = ingestionResultPayloadUtils;
    }

    public static JobContext getJobContext(DbJobDefinition jobDefinition, DbJobInstance jobInstance) {
        Map<String, Integer> stageProgressMap = jobInstance.getProgress();
        if (Objects.isNull(stageProgressMap)) {
            stageProgressMap = new HashMap<>();
        }

        // This is to account for the case when excludePayload = true in jobInstanceDatabaseService.filter
        List<GcsDataResultWithDataType> gcsRecords = null;
        if (Objects.nonNull(jobInstance.getPayload())) {
            gcsRecords = jobInstance.getPayload().getGcsRecords();
        }

        return JobContext.builder()
                .jobInstanceId(JobInstanceId.builder()
                        .jobDefinitionId(jobDefinition.getId())
                        .instanceId(jobInstance.getInstanceId())
                        .build())
                .tenantId(jobDefinition.getTenantId())
                .integrationId(jobDefinition.getIntegrationId())
                .integrationType(jobDefinition.getIntegrationType())
                .jobScheduledStartTime(Date.from(jobInstance.getScheduledStartTime()))
                .gcsRecords(gcsRecords)
                .stageProgressMap(stageProgressMap)
                .stageProgressDetailMap(jobInstance.getProgressDetails())
                .etlProcessorName(jobInstance.getAggProcessorName())
                .timeoutInMinutes(jobInstance.getTimeoutInMinutes())
                .isFull(jobInstance.getIsFull())
                .reprocessingRequested(BooleanUtils.isTrue(jobInstance.getIsReprocessing()))
                .jobType(jobDefinition.getJobType())
                .build();
    }

    public static DbJobInstance createScheduledDbJobInstance(DbJobDefinition jobDefinition, Instant now, Boolean isSchedulerCreated) {
        Set<String> tags = new HashSet<>();
        if (isSchedulerCreated) {
            tags.add(SCHEDULER_CREATED_TAG);
        } else {
            tags.add(MANUAL_CREATED_TAG);
        }
        return DbJobInstance.builder()
                .jobDefinitionId(jobDefinition.getId())
                .status(JobStatus.SCHEDULED)
                .statusChangedAt(now)
                .scheduledStartTime(now)
                .priority(jobDefinition.getDefaultPriority())
                .attemptMax(jobDefinition.getAttemptMax())
                .attemptCount(0)
                .timeoutInMinutes(jobDefinition.getTimeoutInMinutes())
                .aggProcessorName(jobDefinition.getAggProcessorName())
                .tags(tags)
                .build();
    }

    public JobInstanceId scheduleJobDefinition(
            DbJobDefinition jobDefinition,
            Instant now,
            boolean isSchedulerCreated,
            @Nullable Boolean overrideIsFull,
            boolean reprocessingRequested
    ) throws IOException {
        log.debug("Scheduling job for definition id={}", jobDefinition.getId());
        DbJobInstance jobInstance = SchedulingUtils.createScheduledDbJobInstance(jobDefinition, now, isSchedulerCreated);

        JobDefinitionParameterSupplier parameterSupplier = registry.getSupplier(jobDefinition);
        ShouldTakeFull shouldTakeFull;
        if (overrideIsFull != null) {
            log.debug("jobDefinitionId={}, overrideIsFull={}", jobDefinition.getId(), overrideIsFull);
            shouldTakeFull = ShouldTakeFull.of(overrideIsFull);
        } else {
            log.debug("jobDefinitionId={}, calling shouldTakeFull on {}", jobDefinition.getId(), parameterSupplier.getClass().getSimpleName());
            try {
                shouldTakeFull = parameterSupplier.shouldTakeFull(jobInstance, jobDefinition, now);
            } catch (Throwable e) {
                log.warn("Failed to call shouldTakeFull on {}", parameterSupplier.getClass().getSimpleName(), e);
                throw e;
            }
        }

        DbJobInstance jobInstanceWithPayload = jobInstance.toBuilder()
                .isFull(shouldTakeFull.isTakeFull())
                .isReprocessing(reprocessingRequested)
                .payload(null) // SEI-3324: We're calculating the payload on the worker and storing it in GCS now
                .build();

        log.debug("jobDefinitionId={}, takeFull={}, reprocessingRequested={}, hasMetadataUpdate={}",
                jobDefinition.getId(), shouldTakeFull.isTakeFull(), reprocessingRequested, !MapUtils.isEmpty(shouldTakeFull.getMetadataUpdate()));

        JobInstanceId jobInstanceId = jobInstanceDatabaseService.insertAndUpdateJobDefinition(jobInstanceWithPayload, shouldTakeFull.getMetadataUpdate(), now);
        log.debug("Created jobInstanceId={}", jobInstanceId);
        return jobInstanceId;
    }
}
