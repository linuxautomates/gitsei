package io.levelops.internal_api.services;

import io.levelops.cicd.services.CiCdService;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CICDJobTrigger;
import io.levelops.commons.databases.models.database.CiCdScmMapping;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.cicd.CICDJobRunCommits;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.PathSegment;
import io.levelops.commons.databases.models.database.cicd.SegmentType;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.CiCdScmMappingService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Log4j2
public class LocalCiCdService implements CiCdService {
    private static final int FULL_PATH_MAX_LEVELS = 512; // safe guard to prevent infinite loops
    private static final String DEFAULT_JOB_NAME = "Unknown Job";
    private static final String DEFAULT_REPO_NAME = "_DEFAULT_";
    private static final Pattern PATTERN_GIT_HTTP_URL = Pattern.compile("\\b\\/(.+\\/.+)\\.git\\b");
    private static final Pattern PATTERN_GIT_SSH_URL = Pattern.compile("^git@.*\\:(.*).git$");
    private static final Pattern PATTERN_AZURE_DEVOPS_HTTP_URL = Pattern.compile("\\b\\/\\_git\\/(.+)");

    private final CiCdJobsDatabaseService cicdJobsService;
    private final CiCdJobRunsDatabaseService cicdJobRunsDatabaseService;
    private final CiCdScmMappingService cicdScmMappingService;
    private final ScmAggService scmService;
    private final CiCdJobRunStageDatabaseService jobRunStageDatabaseService;
    private final Set<String> skipRepoIdCheckForTenants;

    public LocalCiCdService(final CiCdJobRunsDatabaseService cicdJobRunsDatabaseService,
                            final CiCdScmMappingService cicdScmMappingService,
                            final CiCdJobsDatabaseService cicdJobsService,
                            final ScmAggService scmService,
                            final CiCdJobRunStageDatabaseService jobRunStageDatabaseService,
                            @Value("${CICD_SCM_SKIP_REPO_ID_CHECK_FOR_TENANTS:}") String skipRepoIdCheckForTenantsCommaList) {
        this.cicdJobRunsDatabaseService = cicdJobRunsDatabaseService;
        this.cicdJobsService = cicdJobsService;
        this.cicdScmMappingService = cicdScmMappingService;
        this.scmService = scmService;
        this.jobRunStageDatabaseService = jobRunStageDatabaseService;
        this.skipRepoIdCheckForTenants = CommaListSplitter.splitToStream(skipRepoIdCheckForTenantsCommaList)
                .map(StringUtils::toRootLowerCase)
                .collect(Collectors.toSet());
        log.info("Will skip repoId check for cicd/scm mapping for tenants: {}", skipRepoIdCheckForTenants);
    }

    @Override
    public CICDJob getCiCdJob(String company, String jobId) throws Exception {
        return cicdJobsService.get(company, jobId)
                .orElseThrow(() -> new NotFoundException("Could not find cicd job with id=" + jobId));
    }

    @Override
    public CICDJobRun getCiCdJobRun(String company, String jobRunId) throws Exception {
        return cicdJobRunsDatabaseService.get(company, jobRunId)
                .orElseThrow(() -> new NotFoundException("Could not find cicd job run with id=" + jobRunId));
    }

    @Override
    public JobRunStage getJobRunStage(String company, String stageId) throws Exception {
        return jobRunStageDatabaseService.get(company, stageId)
                .orElseThrow(() -> new NotFoundException("Could not find cicd job stage with id=" + stageId));
    }

    @Override
    public DbListResponse<JobRunStage> listJobRunStages(String company, String jobRunId, DefaultListRequest defaultListRequest) throws InternalApiClientException {
        throw new UnsupportedOperationException();
    }

