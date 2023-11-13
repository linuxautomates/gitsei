package io.levelops.controlplane.controllers;

import io.levelops.commons.models.ListResponse;
import io.levelops.controlplane.discovery.AgentRegistryService;
import io.levelops.controlplane.discovery.RegisteredAgent;
import io.levelops.controlplane.models.DbJob;
import io.levelops.controlplane.models.DbJobConverters;
import io.levelops.controlplane.services.JobTrackingService;
import io.levelops.controlplane.services.JobTrackingService.RefreshJobStatusResult;
import io.levelops.controlplane.services.TriggerResultService;
import io.levelops.ingestion.models.AgentHandle;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.Job;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.ConflictException;
import io.levelops.web.exceptions.ForbiddenException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/agent-callback")
public class AgentCallbackController {

    private static final int LIMIT_JOB_REQUESTS_RETURNED = 25;
    private final AgentRegistryService agentRegistryService;
    private final JobTrackingService jobTrackingService;
    private final TriggerResultService triggerResultService;

    @Autowired
    public AgentCallbackController(final AgentRegistryService agentRegistryService,
                                   final JobTrackingService jobTrackingService,
                                   final TriggerResultService triggerResultService) {
        this.agentRegistryService = agentRegistryService;
        this.jobTrackingService = jobTrackingService;
        this.triggerResultService = triggerResultService;
    }

    private void validateTenantId(String tenantId, String jobId) throws ForbiddenException, NotFoundException {
        // Only validating when tenantId is present so that cloud agents are not restricted.
        // For satellites, the presence of tenantId is enforced by the server API.
        if (Strings.isNotEmpty(tenantId)) {
            DbJob job = jobTrackingService.getJobById(jobId)
                    .orElseThrow(() -> new NotFoundException("Could not find job with id=" + jobId));
            if (!tenantId.equals(job.getTenantId())) {
                throw new ForbiddenException("Invalid tenant");
            }
        }
    }

    @PostMapping("/register")
    public void register(@RequestBody AgentHandle agentHandle) throws BadRequestException {
        try {
            BadRequestException.checkNotNull(agentHandle.getAgentId(), "agent_id is required");
            agentRegistryService.registerAgent(agentHandle);
        } catch (IllegalArgumentException | BadRequestException e) {
            log.error("registration failed for: {}", agentHandle, e);
            throw new BadRequestException("Cannot register Agent: " + e.getMessage(), e);
        }
    }

    /**
     * For legacy agents.
     */
    @GetMapping("/heartbeat")
    public RegisteredAgent heartbeat(@RequestParam("agent_id") String agentId,
                                     @RequestParam(value = "tenant_id", required = false) String tenantId) throws ConflictException, ForbiddenException {
        return heartbeatAndTelemetry(AgentHandle.builder()
                .agentId(agentId)
                .tenantId(tenantId)
                .build());
    }

