package io.levelops.etl.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionFilter;
import io.levelops.aggregations_shared.database.models.DbJobInstanceFilter;
import io.levelops.aggregations_shared.database.models.DbJobInstanceUpdate;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.etl.exceptions.InvalidJobInstanceIdException;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.models.ListResponse;
import io.levelops.etl.utils.SchedulingUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping("/v1/scheduler")
public class SchedulingController {
    private final JobInstanceDatabaseService jobInstanceDatabaseService;
    private final JobDefinitionDatabaseService jobDefinitionDatabaseService;

    // This cache only stores one value. But we still use a cache to get the
    // TTL expiry and concurrency benefits of guava. When we move to a distributed
    // scheduler model, this should probably move to redis
    LoadingCache<String, ListResponse<JobContext>> nextJobsToRunCache;
    private static final String DUMMY_CACHE_KEY = "dummy";

    public SchedulingController(
            JobInstanceDatabaseService jobInstanceDatabaseService,
            JobDefinitionDatabaseService jobDefinitionDatabaseService,
            @Value("${SCHEDULING_CONTROLLER_CACHE_TTL_MINUTES:2}") Integer cacheTtl) {
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
        this.jobDefinitionDatabaseService = jobDefinitionDatabaseService;

        log.info("Scheduling controller cache TTL set to {} minutes", cacheTtl);
        nextJobsToRunCache = CacheBuilder.newBuilder()
                .maximumSize(2)
                .expireAfterWrite(Duration.ofMinutes(cacheTtl))
                .build(CacheLoader.from(dummyKey -> fetchJobsToRun()));
    }

    @GetMapping("/jobs_to_run")
    public ListResponse<JobContext> getJobsToRun(@RequestParam(defaultValue = "false") Boolean disableCache) throws ExecutionException {
        if (disableCache) {
            log.info("/job_to_run bypassing cache");
            nextJobsToRunCache.invalidateAll();
        }
        return nextJobsToRunCache.get(DUMMY_CACHE_KEY);
    }

    private ListResponse<JobContext> fetchJobsToRun() {
        Instant now = Instant.now();
        List<DbJobInstance> jobInstances = jobInstanceDatabaseService.stream(DbJobInstanceFilter.builder()
                .jobStatuses(List.of(JobStatus.SCHEDULED))
                .scheduledTimeAtOrBefore(now)
                .excludePayload(true)
                .orderBy(List.of(DbJobInstanceFilter.JobInstanceOrderByField.builder()
                                .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.PRIORITY)
                                .isAscending(true)
                                .build(),
                        DbJobInstanceFilter.JobInstanceOrderByField.builder()
                                .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.SCHEDULED_START_TIME)
                                .isAscending(true)
                                .build()))
                .build()).toList();

        Map<UUID, DbJobDefinition> jobDefinitionMap = jobDefinitionDatabaseService.stream(DbJobDefinitionFilter.builder()
                        .ids(jobInstances.stream().map(DbJobInstance::getJobDefinitionId).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toMap(DbJobDefinition::getId, Function.identity()));

        return ListResponse.of(jobInstances.stream()
                .map(jobInstance -> {
                    if (jobDefinitionMap.containsKey(jobInstance.getJobDefinitionId())) {
                        return SchedulingUtils.getJobContext(jobDefinitionMap.get(jobInstance.getJobDefinitionId()), jobInstance);
                    } else {
                        log.error("Unable to fetch job definition from db: {}", jobInstance.getJobDefinitionId());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
        );
    }

    @PatchMapping("/claim_job")
    public void claimJob(@RequestParam("job_instance_id") String jobInstanceIdStr,
                         @RequestParam("worker_id") String workerId) throws JsonProcessingException, io.levelops.commons.etl.exceptions.InvalidJobInstanceIdException {
        JobInstanceId jobInstanceId = JobInstanceId.fromString(jobInstanceIdStr);
        DbJobInstance jobInstance = jobInstanceDatabaseService.get(jobInstanceId).orElseThrow();
        DbJobDefinition jobDefinition = jobDefinitionDatabaseService.get(jobInstance.getJobDefinitionId()).orElseThrow();
        if (!jobDefinition.getIsActive()) {
            log.info("Job definition {} for tenant {} integration id {} processor {} is not active. Will not claim job",
                    jobDefinition.getId(), jobDefinition.getTenantId(), jobDefinition.getIntegrationId(), jobDefinition.getAggProcessorName());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job Definition is not active");
        } else if (!Set.of(JobStatus.UNASSIGNED, JobStatus.SCHEDULED).contains(jobInstance.getStatus())) {
            log.debug("Job instance {} for tenant {} integration id {} processor {} is already running. Current status: {}",
                    jobInstance.getJobInstanceId().toString(), jobDefinition.getTenantId(), jobDefinition.getIntegrationId(), jobInstance.getStatus(), jobDefinition.getAggProcessorName());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job instance is already running. Current status: " + jobInstance.getStatus());
        }

        Boolean updated = jobInstanceDatabaseService.update(jobInstanceId, DbJobInstanceUpdate.builder()
                .jobStatus(JobStatus.ACCEPTED)
                .workerId(workerId)
                .statusCondition(jobInstance.getStatus())
                .build());

        if (BooleanUtils.isFalse(updated)) {
            log.info("Unable to claim job instance {}", jobInstance.getJobInstanceId().toString());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to claim job");
        }
    }

    @PatchMapping("/unclaim_job")
    public void unclaimJob(@RequestParam("job_instance_id") String jobInstanceIdStr,
                           @RequestParam("worker_id") String workerId) throws JsonProcessingException, InvalidJobInstanceIdException {
        JobInstanceId jobInstanceId = JobInstanceId.fromString(jobInstanceIdStr);
        DbJobInstance jobInstance = jobInstanceDatabaseService.get(jobInstanceId).orElseThrow();
        if (!jobInstance.getWorkerId().equals(workerId)) {
            log.info("Job instance {} belongs to worker {}, but the worker trying to unclaim job is {}",
                    jobInstance.getJobInstanceId().toString(), jobInstance.getWorkerId(), workerId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job instance does not belong to worker " + workerId);
        }

        Boolean updated = jobInstanceDatabaseService.update(jobInstanceId, DbJobInstanceUpdate.builder()
                .jobStatus(JobStatus.SCHEDULED)
                .workerId("")
                .workerIdCondition(workerId)
                .build());

        if (BooleanUtils.isFalse(updated)) {
            log.info("Unable to unclaim job instance {}", jobInstance.getJobInstanceId().toString());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to claim job");
        }
    }
}
