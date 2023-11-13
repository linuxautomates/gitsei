package io.levelops.aggregations.services;

import com.google.cloud.storage.Storage;
import io.levelops.aggregations.utils.GCSUtils;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.utils.ListUtils;
import io.levelops.integrations.circleci.models.CircleCIBuild;
import io.levelops.integrations.circleci.models.CircleCIBuildStep;
import io.levelops.integrations.circleci.models.CircleCIScmInfo;
import io.levelops.integrations.circleci.models.CircleCIStepAction;
import io.levelops.integrations.circleci.models.CircleCIStepActionLog;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class CircleCIBuildService {

    @Value("${CICD_JOB_RUN_STAGE_LOGS_BUCKET}")
    private String bucketName;

    private Storage storage;

    private final CiCdJobsDatabaseService ciCdJobsDatabaseService;

    private final CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;

    private final CiCdJobRunStageDatabaseService jobRunStageDatabaseService;

    private final CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;

    @Autowired
    public CircleCIBuildService(CiCdJobsDatabaseService ciCdJobsDatabaseService,
                               CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService,
                               CiCdJobRunStageDatabaseService jobRunStageDatabaseService,
                               CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService) {
        this.ciCdJobsDatabaseService = ciCdJobsDatabaseService;
        this.ciCdJobRunsDatabaseService = ciCdJobRunsDatabaseService;
        this.jobRunStageDatabaseService = jobRunStageDatabaseService;
        this.ciCdJobRunStageStepsDatabaseService = ciCdJobRunStageStepsDatabaseService;
    }

    public String insert(String company, UUID instanceId, CircleCIBuild build) {
        if (build.getRepoName() == null) {
            return null;
        }
        try {
            String ciCdJobId = insertIntoCiCdJob(company, instanceId, build);
            log.debug("inserted into cicd jobs for job name {} ", build.getRepoName());
            insertBuildEnriches(company, build, ciCdJobId);
            return build.getBuildUrl();
        } catch (SQLException throwable) {
            log.error("Failed to insert to cicd jobs with job name " + build.getRepoName(), throwable);
            return null;
        }
    }

    private void insertBuildEnriches(String company, CircleCIBuild build, String ciCdJobId) {
        try {
            CICDJobRun ciCdJobRun = insertIntoCiCdJobRun(company, build, ciCdJobId);
            log.debug("inserted into cicd job runs for build number {} ", build.getBuildNumber());
            insertStageAndStep(company, build, ciCdJobRun);
        } catch (SQLException throwable) {
            log.error("Failed to insert to cicd jobs with job run number " + build.getBuildNumber(), throwable);
        }
    }

    private String insertIntoCiCdJob(String company, UUID instanceId, CircleCIBuild build) throws SQLException {
        String repoSlug = build.getUser() != null && build.getRepo() != null ? build.getModifiedSlug() : build.getSlug();
        String jobName = build.getWorkflows().getJobName();
        String jobFullName = repoSlug + "/" + jobName;
        CICDJob cicdJob = CICDJob.builder()
                .cicdInstanceId(instanceId)
                .projectName(repoSlug)
                .jobName(jobName)
                .jobFullName(jobFullName)
                .jobNormalizedFullName(jobFullName)
                .branchName(build.getBranch())
                .scmUserId(build.getUser() != null ? build.getUser() : build.getUsername())
                .build();
        return ciCdJobsDatabaseService.insert(company, cicdJob);
    }

    private CICDJobRun insertIntoCiCdJobRun(String company, CircleCIBuild build, String ciCdJobId) throws SQLException {
        Date started = build.getStartTime();
        Date finished = build.getStopTime();
        CICDJobRun cicdJobRun = CICDJobRun.builder()
                .cicdJobId(UUID.fromString(ciCdJobId))
                .jobRunNumber((long) build.getBuildNumber())
                .status(build.getStatus())
                .startTime(started != null ? started.toInstant() : null)
                .duration(Long.valueOf(build.getBuildTimeMillis()/1000).intValue())
                .endTime(finished !=  null ? finished.toInstant() : null)
                .cicdUserId(build.getUser() != null ? build.getUser() : build.getUsername())
                .scmCommitIds(build.getScmInfoList().stream().map(CircleCIScmInfo::getVcsRevision).collect(Collectors.toList()))
                .params(List.of())
                .build();
        String cicdJobRunId = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
        log.info("Username: {} User: {}",build.getUsername(),build.getUser());
        log.info("Condition: {}", (build.getUser() != null ? build.getUser() : build.getUsername()));
        cicdJobRun = cicdJobRun.toBuilder().id(UUID.fromString(cicdJobRunId)).build();
        log.info("CircleCI - cicdJobRun = {}", cicdJobRun);
        return cicdJobRun;
    }

    private void insertStageAndStep(String company, CircleCIBuild build, CICDJobRun ciCdJobRun) {
        ListUtils.emptyIfNull(build.getSteps()).forEach(step -> {
            try {
                String optStageId = insertJobRunStage(company, ciCdJobRun, build, step);
                ListUtils.emptyIfNull(step.getActions()).forEach(action -> insertJobRunStageStep(company, optStageId, action));
                log.debug("inserted into cicd stages and steps with job run id {} ", ciCdJobRun.getId());
            } catch (SQLException throwable) {
                throw new RuntimeException("Failed to insert job run stage for cicdJobRunId " + ciCdJobRun.getId(), throwable);
            }
        });
    }

    private String insertJobRunStage(String company, CICDJobRun ciCdJobRun, CircleCIBuild build, CircleCIBuildStep step) throws SQLException {
        String stepName = step.getName();
        JobRunStage jobRunStage = JobRunStage.builder()
                .ciCdJobRunId(ciCdJobRun.getId())
                .childJobRuns(Set.of())
                .stageId(StringUtils.left(stepName, 5) + build.getBuildNumber())
                .state(StringUtils.EMPTY)
                .result(StringUtils.EMPTY)
                .logs(StringUtils.EMPTY)
                .url(StringUtils.EMPTY)
                .startTime(Instant.ofEpochMilli(0))
                .duration(0)
                .name(step.getName())
                .fullPath(Set.of())
                .build();

        return jobRunStageDatabaseService.insert(company, jobRunStage);
    }

    private void insertJobRunStageStep(String company, String stageId, CircleCIStepAction action) {
        try {
            List<String> logs = ListUtils.emptyIfNull(action.getActionLogs()).stream().map(CircleCIStepActionLog::getMessage).collect(Collectors.toList());
            Instant started = action.getStartTime().toInstant();
            Instant stopped = action.getEndTime().toInstant();
            String gcsPath = GCSUtils.uploadLogsToGCS(storage, bucketName, company, stageId, String.valueOf(action.getStep()), logs);
            JobRunStageStep jobRunStageStep = JobRunStageStep.builder()
                    .cicdJobRunStageId(UUID.fromString(stageId))
                    .displayName(action.getName())
                    .stepId(action.getStep().toString())
                    .startTime(action.getStartTime().toInstant())
                    .result(action.getStatus())
                    .state(action.getStatus())
                    .gcsPath(gcsPath)
                    .duration(Long.valueOf(stopped.toEpochMilli() - started.toEpochMilli()).intValue())
                    .build();
            ciCdJobRunStageStepsDatabaseService.insert(company, jobRunStageStep);
        } catch (SQLException throwable) {
            throw new RuntimeException("Failed to insert job run stage step for stage id " + stageId, throwable);
        }
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }
}