    @PostMapping("/heartbeat")
    public RegisteredAgent heartbeatAndTelemetry(@RequestBody AgentHandle agentHandle) throws ConflictException, ForbiddenException {
        String agentId = agentHandle.getAgentId();
        String tenantId = agentHandle.getTenantId(); // optional
        try{
            Validate.notBlank(agentHandle.getAgentId(), "agentHandle.getAgentId() cannot be null or empty.");

            if (Strings.isNotEmpty(tenantId)) {
                String currentTenantId = agentRegistryService.getAgentById(agentId)
                        .map(RegisteredAgent::getAgentHandle)
                        .map(AgentHandle::getTenantId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Agent not found..."));
                if (!Objects.equals(currentTenantId, tenantId)) {
                    throw new ForbiddenException("Invalid tenant");
                }
            }
            return agentRegistryService.refreshHeartbeatAndTelemetry(agentHandle)
                    .orElseThrow(() -> new ConflictException("Agent not yet registered: " + agentId));
        }
        catch(Exception e){
            log.error("heartbeat call failed for agent: {}", agentHandle.toBuilder().telemetry(Collections.emptyMap()).build(), e);
            throw e;
        }
    }

    /**
     * Report jobs and return map of jobId (String) <-> acknowledged (Boolean)
     */
    @PostMapping("/jobs/report")
    public Map<String, Boolean> reportJobs(@RequestBody ListResponse<Job> jobs,
                                           @RequestParam(value = "tenant_id", required = false) String tenantId) throws ForbiddenException {
        Set<String> jobIdsNotFound = new HashSet<>();
        if (Strings.isNotEmpty(tenantId)) {
            for (Job job : jobs.getRecords()) {
                try {
                    validateTenantId(tenantId, job.getId());
                } catch (NotFoundException e) {
                    jobIdsNotFound.add(job.getId());
                    log.warn("An agent reported a job that was not found in the control plane: tenant_id={}, job_id={}", tenantId, job.getId());
                }
            }
        }

        return jobs.getRecords().stream().collect(
                Collectors.toMap(Job::getId, job -> {
                    if (jobIdsNotFound.contains(job.getId())) {
                        // if a job was not found, we will report it as acknowledged so that the agent discard it
                        // TODO add a better flow to the satellite to discard jobs when they are not found in the control plane
                        return true;
                    }
                    return reportJob(job);
                }));
    }

    private boolean reportJob(Job job) {
        DbJob dbJob = jobTrackingService.getJobById(job.getId()).orElse(null);
        log.debug("jobId={}, dbJob = {}", job.getId(), dbJob);

        if (jobTrackingService.hasJobAlreadyCompletedSuccessfully(dbJob)) {
            log.info("job_id={} has already completed successfully!", job.getId());
            return true; //Acknowledge to Agent or else it will keep trying.
        }

        RefreshJobStatusResult refreshJobStatusResult = jobTrackingService.refreshJobStatusFromAgentReport(dbJob, job);
        if (!refreshJobStatusResult.isUpdated()) {
            return false; // not acknowledged
        }
        log.debug("Updated jobId={} with new status={} (updatedResults={}, intermediateState={})", job.getId(), refreshJobStatusResult.getStatus(), refreshJobStatusResult.isResults(), refreshJobStatusResult.isIntermediateState());

        try {
            triggerResultService.reportTriggerResults(job.getId(), refreshJobStatusResult.getStatus());
        } catch (Exception e) {
            log.warn("Failed to report trigger results for job_id={}", job.getId(), e);
        }

        return true; // acknowledged
    }

    /**
     * Returns "lite" request (just id and controller)
     *
     * @return
     */
    @PostMapping("/jobs/requests/list")
    public ListResponse<CreateJobRequest> listJobRequests(
            // @ApiParam(allowEmptyValue = true, defaultValue = "false", type = "boolean")
            @RequestParam(value = "reserved", required = false, defaultValue = "false") Boolean reserved,
            @RequestBody AgentHandle agentHandle) {
        return ListResponse.of(jobTrackingService.streamJobsByStatus(JobStatus.SCHEDULED, reserved, agentHandle.getTenantId(), agentHandle.getIntegrationIds())
                .filter(dbJob -> agentHandle.getControllerNames().contains(dbJob.getControllerName()))
                .map(dbJob -> CreateJobRequest.builder()
                        .jobId(dbJob.getId())
                        .controllerName(dbJob.getControllerName())
                        .build())
                .limit(LIMIT_JOB_REQUESTS_RETURNED)
                .collect(Collectors.toList()));
    }

    @GetMapping("/jobs/requests/accept")
    public CreateJobRequest acceptJobRequest(@RequestParam("job_id") String jobId,
                                             @RequestParam("agent_id") String agentId,
                                             @RequestParam(value = "tenant_id", required = false) String tenantId) throws ForbiddenException, NotFoundException {
        validateTenantId(tenantId, jobId);

        if (!jobTrackingService.markJobAsAccepted(jobId, agentId)) {
            throw new NotFoundException("Cannot accept job request: not found or already accepted");
        }
        return jobTrackingService.getJobById(jobId)
                .map(DbJobConverters::convertDbJobToRequest)
                .orElseThrow(() -> new RuntimeException("Failed to get job with id: " + jobId));
    }

    @GetMapping("/jobs/requests/reject")
    public void rejectJobRequest(@RequestParam("job_id") String jobId,
                                 @RequestParam("agent_id") String agentId,
                                 @RequestParam("status") String status,
                                 @RequestParam(value = "tenant_id", required = false) String tenantId) throws ForbiddenException, BadRequestException, NotFoundException {
        validateTenantId(tenantId, jobId);

        JobStatus jobStatus = JobStatus.fromString(status);
        if (jobStatus == null) {
            throw new BadRequestException("Invalid status: " + status);
        }
        if (!jobTrackingService.updateJobStatus(jobId, agentId, jobStatus)) {
            throw new NotFoundException("Cannot accept job request: not found");
        }
    }
}
