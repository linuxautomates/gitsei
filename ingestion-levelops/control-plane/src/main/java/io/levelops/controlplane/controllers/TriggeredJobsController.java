package io.levelops.controlplane.controllers;

import io.levelops.commons.models.ListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.controlplane.database.TriggeredJobDatabaseService;
import io.levelops.controlplane.database.TriggeredJobDatabaseService.TriggeredJobFilter;
import io.levelops.controlplane.models.DbIteration;
import io.levelops.controlplane.models.DbTriggeredJob;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Log4j2
@RestController
@RequestMapping("/v1")
public class TriggeredJobsController {

    private static final int PAGE_SIZE = 25;
    private final TriggeredJobDatabaseService triggeredJobDatabaseService;

    @Autowired
    public TriggeredJobsController(TriggeredJobDatabaseService triggeredJobDatabaseService) {
        this.triggeredJobDatabaseService = triggeredJobDatabaseService;
    }

    @GetMapping("/iterations/{iteration_id}/triggered_jobs")
    public ListResponse<DbTriggeredJob> getTriggeredJobsByIterationId(@PathVariable("iteration_id") String iterationId) {
        return ListResponse.of(triggeredJobDatabaseService.getTriggeredJobsByIterationId(iterationId));
    }

    @GetMapping("/triggered_jobs/{jobId}")
    public DbTriggeredJob getTriggeredJob(@PathVariable("jobId") String jobId) throws NotFoundException {
        return triggeredJobDatabaseService.getTriggeredJobByJobId(jobId)
                .orElseThrow(() -> new NotFoundException("Could not find triggered_job with job_id=" + jobId));
    }


    @GetMapping("/iterations")
    public PaginatedResponse<DbIteration> getIterations(@RequestParam("trigger_id") String triggerId,
                                                        @RequestParam(value = "page", required = false, defaultValue = "0") Integer page) {
        return PaginatedResponse.of(page, PAGE_SIZE,
                triggeredJobDatabaseService.getIterationsByTriggerId(triggerId, page * PAGE_SIZE, PAGE_SIZE));
    }

    @GetMapping("/triggers/{triggerId}/triggered_jobs")
    public PaginatedResponse<DbTriggeredJob> getTriggeredJobsByTriggerId(@PathVariable("triggerId") String triggerId,
                                                                        //  @ApiParam(allowEmptyValue = true, allowableValues = JobStatus.SWAGGER_VALUES)
                                                                         @RequestParam(value = "status", required = false) String status,
                                                                         @RequestParam(value = "page", required = false, defaultValue = "0") Integer page) {
        List<JobStatus> statusFilter = null;
        JobStatus jobStatus = JobStatus.fromString(status);
        if (jobStatus != null) {
            statusFilter = List.of(jobStatus);
        }
        return PaginatedResponse.of(page, PAGE_SIZE,
                triggeredJobDatabaseService.filterTriggeredJobs(page * PAGE_SIZE, PAGE_SIZE, triggerId, TriggeredJobFilter.builder()
                        .statuses(statusFilter)
                        .build(), null));
    }
}
