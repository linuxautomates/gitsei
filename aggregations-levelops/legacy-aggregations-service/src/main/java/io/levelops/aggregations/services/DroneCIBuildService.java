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
import io.levelops.integrations.droneci.models.DroneCIBuild;
import io.levelops.integrations.droneci.models.DroneCIBuildStage;
import io.levelops.integrations.droneci.models.DroneCIBuildStep;
import io.levelops.integrations.droneci.models.DroneCIBuildStepLog;
import io.levelops.integrations.droneci.models.DroneCIEnrichRepoData;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class DroneCIBuildService {

    @Value("${CICD_JOB_RUN_STAGE_LOGS_BUCKET}")
    private String bucketName;
    private Storage storage;
    private final CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private final CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private final CiCdJobRunStageDatabaseService jobRunStageDatabaseService;
    private final CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;

    @Autowired
    public DroneCIBuildService(CiCdJobsDatabaseService ciCdJobsDatabaseService,
                               CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService,
                               CiCdJobRunStageDatabaseService jobRunStageDatabaseService,
                               CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService) {
        this.ciCdJobsDatabaseService = ciCdJobsDatabaseService;
        this.ciCdJobRunsDatabaseService = ciCdJobRunsDatabaseService;
        this.jobRunStageDatabaseService = jobRunStageDatabaseService;
        this.ciCdJobRunStageStepsDatabaseService = ciCdJobRunStageStepsDatabaseService;
    }

    public String insert(String company, UUID instanceId, DroneCIEnrichRepoData repo) {
        if (repo.getName() == null) {
            return null;
        }
        try {
            String ciCdJobId = insertIntoCiCdJob(company, instanceId, repo);
            log.debug("inserted into cicd jobs for job name {} ", repo.getName());
            ListUtils.emptyIfNull(repo.getBuilds()).stream().filter(Objects::nonNull).forEach(build -> insertBuildEnriches(company, build, ciCdJobId));
            return repo.getId().toString();
        } catch (SQLException throwable) {
            log.error("Failed to insert to cicd jobs with job name " + repo.getName(), throwable);
            return null;
        }
    }

    private void insertBuildEnriches(String company, DroneCIBuild build, String ciCdJobId) {
        try {
            CICDJobRun ciCdJobRun = insertIntoCiCdJobRun(company, build, ciCdJobId);
            log.debug("inserted into cicd job runs for build id {} ", build.getId());
            insertStageAndStep(company, build, ciCdJobRun);
        } catch (SQLException throwable) {
            log.error("Failed to insert to cicd jobs with job run number " + build.getNumber(), throwable);
        }
    }

    private String insertIntoCiCdJob(String company, UUID instanceId, DroneCIEnrichRepoData repo) throws SQLException {
        CICDJob cicdJob = CICDJob.builder()
                .cicdInstanceId(instanceId)
                .projectName(repo.getName())
                .jobName(repo.getName())
                .jobFullName(repo.getSlug())
                .jobNormalizedFullName(repo.getName())
                .branchName(repo.getDefaultBranch())
                .scmUrl(repo.getLink())
                .scmUserId(String.valueOf(repo.getNamespace()))
                .build();
        return ciCdJobsDatabaseService.insert(company, cicdJob);
    }

    private CICDJobRun insertIntoCiCdJobRun(String company, DroneCIBuild build, String ciCdJobId) throws SQLException {
        long started = build.getStarted();
        long finished = build.getFinished();
        CICDJobRun cicdJobRun = CICDJobRun.builder()
                .cicdJobId(UUID.fromString(ciCdJobId))
                .jobRunNumber(build.getNumber())
                .status(build.getStatus())
                .startTime(started != 0 ? Instant.ofEpochSecond(started) : null)
                .duration((Long.valueOf(finished - started).intValue()))
                .endTime(finished != 0 ? Instant.ofEpochSecond(finished) : null)
                .cicdUserId(build.getAuthorName())
                .scmCommitIds(List.of(build.getAfter()))
                .params(List.of())
                .build();
        String cicdJobRunId = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
        cicdJobRun = cicdJobRun.toBuilder().id(UUID.fromString(cicdJobRunId)).build();
        log.info("DroneCI - cicdJobRun = {}", cicdJobRun);
        return cicdJobRun;
    }

    private void insertStageAndStep(String company, DroneCIBuild build, CICDJobRun ciCdJobRun) {
        ListUtils.emptyIfNull(build.getStages()).forEach(buildStage -> {
            try {
                String optStageId = insertJobRunStage(company, ciCdJobRun, build, buildStage);
                ListUtils.emptyIfNull(buildStage.getSteps()).forEach(buildStep -> insertJobRunStageStep(company, optStageId, buildStep));
                log.debug("inserted into cicd stages and steps with job run id {} ", ciCdJobRun.getId());
            } catch (SQLException throwable) {
                throw new RuntimeException("Failed to insert job run stage for cicdJobRunId " + ciCdJobRun.getId(), throwable);
            }
        });
    }

    private String insertJobRunStage(String company, CICDJobRun ciCdJobRun, DroneCIBuild build, DroneCIBuildStage buildStage) throws SQLException {
        long started = buildStage.getStarted();
        long stopped = buildStage.getStopped();

        JobRunStage jobRunStage = JobRunStage.builder()
                .ciCdJobRunId(ciCdJobRun.getId())
                .stageId(String.valueOf(buildStage.getNumber()))
                .state(buildStage.getStatus())
                .childJobRuns(Set.of())
                .name(buildStage.getName())
                .description(StringUtils.EMPTY)
                .result(buildStage.getStatus())
                .duration(Long.valueOf(stopped - started).intValue())
                .logs(StringUtils.EMPTY)
                .startTime(Instant.ofEpochSecond(started))
                .url(buildStage.getStageUrl())
                .fullPath(Set.of())
                .build();

        return jobRunStageDatabaseService.insert(company, jobRunStage);
    }

    private void insertJobRunStageStep(String company, String stageId, DroneCIBuildStep buildStep) {
        try {
            List<String> stepLogs = ListUtils.emptyIfNull(buildStep.getStepLogs()).stream().map(DroneCIBuildStepLog::getOut).collect(Collectors.toList());
            Long started = buildStep.getStarted();
            Long stopped = buildStep.getStopped();
            String gcsPath = GCSUtils.uploadLogsToGCS(storage, bucketName, company, stageId, String.valueOf(buildStep.getNumber()), stepLogs);
            JobRunStageStep jobRunStageStep = JobRunStageStep.builder()
                    .cicdJobRunStageId(UUID.fromString(stageId))
                    .stepId(String.valueOf(buildStep.getNumber()))
                    .displayName(buildStep.getName())
                    .displayDescription(StringUtils.EMPTY)
                    .startTime((started != 0) ? Instant.ofEpochSecond(started) : null)
                    .result(buildStep.getStatus())
                    .state(buildStep.getStatus())
                    .duration(Long.valueOf(stopped - started).intValue())
                    .gcsPath(gcsPath)
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