    static String parseRepoIdFromScmUrl(final UUID jobId, final String scmUrl) {
        log.debug("scmUrl {}", scmUrl);
        if (StringUtils.isBlank(scmUrl)) {
            log.debug("CiCd Job Id {} scm url is blank.", jobId);
            return null;
        }
        //First try to match with Git Http Url Pattern
        var matcher = PATTERN_GIT_HTTP_URL.matcher(scmUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = PATTERN_GIT_SSH_URL.matcher(scmUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = PATTERN_AZURE_DEVOPS_HTTP_URL.matcher(scmUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        log.warn("CiCd Job Id {} scm url may not be supported for format {}", jobId, scmUrl);
        return scmUrl;
    }

    private Optional<String> getValidRepoIdFromJobRun(String company, UUID jobRunId) throws Exception {
        var run = cicdJobRunsDatabaseService.get(company, jobRunId.toString());
        if (run.isEmpty()) {
            throw new SQLException("Unable to find the job run with id " + jobRunId);
        }
        log.debug("run {}", run.get());
        var job = cicdJobsService.get(company, run.get().getCicdJobId().toString());
        if (job.isEmpty()) {
            throw new SQLException("Unable to find the job for the run with id " + jobRunId);
        }
        log.debug("job {}", job.get());
        String parsedRepoId = parseRepoIdFromScmUrl(job.get().getId(), job.get().getScmUrl());
        log.debug("parsedRepoId = {}", parsedRepoId);
        return Optional.ofNullable(parsedRepoId);
    }

    @Override
    public Set<PathSegment> getJobRunFullPath(String company, String startingJobRunId, boolean bottomUp) throws SQLException, NotFoundException {

        String currentJobRunId = startingJobRunId;

        log.debug("Generating full path for job_run_id={}", startingJobRunId);

        Set<String> visitedJobRunIds = new HashSet<>();
        Set<PathSegment> path = new HashSet<>();
        for (int position = 1; currentJobRunId != null && position < FULL_PATH_MAX_LEVELS; ++position) {
            if (visitedJobRunIds.contains(currentJobRunId)) {
                log.warn("Detected infinite loop while generating full path for job_run_id={}", startingJobRunId);
                break;
            }
            visitedJobRunIds.add(currentJobRunId);

            Optional<CICDJobRun> jobRunOpt = cicdJobRunsDatabaseService.get(company, currentJobRunId);
            if (jobRunOpt.isEmpty()) {
                break;
            }
            CICDJobRun jobRun = jobRunOpt.get();

            log.debug("Generating full path for job_run_id={} - currentJobRunId={}, position={}", startingJobRunId, currentJobRunId, position);

            // find associated job
            CICDJob cicdJob = cicdJobsService.get(company, jobRun.getCicdJobId().toString())
                    .orElseThrow(() -> new NotFoundException("Job run without a valid job: jobRunId=" + startingJobRunId));

            // add segment to output path
            path.add(PathSegment.builder()
                    .id(jobRun.getId().toString())
                    .position(position)
                    .name(cicdJob.getJobName())
                    .type(SegmentType.CICD_JOB)
                    .build());

            Optional<JobRunStage> parentStageOpt = findParentStage(company, currentJobRunId);
            if (parentStageOpt.isPresent()) {
                position++;
                var parentStage = parentStageOpt.get();
                path.add(PathSegment.builder()
                        .id(parentStage.getId().toString())
                        .position(position)
                        .name(parentStage.getName())
                        .type(SegmentType.CICD_STAGE)
                        .build());
                currentJobRunId = parentStage.getCiCdJobRunId() != null ? parentStage.getCiCdJobRunId().toString() : null;
                continue;
            }

            Optional<CICDJobRun> parentJobRun = findParentJobRun(company, jobRun, cicdJob);
            currentJobRunId = parentJobRun
                    .map(CICDJobRun::getId)
                    .map(UUID::toString)
                    .orElse(null);
        }

        if (path.isEmpty()) {
            throw new NotFoundException("No path found for job run id=" + startingJobRunId);
        }

        var output = bottomUp ? path : PathSegment.reverse(path);
        log.info("Generated full path for job_run_id={}: {}", startingJobRunId, output);
        return output;
    }

    private Optional<JobRunStage> findParentStage(String company, String jobRunId) throws SQLException {
        return IterableUtils.getFirst(jobRunStageDatabaseService.list(company, 0, 1, QueryFilter.builder()
                .strictMatch("child_job_runs", List.of(jobRunId))
                .build()).getRecords());
    }

    private Optional<CICDJobRun> findParentJobRun(String company, CICDJobRun jobRun, CICDJob cicdJob) throws SQLException {
        // only considering first trigger - in the vast majority of the time, there will be only one
        Optional<CICDJobTrigger> triggerOpt = SetUtils.emptyIfNull(jobRun.getTriggers()).stream()
                .filter(trigger -> "UpstreamCause".equalsIgnoreCase(trigger.getType()))
                .filter(trigger -> !"unknown".equalsIgnoreCase(trigger.getId()))
                .findFirst();
        if (triggerOpt.isEmpty()) {
            return Optional.empty();
        }
        CICDJobTrigger trigger = triggerOpt.get();

        // find parent job
        String parentJobFullPath = trigger.getId();
        DbListResponse<CICDJob> parentJobResult = cicdJobsService.listByFilter(company, 0, 1, null, null, null,
                List.of(parentJobFullPath), List.of(cicdJob.getCicdInstanceId()));
        Optional<UUID> parentJobId = IterableUtils.getFirst(parentJobResult.getRecords())
                .map(CICDJob::getId);
        if (parentJobId.isEmpty()) {
            log.warn("No parent job with full path={} for cicdInstanceId={}", parentJobFullPath, cicdJob.getCicdInstanceId());
            return Optional.empty();
        }

        // find parent job run
        DbListResponse<CICDJobRun> parentJobRunResult = cicdJobRunsDatabaseService.listByFilter(company, 0, 1, null,
                List.of(parentJobId.get()), List.of(Long.valueOf(trigger.getBuildNumber())));
        if (CollectionUtils.isEmpty(parentJobRunResult.getRecords())) {
            log.warn("No parent job run found for jobId={}, buildNumber={}", parentJobId.get(), trigger.getBuildNumber());
            return Optional.empty();
        }

        return Optional.ofNullable(parentJobRunResult.getRecords().get(0));
    }

    @Override
    public Set<PathSegment> getJobStageFullPath(String company, String stageId, boolean bottomUp) throws SQLException, NotFoundException {
        JobRunStage jobRunStage = jobRunStageDatabaseService.get(company, stageId)
                .orElseThrow(() -> new NotFoundException("Could not find job run stage with id=" + stageId));
        PathSegment currentPathSegment = PathSegment.builder()
                .id(jobRunStage.getId().toString())
                .position(1)
                .name(jobRunStage.getName())
                .type(SegmentType.CICD_STAGE)
                .build();
        if (jobRunStage.getCiCdJobRunId() == null) {
            return Set.of(currentPathSegment);
        }

        Set<PathSegment> parentPath = getJobRunFullPath(company, jobRunStage.getCiCdJobRunId().toString(), false);

        Set<PathSegment> fullPath = PathSegment.concat(parentPath, Set.of(currentPathSegment));

        return bottomUp ? PathSegment.reverse(fullPath) : fullPath;
    }

}