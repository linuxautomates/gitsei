package io.levelops.controlplane.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.controlplane.database.JobDatabaseService;
import io.levelops.controlplane.discovery.AgentRegistryService;
import io.levelops.controlplane.models.DbJob;
import io.levelops.controlplane.models.DbJobUpdate;
import io.levelops.ingestion.merging.IngestionResultMergingService;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.Job;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.ingestion.utils.JobIdFactory;
import io.levelops.web.exceptions.NotFoundException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.controlplane.database.JobDatabaseService.JobFilter;

@Log4j2
@Service
public class JobTrackingService {

    private static final int PAGE_SIZE = 25;
    private static boolean ENABLE_AGENT_JOB_SUPPORT_CHECK = false; // if false - will always schedule jobs
    private final ObjectMapper objectMapper;
    private final JobDatabaseService jobDatabaseService;
    private final AgentRegistryService agentRegistryService;
    private final IngestionResultMergingService ingestionResultMergingService;

    @Autowired
    public JobTrackingService(ObjectMapper objectMapper,
                              JobDatabaseService jobDatabaseService,
                              AgentRegistryService agentRegistryService,
                              IngestionResultMergingService ingestionResultMergingService) {
        this.objectMapper = objectMapper;
        this.jobDatabaseService = jobDatabaseService;
        this.agentRegistryService = agentRegistryService;
        this.ingestionResultMergingService = ingestionResultMergingService;
    }

    // region multi jobs
    public Stream<DbJob> streamJobsByStatus(JobStatus status, @Nullable Boolean reserved, @Nullable String tenantId, @Nullable List<String> integrationIds) {
        JobFilter filter = JobFilter.builder()
                .statuses(List.of(status))
                .reserved(reserved)
                .tenantId(tenantId)
                .integrationIds(integrationIds)
                .build();
        return jobDatabaseService.streamJobs(PAGE_SIZE, filter);
    }

    public Map<String, JobStatus> scheduleAllJobs() {
        JobFilter filter = JobFilter.builder()
                .statuses(List.of(JobStatus.UNASSIGNED))
                .build();
        return jobDatabaseService.streamJobs(PAGE_SIZE, filter)
                .collect(Collectors.toMap(DbJob::getId, this::scheduleJob));
    }
    // endregion

    public String createJob(CreateJobRequest jobRequest) throws JsonProcessingException {
        String jobId = JobIdFactory.useJobIdOrGenerateNew(jobRequest.getJobId());
        String query = objectMapper.writeValueAsString(jobRequest.getQuery());

        jobDatabaseService.createJob(
                jobId,
                jobRequest.getControllerName(),
                query,
                jobRequest.getTenantId(),
                jobRequest.getIntegrationId(),
                jobRequest.getReserved(),
                jobRequest.getCallbackUrl(),
                jobRequest.getTags());

        log.info("⟜  Successfully created job: id={}, controller={}, tenant={}, integration={}", jobId, jobRequest.getControllerName(), jobRequest.getTenantId(), jobRequest.getIntegrationId());
        return jobId;
    }

    public String createJob(CreateJobRequest jobRequest, boolean schedule) throws JsonProcessingException, NotFoundException {
        String jobId = createJob(jobRequest);
        if (schedule) {
            scheduleJob(jobId);
        }
        return jobId;
    }

    // TODO lock
    // TODO improve scheduling response ?
    public JobStatus scheduleJob(DbJob dbJob) {
        if (ENABLE_AGENT_JOB_SUPPORT_CHECK) {
            if (agentRegistryService.getAgentsByControllerName(dbJob.getControllerName()).isEmpty()) {
                return JobStatus.UNASSIGNED;
            }
        }

        jobDatabaseService.updateJob(dbJob.getId(), DbJobUpdate.builder()
                .status(JobStatus.SCHEDULED)
                .build());
        return JobStatus.SCHEDULED;
    }

    public JobStatus scheduleJob(String jobId) throws NotFoundException {
        // TODO optimize createJob for inline scheduling
        DbJob dbJob = jobDatabaseService.getJobById(jobId).orElseThrow(NotFoundException::new);
        return scheduleJob(dbJob);
    }

    public Optional<DbJob> getJobById(String jobId) {
        return jobDatabaseService.getJobById(jobId);
    }

