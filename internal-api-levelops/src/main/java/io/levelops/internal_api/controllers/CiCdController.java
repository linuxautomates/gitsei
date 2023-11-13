package io.levelops.internal_api.controllers;

import io.levelops.cicd.services.CiCdService;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.PathSegment;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Log4j2
@RestController
@RequestMapping("/internal/v1/tenants/{company}/cicd")
public class CiCdController {
    private final CiCdService cicdService;
    private final CiCdJobRunStageDatabaseService jobRunStageDatabaseService;

    @Autowired
    public CiCdController(final CiCdService cicdService,
                          CiCdJobRunStageDatabaseService jobRunStageDatabaseService) {
        this.cicdService = cicdService;
        this.jobRunStageDatabaseService = jobRunStageDatabaseService;
    }

    @GetMapping(path = "/jobs/{jobId}")
    public CICDJob getCiCdJob(@PathVariable("company") final String company,
                              @PathVariable("jobId") final String jobId) throws Exception {
        return cicdService.getCiCdJob(company, jobId);
    }

    @GetMapping(path = "/job_runs/{jobRunId}")
    public CICDJobRun getCiCdJobRun(@PathVariable("company") final String company,
                                    @PathVariable("jobRunId") final String jobRunId) throws Exception {
        return cicdService.getCiCdJobRun(company, jobRunId);
    }

    @GetMapping(path = "/job_run_stages/{stageId}")
    public JobRunStage getJobRunStage(@PathVariable("company") final String company,
                                      @PathVariable("stageId") final String stageId) throws Exception {
        return cicdService.getJobRunStage(company, stageId);
    }

    @PostMapping("/job_runs/{jobRunId}/stages")
    public DeferredResult<ResponseEntity<DbListResponse<JobRunStage>>> listStages(@PathVariable("company") final String company,
                                                                                  @PathVariable("jobRunId") final String jobRunId,
                                                                                  @RequestBody DefaultListRequest defaultListRequest) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(jobRunStageDatabaseService.list(company,
                defaultListRequest.getPage(), defaultListRequest.getPageSize(),
                QueryFilter.fromRequestFilters(MapUtils.emptyIfNull(defaultListRequest.getFilter())).toBuilder()
                        .strictMatch("cicd_job_run_id", UUID.fromString(jobRunId))
                        .build())));
    }

    @GetMapping("/job_runs/{jobRunId}/full_path")
    public Set<PathSegment> getFullPath(@PathVariable("company") final String company,
                                        @PathVariable("jobRunId") final String jobRunId,
                                        @RequestParam(value = "bottom_up", required = false, defaultValue = "false") Boolean bottomUp) throws Exception {
        return cicdService.getJobRunFullPath(company, jobRunId, bottomUp);
    }

    @GetMapping("/job_run_stages/{stageId}/full_path")
    public Set<PathSegment> getStageFullPath(@PathVariable("company") final String company,
                                             @PathVariable("stageId") final String stageId,
                                             @RequestParam(value = "bottom_up", required = false, defaultValue = "false") Boolean bottomUp) throws Exception {
        return cicdService.getJobStageFullPath(company, stageId, bottomUp);
    }

}