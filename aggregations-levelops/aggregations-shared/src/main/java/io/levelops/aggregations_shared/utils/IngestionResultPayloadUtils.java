package io.levelops.aggregations_shared.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobInstanceFilter;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.GcsDataResultWithDataType;
import io.levelops.commons.etl.models.JobInstancePayload;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.integrations.storage.models.StorageData;
import io.levelops.integrations.storage.models.StorageResult;
import io.levelops.services.GcsStorageService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class to help with generating a payload from ingestion results.
 * This is useful only for INGESTION_RESULT_PROCESSING_JOB type jobs.
 */
@Service
@Log4j2
public class IngestionResultPayloadUtils {

    private final ObjectMapper objectMapper;
    private final ControlPlaneService controlPlaneService;
    private final JobInstanceDatabaseService jobInstanceDatabaseService;
    private final JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private final GcsStorageService gcsStorageService;

    public IngestionResultPayloadUtils(
            ObjectMapper objectMapper,
            ControlPlaneService controlPlaneService,
            JobInstanceDatabaseService jobInstanceDatabaseService,
            JobDefinitionDatabaseService jobDefinitionDatabaseService,
            GcsStorageService gcsStorageService) {
        this.objectMapper = objectMapper;
        this.controlPlaneService = controlPlaneService;
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
        this.jobDefinitionDatabaseService = jobDefinitionDatabaseService;
        this.gcsStorageService = gcsStorageService;
    }


    /**
     * Uploads the payload to GCS and returns the URI
     *
     * @param payload     payload to upload
     * @param jobInstance job instance to upload for - this affects the path of the upload
     * @return html uri of the uploaded file
     */
    public GcsDataResult uploadPayloadToGcs(JobInstancePayload payload, DbJobInstance jobInstance) throws JsonProcessingException {
        DbJobDefinition jobDefinition = jobDefinitionDatabaseService.get(jobInstance.getJobDefinitionId()).get();

        StorageResult result = gcsStorageService.pushOne(StorageData.builder()
                .jsonContent(objectMapper.writeValueAsString(payload))
                .integrationKey(IntegrationKey.builder()
                        .tenantId(jobDefinition.getTenantId())
                        .integrationId(jobDefinition.getIntegrationId())
                        .build())
                .jobId(jobInstance.getJobInstanceId().toString())
                .relativePath("payload.json")
                .build());
        return result.getRecords().get(0);
    }


    public Optional<JobInstancePayload> downloadPayloadFromGcs(String fileName) throws IOException {
        try {
            StorageData storageResult = gcsStorageService.read(fileName);
            return Optional.ofNullable(objectMapper.readValue(storageResult.getContent(), JobInstancePayload.class));
        } catch (FileNotFoundException e) {
            log.error("Failed to download payload from GCS. File not found: {}", fileName);
            return Optional.empty();
        }
    }

    public JobInstancePayload generatePayloadForJobDefinition(
            UUID jobDefinitionId,
            boolean isFull
    ) throws IngestionServiceException {
        DbJobDefinition jobDefinition = jobDefinitionDatabaseService.get(jobDefinitionId).get();
        return generatePayloadForJobDefinition(jobDefinition, isFull);
    }

    public JobInstancePayload generatePayloadForJobDefinition(
            DbJobDefinition jobDefinition,
            boolean isFull) throws IngestionServiceException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<GcsDataResultWithDataType> gcsRecords = getGcsDataResultForJobDefinition(jobDefinition, isFull);
        Map<String, JobInstancePayload.IngestionJobStatus> ingestionJobStatusMap = new HashMap<>();

        gcsRecords.forEach(gcsRecord -> {
            ingestionJobStatusMap.put(gcsRecord.getIngestionJobId(), JobInstancePayload.IngestionJobStatus.builder()
                    .ingestionJobId(gcsRecord.getIngestionJobId())
                    .isComplete(true)
                    .build());
        });
        log.info("Generated payload for job definition {} in {} ms. Payload size: {}",
                jobDefinition.getId(), stopwatch.elapsed().toMillis(), gcsRecords.size());

