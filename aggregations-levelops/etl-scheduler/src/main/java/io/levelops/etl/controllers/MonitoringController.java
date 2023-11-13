package io.levelops.etl.controllers;

import com.google.common.collect.ImmutableMap;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionFilter;
import io.levelops.aggregations_shared.database.models.DbJobInstanceFilter;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.ListResponse;
import io.levelops.etl.utils.SchedulingUtils;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;


@Log4j2
@RestController
@RequestMapping("/v1/monitoring")
public class MonitoringController {
    int MAX_LOOKBACK = 500;
    int PAGE_SIZE = 25;
    private final JobInstanceDatabaseService jobInstanceDatabaseService;
    private final JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private final ControlPlaneService controlPlaneService;
    private final SchedulingUtils schedulingUtils;

    public MonitoringController(
            JobInstanceDatabaseService jobInstanceDatabaseService,
            JobDefinitionDatabaseService jobDefinitionDatabaseService,
            ControlPlaneService controlPlaneService,
            SchedulingUtils schedulingUtils) {
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
        this.jobDefinitionDatabaseService = jobDefinitionDatabaseService;
        this.controlPlaneService = controlPlaneService;
        this.schedulingUtils = schedulingUtils;
    }


    @GetMapping("/job_instances")
    public ListResponse<DbJobInstance> getJobInstances(
            @RequestParam(value = "job_definition_id", required = true) String jobDefinitionId,
            @RequestParam(value = "page_size", required = false) Integer pageSize,
            @RequestParam(value = "page", required = false) Integer pageNumber
    ) {
        pageNumber = ObjectUtils.defaultIfNull(pageNumber, 0);
        pageSize = ObjectUtils.defaultIfNull(pageSize, PAGE_SIZE);
        var jobDefinition = jobDefinitionDatabaseService.get(UUID.fromString(jobDefinitionId)).get();
        var jobInstanceList = jobInstanceDatabaseService.filter(pageNumber, pageSize, DbJobInstanceFilter.builder()
                .jobDefinitionIds(List.of(jobDefinition.getId()))
                .orderBy(List.of(DbJobInstanceFilter.JobInstanceOrderByField.builder()
                        .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.INSTANCE_ID)
                        .isAscending(false)
                        .build()))
                .build()
        ).getRecords();

