package io.levelops.controlplane.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.controlplane.database.JobDatabaseService;
import io.levelops.controlplane.database.TriggeredJobDatabaseService;
import io.levelops.controlplane.database.TriggeredJobDatabaseService.TriggeredJobFilter;
import io.levelops.controlplane.models.DbJob;
import io.levelops.controlplane.models.DbJobUpdate;
import io.levelops.controlplane.models.DbTriggeredJob;
import io.levelops.controlplane.models.JobDTOConverters;
import io.levelops.controlplane.services.JobTrackingService;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping("/v1/jobs")
public class JobManagementController {

    private static final int PAGE_SIZE = 25;
    private final JobDatabaseService jobDatabaseService;
    private final JobTrackingService jobTrackingService;
    private final TriggeredJobDatabaseService triggeredJobDatabaseService;

    @Autowired
    public JobManagementController(JobDatabaseService jobDatabaseService,
                                   JobTrackingService jobTrackingService,
                                   TriggeredJobDatabaseService triggeredJobDatabaseService) {
        this.jobDatabaseService = jobDatabaseService;
        this.jobTrackingService = jobTrackingService;
        this.triggeredJobDatabaseService = triggeredJobDatabaseService;
    }

    @GetMapping
    public PaginatedResponse<JobDTO> getJobs(@RequestParam(value = "page", required = false) Integer pageNumber,
                                             @RequestParam(value = "page_size", required = false) Integer pageSize,
                                            //  @ApiParam(allowEmptyValue = true, allowableValues = JobStatus.SWAGGER_VALUES)
                                             @RequestParam(value = "status", required = false) String status,
                                             @RequestParam(value = "statuses", required = false) List<String> statuses,
                                             @RequestParam(value = "tenant_id", required = false) String tenantId,
                                            //  @ApiParam(value = "<b><font color=\"red\">REQUIRES TENANT_ID</font></b>")
                                             @RequestParam(value = "integration_id", required = false) String integrationId,
                                            //  @ApiParam(allowEmptyValue = true, allowableValues = "null,false,true")
                                             @RequestParam(value = "reserved", required = false) String reserved,
                                            //  @ApiParam(value = "<b><font color=\"red\">SUPERSEDES OTHER FILTERS</font></b><br/>(works with status)")
                                             @RequestParam(value = "trigger_id", required = false) String triggerId,
                                            //  @ApiParam("updated after<br/>(epoch seconds)") 
                                            @RequestParam(value = "after", required = false) Long updatedAfter,
                                            //  @ApiParam("updated before<br/>(epoch seconds)") 
                                            @RequestParam(value = "before", required = false) Long updatedBefore,
                                            //  @ApiParam(hidden = true) 
                                            @RequestParam(value = "return_total_count", required = false) Boolean returnTotalCount,
                                             @RequestParam(value = "include_job_result_field", required = false, defaultValue = "true") boolean includeJobResultField) {
        pageNumber = ObjectUtils.defaultIfNull(pageNumber, 0);
        pageSize = ObjectUtils.defaultIfNull(pageSize, PAGE_SIZE);

        List<JobStatus> statusesFilter;
        if (CollectionUtils.isNotEmpty(statuses)) {
            statusesFilter = statuses.stream()
                    .map(JobStatus::fromString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            JobStatus jobStatus = JobStatus.fromString(status);
            statusesFilter = jobStatus != null ? List.of(jobStatus) : null;
        }

        if (Strings.isNotEmpty(triggerId)) {
            TriggeredJobFilter filter = TriggeredJobFilter.builder()
                    .statuses(statusesFilter)
                    .afterExclusive(updatedAfter)
                    .beforeInclusive(updatedBefore)
                    .returnTotalCount(returnTotalCount)
                    .build();
            DbListResponse<DbTriggeredJob> jobsResponse = triggeredJobDatabaseService.filterTriggeredJobs(pageNumber * pageSize, pageSize, triggerId, filter, null);
            List<JobDTO> jobs = jobsResponse.getRecords().stream()
                    .map(triggeredJob -> jobDatabaseService.getJobMetadataById(triggeredJob.getJobId(), true, includeJobResultField)
                            .map(dbJob -> JobDTOConverters.convertFromDbJobAndTriggeredJob(dbJob, triggeredJob))
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return PaginatedResponse.of(pageNumber, pageSize, jobsResponse.getTotalCount(), jobs);
        }

        List<String> integrationIds = Strings.isNotEmpty(integrationId) ? List.of(integrationId) : null;
        Boolean reservedBool = "null".equalsIgnoreCase(reserved) ? null : Boolean.parseBoolean(reserved);

        JobDatabaseService.JobFilter filter = JobDatabaseService.JobFilter.builder()
                .statuses(statusesFilter)
                .reserved(reservedBool)
                .tenantId(tenantId)
                .integrationIds(integrationIds)
                .after(updatedAfter != null ? DateUtils.fromEpochSecond(updatedAfter) : null)
                .before(updatedBefore != null ? DateUtils.fromEpochSecond(updatedBefore) : null)
                .build();
        List<JobDTO> jobs = jobDatabaseService.filterJobs(pageNumber * pageSize, pageSize, filter).stream()
                .map(dbJob -> triggeredJobDatabaseService.getTriggeredJobByJobId(dbJob.getId())
                        .map(triggeredJob -> JobDTOConverters.convertFromDbJobAndTriggeredJob(dbJob, triggeredJob))
                        .orElse(JobDTOConverters.convertFromDbJob(dbJob)))
                .collect(Collectors.toList());
        return PaginatedResponse.of(pageNumber, pageSize, jobs);
    }

    @PostMapping
    public SubmitJobResponse createJob(@RequestBody CreateJobRequest jobRequest,
                                       @RequestParam(value = "schedule", defaultValue = "true") boolean schedule) throws BadRequestException, NotFoundException {

        // TODO move to jobTrackingService

        String jobId;
        try {
            jobId = jobTrackingService.createJob(jobRequest);
        } catch (JsonProcessingException e) {
            throw new BadRequestException(e);
        }

        JobStatus scheduleStatus = JobStatus.UNASSIGNED;
        if (schedule) {
            scheduleStatus = jobTrackingService.scheduleJob(jobId);
        }

        return SubmitJobResponse.builder()
                .jobId(jobId)
                .status(scheduleStatus)
                .build();
    }

    @GetMapping("/schedule")
    public Map<String, JobStatus> scheduleAllJobs() {
        return jobTrackingService.scheduleAllJobs();
    }

    @GetMapping("/{jobId}")
    public DbJob getJob(@PathVariable String jobId) throws NotFoundException {
        return jobDatabaseService.getJobById(jobId)
                .orElseThrow(() -> new NotFoundException("Could not find job with id=" + jobId));
    }

    @PutMapping("/{jobId}/retry")
    public Map<String, Object> retryJob(@PathVariable String jobId,
                                        @RequestParam(value = "clearIntermediateState", defaultValue = "true") boolean clearIntermediateState,
                                        @RequestParam(value = "clearPreviousErrors", defaultValue = "true") boolean clearPreviousErrors) throws NotFoundException {
        DbJobUpdate.DbJobUpdateBuilder builder = DbJobUpdate.builder()
                .status(JobStatus.SCHEDULED)
                .attemptCount(0);
        if (clearIntermediateState) {
            builder.intermediateState(Map.of());
            builder.result(Map.of());
        }
        if (clearPreviousErrors) {
            builder.error(Map.of());
            builder.ingestionFailures(List.of());
        }
        boolean success = jobDatabaseService.updateJob(jobId, builder.build());
        if (!success) {
            throw new NotFoundException("Could not find job with id=" + jobId);
        }
        return Map.of("job_id", jobId,
                "status", JobStatus.SCHEDULED,
                "attempt_count", 0);
    }

}