        return JobInstancePayload.builder()
                .gcsRecords(gcsRecords)
                .ingestionJobStatusMap(ingestionJobStatusMap)
                .build();
    }

    public List<GcsDataResultWithDataType> getGcsDataResultForJobDefinition(
            DbJobDefinition jobDefinition,
            boolean isFull) throws IngestionServiceException {
        TriggerResults ingestionResult = controlPlaneService.getAllTriggerResults(
                IntegrationKey.builder()
                        .tenantId(jobDefinition.getTenantId()).integrationId(jobDefinition.getIntegrationId()).build(),
                false,
                false,
                true
        ).getTriggerResults().get(0);

        var ingestionJobDtoStream = ingestionResult.getJobs().stream()
                .sorted(Comparator.comparing(JobDTO::getCreatedAt)
                        .reversed()
                        .thenComparing(JobDTO::getId));
        if (isFull) {
            return getGcsDataForTriggerResults(ingestionJobDtoStream.collect(Collectors.toList()));
        } else {
            Set<String> ingestionJobIdsToProcess = getIngestionJobIdsNotYetProcessed(ingestionResult, jobDefinition);
            List<JobDTO> ingestionJobs = ingestionJobDtoStream
                    .filter(jobDTO -> ingestionJobIdsToProcess.contains(jobDTO.getId()))
                    .collect(Collectors.toList());
            log.info("Taking partial for job definition {}. Processing ingestion job ids: {}", jobDefinition.getId(), ingestionJobIdsToProcess);
            return getGcsDataForTriggerResults(ingestionJobs);
        }
    }

    public Set<String> getIngestionJobIdsNotYetProcessed(TriggerResults ingestionResult, DbJobDefinition jobDefinition) {
        var jobInstanceStream = streamJobInstancesFromLastFull(
                jobInstanceDatabaseService.stream(DbJobInstanceFilter.builder()
                        .jobDefinitionIds(List.of(jobDefinition.getId()))
                        .jobStatuses(List.of(JobStatus.SUCCESS, JobStatus.PARTIAL_SUCCESS))
                        .orderBy(List.of(DbJobInstanceFilter.JobInstanceOrderByField.builder()
                                .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.INSTANCE_ID)
                                .isAscending(false)
                                .build()))
                        .build()
                ))
                .map(jobInstance -> {
                    if (Objects.isNull(jobInstance.getPayload()) && Objects.nonNull(jobInstance.getPayloadGcsFilename())) {
                        try {
                            Optional<JobInstancePayload> payloadOpt = downloadPayloadFromGcs(jobInstance.getPayloadGcsFilename());
                            if (payloadOpt.isPresent()) {
                                return jobInstance.toBuilder()
                                        .payload(payloadOpt.get())
                                        .build();
                            }
                        } catch (IOException e) {
                            log.error("Unable to rehydrate payload while determining incremental payload. Job instance: {}", jobInstance, e);
                            throw new RuntimeException(e);
                        }
                    }
                    return jobInstance;
                });
        Map<String, JobDTO> ingestionJobIdToJobDto = ingestionResult.getJobs().stream()
                .collect(Collectors.toMap(JobDTO::getId, Function.identity(), (a, b) -> b));
        Set<String> allProcessedIngestionJobIds = jobInstanceStream
                .map(DbJobInstance::getFullIngestionJobIds)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Set<String> unprocessedIngestionJobIds = new HashSet<>();

        ingestionJobIdToJobDto.forEach((id, value) -> {
            if (!allProcessedIngestionJobIds.contains(id)) {
                unprocessedIngestionJobIds.add(id);
            }
        });
        return unprocessedIngestionJobIds;
    }

    public Stream<DbJobInstance> streamJobInstancesFromLastFull(Stream<DbJobInstance> stream) {
        return stream.takeWhile(inclusiveFirstFailed(dbJobInstance -> !dbJobInstance.getIsFull()));
    }

    private static <T> Predicate<T> inclusiveFirstFailed(final Predicate<T> p) {
        final var goOn = new AtomicBoolean(true);
        return t -> p.test(t) ? goOn.get() : goOn.getAndSet(false);
    }


    private List<GcsDataResultWithDataType> getGcsDataForTriggerResults(
            List<JobDTO> ingestionJobList) {
        AtomicInteger index = new AtomicInteger();
        return ingestionJobList.stream().flatMap(jobDTO -> {
            ListResponse<StorageResult> storageResults = objectMapper.convertValue(jobDTO.getResult(),
                    ListResponse.typeOf(objectMapper, StorageResult.class));
            if (storageResults == null) {
                log.error("Failed to get storage results for job: {}", jobDTO);
                return Stream.empty();
            }
            return storageResults.getRecords().stream()
                    .flatMap(storageResult -> storageResult.getRecords().stream()
                            .map(gcsDataResult -> GcsDataResultWithDataType.builder()
                                    .gcsDataResult(gcsDataResult)
                                    .dataTypeName(storageResult.getStorageMetadata().getDataType())
                                    .ingestionJobId(jobDTO.getId())
                                    .index(index.getAndIncrement())
                                    .build()));
        }).collect(Collectors.toList());
    }
}