        return ListResponse.of(jobInstanceList);
    }

    @GetMapping("/job_definitions")
    public ListResponse<DbJobDefinition> getJobDefinitions(
            @RequestParam(value = "tenant_id", required = true) String tenantId
    ) {
        var jobDefinitionList = jobDefinitionDatabaseService.filter(0, 100, DbJobDefinitionFilter.builder()
                .tenantIds(List.of(tenantId))
                .build()).getRecords();

        return ListResponse.of(jobDefinitionList);
    }

    private DbJobDefinition getJobDefinition(
            @Nullable String jobDefinitionId,
            @Nullable String tenantId,
            @Nullable String integrationId) {
        if (StringUtils.isNotEmpty(jobDefinitionId)) {
            return jobDefinitionDatabaseService.get(UUID.fromString(jobDefinitionId)).get();
        } else if (StringUtils.isNotEmpty(tenantId) && StringUtils.isNotEmpty(integrationId)) {
            var records = jobDefinitionDatabaseService.filter(0, 1, DbJobDefinitionFilter.builder()
                    .tenantIdIntegrationIdPair(Pair.of(tenantId, integrationId))
                    .jobTypes(List.of(JobType.INGESTION_RESULT_PROCESSING_JOB))
                    .build()).getRecords();
            if (records.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ETL Job definition not found");
            } else {
                return records.get(0);
            }
        } else {
            throw new IllegalArgumentException(
                    "Either the job definition id or tenant id + integration id need to be provided");
        }
    }

    private Map<String, Object> summarizeIngestionJob(JobDTO jobDTO) {
        if (jobDTO == null) {
            return Map.of();
        }
        var querySummary = new HashMap<String, Object>();
        if (jobDTO.getQuery() instanceof Map query) {
            if (query.containsKey("from") && query.get("from") instanceof Long) {
                querySummary.put("from", Instant.ofEpochMilli((Long) query.get("from")).toString());
            }
            if (query.containsKey("to") && query.get("to") instanceof Long) {
                querySummary.put("to", Instant.ofEpochMilli((Long) query.get("to")).toString());
            }
        }
        return ImmutableMap.of(
                "id", jobDTO.getId(),
                "created_at", Instant.ofEpochSecond(jobDTO.getCreatedAt()).toString(),
                "query", querySummary
        );
    }

    private Map<String, String> summarizeEtlJobInstance(@Nullable DbJobInstance instance) {
        if (instance == null) {
            return Map.of();
        }
        return ImmutableMap.of(
                "id", instance.getJobInstanceId().toString(),
                "created_at", instance.getCreatedAt().toString()
        );
    }

    @GetMapping("/job_summary")
    public Map<String, Object> getJobDefinitionSummary(
            @RequestParam(value = "job_definition_id", required = false) String jobDefinitionId,
            @RequestParam(value = "tenant_id", required = false) String tenantId,
            @RequestParam(value = "integration_id", required = false) String integrationId
    ) throws IngestionServiceException {
        var jobDefinition = getJobDefinition(jobDefinitionId, tenantId, integrationId);
        var jobInstanceList = jobInstanceDatabaseService.stream(DbJobInstanceFilter.builder()
                .jobDefinitionIds(List.of(jobDefinition.getId()))
                .jobStatuses(List.of(JobStatus.SUCCESS, JobStatus.PARTIAL_SUCCESS, JobStatus.FAILURE))
                .orderBy(List.of(DbJobInstanceFilter.JobInstanceOrderByField.builder()
                        .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.INSTANCE_ID)
                        .isAscending(false)
                        .build()))
                .build()
        ).toList();
        var nonTerminalJobInstanceList = jobInstanceDatabaseService.stream(DbJobInstanceFilter.builder()
                .jobDefinitionIds(List.of(jobDefinition.getId()))
                .jobStatuses(List.of(JobStatus.SCHEDULED, JobStatus.PENDING))
                .orderBy(List.of(DbJobInstanceFilter.JobInstanceOrderByField.builder()
                        .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.INSTANCE_ID)
                        .isAscending(false)
                        .build()))
                .build()
        ).toList();
        var scheduledJobInstanceList = nonTerminalJobInstanceList.stream()
                .filter(instance -> instance.getStatus().equals(JobStatus.SCHEDULED))
                .map(this::summarizeEtlJobInstance)
                .toList();
        var pendingJobInstanceList = nonTerminalJobInstanceList.stream()
                .filter(instance -> instance.getStatus().equals(JobStatus.PENDING))
                .map(this::summarizeEtlJobInstance)
                .toList();

        TriggerResults result = controlPlaneService.getAllTriggerResults(
                IntegrationKey.builder()
                        .tenantId(jobDefinition.getTenantId()).integrationId(jobDefinition.getIntegrationId()).build(),
                false,
                false,
                true
        ).getTriggerResults().get(0);
        var ingestionJobIdToDto = result.getJobs().stream().collect(Collectors.toMap(JobDTO::getId, Function.identity()));
        var allProcessedIngestionJobIds = jobInstanceList.stream()
                .map(DbJobInstance::getFullIngestionJobIds)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Map<String, Map<String, Object>> unprocessedIngestionJobMap = new HashMap<>();
        var latestIngestionJob = summarizeIngestionJob(
                ingestionJobIdToDto.values().stream().max(Comparator.comparing(JobDTO::getCreatedAt)).orElse(null));

        ingestionJobIdToDto.forEach((id, dto) -> {
            if (!allProcessedIngestionJobIds.contains(id)) {
                var summary = summarizeIngestionJob(dto);
                unprocessedIngestionJobMap.put(id, summary);
            }
        });

        var latestSuccessfulFull = summarizeEtlJobInstance(
                jobInstanceList.stream()
                        .filter(instance -> instance.getIsFull() && instance.getStatus().equals(JobStatus.SUCCESS))
                        .findFirst().orElse(null));
        var latestSuccessfulInstance = summarizeEtlJobInstance(
                jobInstanceList.stream()
                        .filter(instance -> instance.getStatus().equals(JobStatus.SUCCESS))
                        .findFirst().orElse(null));

        long successCount = jobInstanceList.stream().filter(instance -> instance.getStatus().equals(JobStatus.SUCCESS)).count();
        long partialSuccessCount = jobInstanceList.stream().filter(instance -> instance.getStatus().equals(JobStatus.PARTIAL_SUCCESS)).count();
        long failureCount = jobInstanceList.stream().filter(instance -> instance.getStatus().equals(JobStatus.FAILURE)).count();

        return ImmutableMap.of(
                "job_definition_id", jobDefinition.getId(),
                "etl_success_count", successCount,
                "etl_partial_success_count", partialSuccessCount,
                "etl_failure_count", failureCount,
                "etl_latest_successful", latestSuccessfulInstance,
                "etl_latest_successful_full", latestSuccessfulFull,
                "etl_currently_running", pendingJobInstanceList,
                "etl_currently_scheduled", scheduledJobInstanceList,
                "unprocessed_ingestion_jobs", unprocessedIngestionJobMap.values(),
                "latest_ingestion_job", latestIngestionJob
        );
    }

    @PostMapping("/schedule_full")
    public JobInstanceId scheduleFullJobInstance(
            @RequestParam(value = "job_definition_id", required = false) String jobDefinitionId,
            @RequestParam(value = "tenant_id", required = false) String tenantId,
            @RequestParam(value = "integration_id", required = false) String integrationId,
            @RequestParam(value = "request_reprocessing", required = false, defaultValue = "false") boolean reprocessing
            ) throws IOException, IngestionServiceException {
        Instant now = Instant.now();
        var jobDefinition = getJobDefinition(jobDefinitionId, tenantId, integrationId);
        return schedulingUtils.scheduleJobDefinition(jobDefinition, now, false, true, reprocessing);
    }

    @PostMapping("/schedule_incremental")
    public JobInstanceId scheduleIncremental(
            @RequestParam(value = "job_definition_id", required = false) String jobDefinitionId,
            @RequestParam(value = "tenant_id", required = false) String tenantId,
            @RequestParam(value = "integration_id", required = false) String integrationId,
            @RequestParam(value = "request_reprocessing", required = false, defaultValue = "false") boolean requestReprocessing) throws IOException, IngestionServiceException {
        Instant now = Instant.now();
        var jobDefinition = getJobDefinition(jobDefinitionId, tenantId, integrationId);
        return schedulingUtils.scheduleJobDefinition(jobDefinition, now, false, false, requestReprocessing);
    }

    @GetMapping("/ingestion_processing/job_instances")
    public Map<String, DbJobInstance> getIngestionProcessingJobInstances(
            @RequestParam(value = "tenant_id", required = true) String tenantId,
            @RequestParam(value = "integration_id", required = true) String integrationId,
            @RequestParam(value = "ingestion_job_ids", required = true) List<String> ingestionJobIds,
            @RequestParam(value = "fetch_all_fields", defaultValue = "false") Boolean fetchAllFields
    ) {
        var jobDefinition = getJobDefinition(null, tenantId, integrationId);
        var jobInstanceStream = jobInstanceDatabaseService.stream(DbJobInstanceFilter.builder()
                .jobDefinitionIds(List.of(jobDefinition.getId()))
                .orderBy(List.of(DbJobInstanceFilter.JobInstanceOrderByField.builder()
                        .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.INSTANCE_ID)
                        .isAscending(false)
                        .build()))
                .build()
        );
        Map<String, DbJobInstance> ingestionJobIdToJobInstance = new HashMap<>();
        Set<String> ingestionJobIdsSet = new HashSet<>(ingestionJobIds);
        AtomicInteger i = new AtomicInteger();
        // The max lookback is a bit of a hack. Ideally we would keep the cutoff as
        // the earliest ingestion job creation time in the list of ingestion job ids.
        jobInstanceStream
                .takeWhile(instance -> i.getAndIncrement() < MAX_LOOKBACK)
                .forEach(jobInstance -> {
                    jobInstance.getFullIngestionJobIds().forEach(ingestionId -> {
                        if (ingestionJobIdsSet.contains(ingestionId)) {
                            if (!ingestionJobIdToJobInstance.containsKey(ingestionId) || (
                                    jobInstance.getStatus().equals(JobStatus.SUCCESS) &&
                                            !ingestionJobIdToJobInstance.get(ingestionId).getStatus().equals(JobStatus.SUCCESS))) {
                                DbJobInstance jobInstanceToReturn = jobInstance;
                                if (!fetchAllFields) {
                                    jobInstanceToReturn = jobInstance.toBuilder()
                                            .payload(null)
                                            .progressDetails(null)
                                            .build();
                                }
                                ingestionJobIdToJobInstance.put(ingestionId, jobInstanceToReturn);
                            }
                        }
                    });
                });
        return ingestionJobIdToJobInstance;
    }
}