    public boolean markJobAsAccepted(String jobId, String agentId) {
        return jobDatabaseService.updateJob(jobId, DbJobUpdate.builder()
                .agentId(agentId)
                .status(JobStatus.ACCEPTED)
                .statusCondition(JobStatus.SCHEDULED) // only if job is still in scheduled state
                .incrementAttemptCount(true)
                .build());
    }

    public boolean markJobAsFailed(String jobId) {
        return jobDatabaseService.updateJob(jobId, DbJobUpdate.builder()
                .status(JobStatus.FAILURE)
                .build());
    }


    public boolean updateJobStatus(String jobId, String agentId, JobStatus jobStatus) {
        return jobDatabaseService.updateJob(jobId, DbJobUpdate.builder()
                .agentId(agentId)
                .status(jobStatus)
                .build());
    }

    public boolean hasJobAlreadyCompletedSuccessfully(@Nullable DbJob job) {
        if (job == null) {
            return false;
        }
        boolean jobAlreadyCompletedSuccessfully = (JobStatus.SUCCESS.equals(job.getStatus()));
        log.debug("jobId = {}, existingStatus = {}, jobAlreadyCompletedSuccessfully = {}", job.getId(), job.getStatus(), jobAlreadyCompletedSuccessfully);
        return jobAlreadyCompletedSuccessfully;
    }

    public RefreshJobStatusResult refreshJobStatusFromAgentReport(@Nullable DbJob dbJob, Job job) {

        JobStatus status;
        boolean updateResults = false;
        if (!job.isDone()) {
            status = JobStatus.PENDING;
        } else if (job.isCancelled()) {
            status = JobStatus.ABORTED;
        } else if (job.isSuccessful()) {
            status = JobStatus.SUCCESS;
            updateResults = true;
        } else {
            status = JobStatus.FAILURE;
            updateResults = true;
        }

        DbJobUpdate jobUpdate;
        if (updateResults) {
            Map<String, Object> mergedResults = mergeResults(dbJob, job);
            jobUpdate = DbJobUpdate.builder()
                    .agentId(job.getAgentId())
                    .status(status)
                    .result(mergedResults)
                    .ingestionFailures(job.getIngestionFailures())
                    .error(ParsingUtils.toJsonObject(objectMapper, job.getException()))
                    .intermediateState(job.getIntermediateState())
                    .build();
            if (MapUtils.isNotEmpty(job.getIntermediateState())) {
                // DEBUG remove this
                log.info(">>>> jobId={}, intermediateState={}, oldResults={}, newResults={}, merged={}", job.getId(), job.getIntermediateState(), dbJob != null? dbJob.getResult() : null, job.getResult(), mergedResults);
            }
        }

        // Commenting out because there's more work that needs to be done to truly enable realtime checkpointing
        // https://harness.atlassian.net/browse/SEI-3852
        /*
        else if (status.equals(JobStatus.PENDING)) {
            jobUpdate = DbJobUpdate.builder()
                    .agentId(job.getAgentId())
                    .status(status)
                    .intermediateState(job.getIntermediateState())
                    .build();
            if (MapUtils.isNotEmpty(job.getIntermediateState())) {
                log.info(">>>> Pending job, intermediate state update - jobId={}, intermediateState={}", job.getId(), job.getIntermediateState());
            }
        }
        */
        else {
            // make sure not to clear eventual previous results if there is nothing new
            jobUpdate = DbJobUpdate.builder()
                    .agentId(job.getAgentId())
                    .status(status)
                    .build();
        }
        log.debug("Updating jobId={} with {}", job.getId(), jobUpdate);
        boolean updated = jobDatabaseService.updateJob(job.getId(), jobUpdate);

        // TODO error handling? (what to do with missing jobs?, db error?)

        return RefreshJobStatusResult.builder()
                .updated(updated)
                .status(status)
                .results(updateResults)
                .intermediateState(MapUtils.isNotEmpty(job.getIntermediateState()))
                .build();
    }

    private Map<String, Object> mergeResults(@Nullable DbJob dbJob, Job job) {
        // this will only merge when possible
        return ingestionResultMergingService.merge(dbJob != null ? dbJob.getResult() : null, job.getResult());
    }

    @Value
    @Builder
    public static class RefreshJobStatusResult {
        boolean updated;
        boolean results;
        boolean intermediateState;
        JobStatus status;
    }
}
