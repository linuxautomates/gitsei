package io.levelops.aggregations.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CiCdJobRunTest;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunTestDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.integrations.gitlab.models.GitlabJob;
import io.levelops.integrations.gitlab.models.GitlabPipeline;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class GitlabPipelineService {

    public static final String UNKNOWN = "UNKNOWN";
    private final CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private final CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private final CiCdJobRunStageDatabaseService jobRunStageDatabaseService;
    private final CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;
    private final CiCdJobRunTestDatabaseService cicdJobRunTestDatabaseService;
    private final UserIdentityService userIdentityService;


    @Autowired
    public GitlabPipelineService(DataSource dataSource, CiCdJobsDatabaseService ciCdJobsDatabaseService,
                                 CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService,
                                 CiCdJobRunStageDatabaseService jobRunStageDatabaseService,
                                 CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService,
                                 CiCdJobRunTestDatabaseService cicdJobRunTestDatabaseService) {
        this.ciCdJobsDatabaseService = ciCdJobsDatabaseService;
        this.ciCdJobRunsDatabaseService = ciCdJobRunsDatabaseService;
        this.jobRunStageDatabaseService = jobRunStageDatabaseService;
        this.ciCdJobRunStageStepsDatabaseService = ciCdJobRunStageStepsDatabaseService;
        this.cicdJobRunTestDatabaseService = cicdJobRunTestDatabaseService;
        this.userIdentityService = new UserIdentityService(dataSource);
    }

    public String insert(String company, String integrationId, UUID instanceId, GitlabPipeline pipeline) {
        if (pipeline.getPipelineId() == null) {
            return null;
        }
        if (pipeline.getUser() != null &&
                pipeline.getUser().getUsername() != null &&
                pipeline.getUser().getName() != null) {
            try {
                userIdentityService.insert(company, DbScmUser.builder()
                        .integrationId(integrationId)
                        .cloudId(pipeline.getUser().getUsername())
                        .displayName(pipeline.getUser().getName())
                        .originalDisplayName(pipeline.getUser().getName())
                        .build());
            } catch (SQLException e) {
                log.error("Failed to insert into integration users for username:{} , company: {}, integration id:{}", pipeline.getUser().getUsername(), company, integrationId, e);
            }
        }
        try {
            String ciCdJobId = insertIntoCiCdJobs(company, instanceId, pipeline);
            log.debug("inserted into cicd jobs for job name {} ", pipeline.getRef());
            CICDJobRun ciCdJobRun = insertIntoCiCdJobRuns(company, pipeline, ciCdJobId);
            log.debug("inserted into cicd job runs for pipeline id {} ", pipeline.getPipelineId());
            List<String> stepIds = insertStageAndStep(company, pipeline, ciCdJobRun);
            insertIntoJobRunsTest(company, pipeline, ciCdJobRun.getId().toString());
            return stepIds.stream().findFirst().orElse(null);
        } catch (SQLException | UncategorizedSQLException throwables) {
            log.error("Failed to insert to cicd jobs with job name " + pipeline.getRef(), throwables);
            return null;
        }
    }

    private void insertIntoJobRunsTest(String company, GitlabPipeline pipeline, String ciCdJobRunId) {
        if (pipeline.getTestReport() != null) {
            List<CiCdJobRunTest> ciCdJobRunTests = CiCdJobRunTest.fromGitlabTestReport(ciCdJobRunId, pipeline.getTestReport());
            ciCdJobRunTests.forEach(ciCdJobRunTest -> {
                try {
                    cicdJobRunTestDatabaseService.insert(company, ciCdJobRunTest.toBuilder().cicdJobRunId(ciCdJobRunId).build());
                } catch (SQLException throwables) {
                    log.error("Failed to insert to job run test with jobRunId " + ciCdJobRunId +
                            " and test name " + ciCdJobRunTest.getTestName(), throwables);
                }
            });
            log.debug("inserted into cicd job runs tests with job run id {} ", ciCdJobRunId);
        }
    }

    private List<String> insertStageAndStep(String company, GitlabPipeline pipeline, CICDJobRun ciCdJobRun) {
        Map<String, List<GitlabJob>> jobsAcrossStage = CollectionUtils.emptyIfNull(pipeline.getJobs()).stream()
                .collect(Collectors.groupingBy(GitlabJob::getStage, Collectors.toList()));
        return jobsAcrossStage.entrySet().stream()
                .flatMap(es -> {
                    try {
                        Optional<String> optStageId = insertJobRunStage(company, ciCdJobRun, es);
                        if (optStageId.isEmpty()) {
                            return Stream.empty();
                        }
                        List<String> stepId = insertJobRunStageStep(company, es, optStageId.get());
                        log.debug("inserted into cicd stages and steps with job run id {} ", ciCdJobRun.getId());
                        return stepId.stream();
                    } catch (SQLException throwables) {
                        throw new RuntimeException("Failed to insert job run stage for cicdJobRunId " + ciCdJobRun.getId(), throwables);
                    }
                }).collect(Collectors.toList());
    }

    private List<String> insertJobRunStageStep(String company, Map.Entry<String, List<GitlabJob>> es, String finalStageId) {
        return es.getValue().stream()
                .filter(job -> job.getStartedAt() != null)
                .map(job -> {
                    try {
                        JobRunStageStep jobRunStageStep = JobRunStageStep.builder()
                                .cicdJobRunStageId(UUID.fromString(finalStageId))
                                .stepId(job.getId())
                                .displayName(job.getName())
                                .startTime((job.getStartedAt() != null) ? job.getStartedAt().toInstant() : null)
                                .state(job.getStatus().toUpperCase())
                                .result(job.getStatus().toUpperCase())
                                .duration((int) job.getDuration())
                                .build();
                        return ciCdJobRunStageStepsDatabaseService.insert(company, jobRunStageStep);
                    } catch (SQLException throwables) {
                        throw new RuntimeException("Failed to insert job run stage step for stage id " + finalStageId, throwables);
                    }
                }).collect(Collectors.toList());
    }

    private Optional<String> insertJobRunStage(String company, CICDJobRun ciCdJobRun, Map.Entry<String, List<GitlabJob>> es) throws SQLException {

        long duration = 0;
        String stageStatus = calculateStageStatusFromJobStatuses(es);
        Instant instant = es.getValue().stream()
                .filter(job -> job.getStartedAt() != null)
                .map(job -> job.getStartedAt().toInstant())
                .min(Instant::compareTo)
                .orElse(ciCdJobRun.getStartTime());

        Instant endInstant = es.getValue().stream()
                .filter(job -> job.getFinishedAt() != null)
                .map(job -> job.getFinishedAt().toInstant())
                .max(Instant::compareTo)
                .orElse(ciCdJobRun.getEndTime());

        if(!(instant == null || endInstant == null ))
            duration = Duration.between(instant, endInstant).toSeconds();
        else
            duration = 0;

        JobRunStage jobRunStage = JobRunStage.builder()
                .ciCdJobRunId(ciCdJobRun.getId())
                .name(es.getKey())
                .stageId(es.getKey())
                .state(stageStatus)
                .duration(duration < 0 ? 0 : (int) duration)
                .startTime(instant)
                .result(stageStatus)
                .description(StringUtils.EMPTY)
                .logs(StringUtils.EMPTY)
                .url(StringUtils.EMPTY)
                .fullPath(Set.of())
                .childJobRuns(Set.of())
                .build();
        if (instant == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(jobRunStageDatabaseService.insert(company, jobRunStage));
    }

    private String calculateStageStatusFromJobStatuses(Map.Entry<String, List<GitlabJob>> es) {

        /*
        Stage Status is not provided by Gitlab APIs.
        Stage Status calculation logic is implemented based on Job Status and it can be improved
        scenario by scenario for different job and stage configurations,

        Currently considered Stage statuses as
            failed, passed with warnings, canceled and passed.

        Possible Job Statuses are as follows:
        - failed
        - warning
        - pending
        - running
        - manual
        - scheduled
        - canceled
        - success
        - skipped
        - created
        */

        List<String> states = new ArrayList<>();

        es.getValue().forEach(job -> {
            if("failed".equalsIgnoreCase(job.getStatus()) && job.isAllowFailure())
                states.add("passed with warnings");
            else if ("failed".equalsIgnoreCase(job.getStatus()))
                states.add("failed");
            else if("canceled".equalsIgnoreCase(job.getStatus()))
                states.add("canceled");
            else if("success".equalsIgnoreCase(job.getStatus()))
                states.add("passed");
            else
                states.add(job.getStatus());
        });

        return states.contains("canceled") ? "canceled" :
                        states.contains("failed") ?  "failed" :
                        states.contains("passed with warnings") ? "passed with warnings" :
                        !states.contains("passed") ? "undefined in SEI" : "passed";
    }

    private CICDJobRun insertIntoCiCdJobRuns(String company, GitlabPipeline pipeline, String ciCdJobId) throws SQLException {
        CICDJobRun cicdJobRun = CICDJobRun.builder()
                .cicdJobId(UUID.fromString(ciCdJobId))
                .jobRunNumber(Long.valueOf(pipeline.getPipelineId()))
                .status(pipeline.getStatus())
                .startTime(pipeline.getStartedAt() != null ? pipeline.getStartedAt().toInstant() : null)
                .endTime(pipeline.getFinishedAt() != null ? pipeline.getFinishedAt().toInstant() : null)
                .duration(pipeline.getDuration())
                .cicdUserId(pipeline.getUser() != null ? pipeline.getUser().getName() : UNKNOWN)
                .scmCommitIds(List.of(pipeline.getSha()))
                .params(parsePipelineVariables(pipeline))
                .build();
        String cicdJobRunId = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
        cicdJobRun = cicdJobRun.toBuilder().id(UUID.fromString(cicdJobRunId)).build();
        log.info("Gitlab - cicdJobRun = {}", cicdJobRun);
        return cicdJobRun;
    }

    @NotNull
    private List<CICDJobRun.JobRunParam> parsePipelineVariables(GitlabPipeline pipeline) {
        return CollectionUtils.emptyIfNull(pipeline.getVariables()).stream().map(var -> CICDJobRun.JobRunParam.builder()
                .name(var.getKey())
                .value(var.getValue())
                .type(var.getVariableType()).build()).collect(Collectors.toList());
    }

    private String insertIntoCiCdJobs(String company, UUID instanceId, GitlabPipeline pipeline) throws SQLException {
        CICDJob cicdJob = CICDJob.builder()
                .projectName(MoreObjects.firstNonNull(pipeline.getPathWithNamespace(), UNKNOWN))
                .jobName(pipeline.getRef())
                .jobFullName(MoreObjects.firstNonNull(pipeline.getPathWithNamespace(), UNKNOWN) + "/" + pipeline.getRef())
                .jobNormalizedFullName(MoreObjects.firstNonNull(pipeline.getPathWithNamespace(), UNKNOWN) + "/" + pipeline.getRef())
                .branchName(pipeline.getRef())
                .scmUrl(MoreObjects.firstNonNull(pipeline.getHttpUrlToRepo(), UNKNOWN))
                .scmUserId(UNKNOWN)
                .cicdInstanceId(instanceId)
                .build();
        return ciCdJobsDatabaseService.insert(company, cicdJob);
    }

    public Long getOldestJobRunStartTime(String company, String integrationId) {
        int integration = Integer.parseInt(integrationId);
        Long oldestJobRunStartTime = ciCdJobRunsDatabaseService.getOldestJobRunStartTimeInMillis(company,integration);
        return oldestJobRunStartTime != null ? oldestJobRunStartTime : Instant.now().minus(1l, ChronoUnit.DAYS).toEpochMilli();
    }
}
