package io.levelops.aggregation_shared.test_utils;

import com.google.common.base.MoreObjects;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.GcsDataResultWithDataType;
import io.levelops.commons.etl.models.JobInstancePayload;
import io.levelops.commons.etl.models.JobMetadata;
import io.levelops.commons.etl.models.JobPriority;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.integrations.gcs.models.BlobId;
import io.levelops.integrations.gcs.models.GcsDataResult;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class JobInstanceTestUtils {

    private static final JobMetadata EMPTY_METADATA = JobMetadata.builder().build();

    public static JobInstancePayload createPayload(List<GcsDataResultWithDataType> gcsRecords) {
        Map<String, JobInstancePayload.IngestionJobStatus> jobStatusMap = new HashMap<>();
        gcsRecords.forEach(record -> {
            jobStatusMap.put(record.getIngestionJobId(), JobInstancePayload.IngestionJobStatus.builder()
                    .ingestionJobId(record.getIngestionJobId())
                    .isComplete(true)
                    .build());
        });
        return JobInstancePayload.builder()
                .gcsRecords(gcsRecords)
                .ingestionJobStatusMap(jobStatusMap)
                .build();
    }

    public static JobInstancePayload createPayloadFromIngestionJobIds(List<String> ingestionJobIds) {
        AtomicInteger index = new AtomicInteger();
        var gcsRecords = ingestionJobIds.stream().map(jobId -> GcsDataResultWithDataType.builder()
                .dataTypeName("tickets")
                .ingestionJobId(jobId)
                .index(index.getAndIncrement())
                .gcsDataResult(GcsDataResult.builder()
                        .uri("uri")
                        .htmlUri("htmluri")
                        .blobId(BlobId.builder()
                                .name("blob")
                                .generation(1L)
                                .bucket("bucket")
                                .build())
                        .build())
                .build()).toList();

        return JobInstanceTestUtils.createPayload(gcsRecords);
    }

    public static DbJobInstance createInstance(DbJobInstance instance, UUID jobDefinitionId) {
        Instant startTime = Instant.now();
        return DbJobInstance.builder()
                .jobDefinitionId(MoreObjects.firstNonNull(instance.getJobDefinitionId(), jobDefinitionId))
                .workerId(MoreObjects.firstNonNull(instance.getWorkerId(), "workerId"))
                .status(MoreObjects.firstNonNull(instance.getStatus(), JobStatus.PENDING))
                .scheduledStartTime(MoreObjects.firstNonNull(instance.getScheduledStartTime(), Instant.now()))
                .startTime(MoreObjects.firstNonNull(instance.getScheduledStartTime(), Instant.now()))
                .priority(MoreObjects.firstNonNull(instance.getPriority(), JobPriority.HIGH))
                .attemptMax(MoreObjects.firstNonNull(instance.getAttemptMax(), 10))
                .attemptCount(MoreObjects.firstNonNull(instance.getAttemptCount(), 0))
                .timeoutInMinutes(MoreObjects.firstNonNull(instance.getTimeoutInMinutes(), 10L))
                .aggProcessorName("jira")
                .lastHeartbeat(instance.getLastHeartbeat())
                .updatedAt(startTime)
                .metadata(instance.getMetadata())
                .progress(Map.of("1", 1))
                .progressDetails(instance.getProgressDetails())
                .payload(MoreObjects.firstNonNull(instance.getPayload(), createPayload(List.of(GcsDataResultWithDataType.builder()
                        .dataTypeName("tickets")
                        .ingestionJobId("randomId")
                        .index(0)
                        .gcsDataResult(GcsDataResult.builder()
                                .uri("uri")
                                .htmlUri("htmluri")
                                .blobId(BlobId.builder()
                                        .name("blob")
                                        .generation(1L)
                                        .bucket("bucket")
                                        .build())
                                .build())
                        .build()))))
                .isFull(MoreObjects.firstNonNull(instance.getIsFull(), true))
                .isReprocessing(MoreObjects.firstNonNull(instance.getIsReprocessing(), false))
                .tags(MoreObjects.firstNonNull(instance.getTags(), Set.of("s1", "s2", "s3")))
                .createdAt(instance.getCreatedAt())
                .build();
    }

    public static void assertTimestamp(Instant expected, Instant test) {
        if (expected != null) {
            assertThat(test).isCloseTo(expected, within(1, ChronoUnit.SECONDS));
        }
    }

    public static void assertEquals(DbJobInstance j1, DbJobInstance expected) {
        assertThat(j1.getJobDefinitionId()).isEqualTo(expected.getJobDefinitionId());
        assertThat(j1.getInstanceId()).isEqualTo(expected.getInstanceId());
        assertThat(j1.getWorkerId()).isEqualTo(expected.getWorkerId());
        assertThat(j1.getStatus()).isEqualTo(expected.getStatus());
        assertTimestamp(j1.getScheduledStartTime(), expected.getScheduledStartTime());
        assertTimestamp(j1.getStartTime(), expected.getStartTime());
        assertThat(j1.getPriority()).isEqualTo(expected.getPriority());
        assertThat(j1.getAttemptMax()).isEqualTo(expected.getAttemptMax());
        assertThat(j1.getAttemptCount()).isEqualTo(expected.getAttemptCount());
        assertThat(j1.getTimeoutInMinutes()).isEqualTo(expected.getTimeoutInMinutes());
        assertThat(j1.getAggProcessorName()).isEqualTo(expected.getAggProcessorName());
        assertTimestamp(j1.getLastHeartbeat(), expected.getLastHeartbeat());
//        assertThat(j1.getUpdatedAt()).isEqualTo(expected.getUpdatedAt());
        if (expected.getMetadata() == null || expected.getMetadata().equals(EMPTY_METADATA)) {
            assertThat(j1.getMetadata()).isIn(null, EMPTY_METADATA);
        } else {
            assertThat(j1.getMetadata()).isEqualTo(expected.getMetadata());
        }
        assertThat(j1.getProgress()).isEqualTo(expected.getProgress());
        assertThat(j1.getPayload()).isEqualTo(expected.getPayload());
        assertThat(j1.getPayloadGcsFilename()).isEqualTo(expected.getPayloadGcsFilename());
        assertThat(j1.getIsFull()).isEqualTo(expected.getIsFull());
        assertThat(j1.getTags()).isEqualTo(expected.getTags());
    }

    // Creates the instances and adds in the job instance id to the data structure
    // for easy validations

    public static List<DbJobInstance> insert(JobInstanceDatabaseService jobInstanceDatabaseService, DbJobInstance... instances) {
        return insert(StreamUtils.toStream(instances), jobInstanceDatabaseService);
    }

    public static List<DbJobInstance> insert(List<DbJobInstance> instances, JobInstanceDatabaseService jobInstanceDatabaseService) {
        return insert(instances.stream(), jobInstanceDatabaseService);
    }

    public static List<DbJobInstance> insert(Stream<DbJobInstance> instances, JobInstanceDatabaseService jobInstanceDatabaseService) {
        return instances
                .map(instance -> {
                    try {
                        var jobInstanceId = jobInstanceDatabaseService.insert(instance);
                        return instance.toBuilder().instanceId(jobInstanceId.getInstanceId()).build();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    public static DbJobDefinition createJobDefinition(
            Instant lastIterationTs, String tenantId, String integrationId, Boolean isActive) {
        tenantId = MoreObjects.firstNonNull(tenantId, "sid");
        integrationId = MoreObjects.firstNonNull(integrationId, "1");
        lastIterationTs = MoreObjects.firstNonNull(lastIterationTs, Instant.now());
        isActive = MoreObjects.firstNonNull(isActive, true);
        return DbJobDefinition.builder()
                .tenantId(tenantId)
                .integrationId(integrationId)
                .integrationType("jira")
                .ingestionTriggerId(UUID.randomUUID().toString())
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .isActive(isActive)
                .defaultPriority(JobPriority.HIGH)
                .attemptMax(10)
                .retryWaitTimeInMinutes(11)
                .timeoutInMinutes(12L)
                .frequencyInMinutes(13)
                .fullFrequencyInMinutes(14)
                .aggProcessorName("jira")
                .lastIterationTs(lastIterationTs)
                .metadata(null)
                .build();
    }
}
