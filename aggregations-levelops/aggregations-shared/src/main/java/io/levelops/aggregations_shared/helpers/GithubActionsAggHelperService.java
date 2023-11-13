package io.levelops.aggregations_shared.helpers;

import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.models.filters.CICDInstanceFilter;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.CiCdPushedArtifactsDatabaseService;
import io.levelops.commons.databases.services.CiCdPushedParamsDatabaseService;
import io.levelops.commons.databases.services.CicdJobRunArtifactCorrelationService;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ListUtils;
import io.levelops.integrations.github_actions.models.GithubActionsEnrichedWorkflowRun;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRun;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRunJob;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRunJobStep;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log4j2
public class GithubActionsAggHelperService {

    private CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService;
    private CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;
    private CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private CiCdPushedArtifactsDatabaseService ciCdPushedArtifactsDatabaseService;
    private CiCdPushedParamsDatabaseService ciCdPushedParamsDatabaseService;

    private CicdJobRunArtifactCorrelationService cicdJobRunArtifactCorrelationService;
    @Autowired
    public GithubActionsAggHelperService(CiCdJobsDatabaseService ciCdJobsDatabaseService,
                                         CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService,
                                         CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService,
                                         CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService,
                                         CiCdInstancesDatabaseService ciCdInstancesDatabaseService,
                                         CiCdPushedArtifactsDatabaseService ciCdPushedArtifactsDatabaseService,
                                         CiCdPushedParamsDatabaseService ciCdPushedParamsDatabaseService,
                                         CicdJobRunArtifactCorrelationService cicdJobRunArtifactCorrelationService) {
        this.ciCdJobsDatabaseService = ciCdJobsDatabaseService;
        this.ciCdJobRunsDatabaseService = ciCdJobRunsDatabaseService;
        this.ciCdJobRunStageDatabaseService = ciCdJobRunStageDatabaseService;
        this.ciCdJobRunStageStepsDatabaseService = ciCdJobRunStageStepsDatabaseService;
        this.ciCdInstancesDatabaseService = ciCdInstancesDatabaseService;
        this.ciCdPushedArtifactsDatabaseService = ciCdPushedArtifactsDatabaseService;
        this.ciCdPushedParamsDatabaseService = ciCdPushedParamsDatabaseService;
        this.cicdJobRunArtifactCorrelationService = cicdJobRunArtifactCorrelationService;
    }

    public void processGithubActionsWorkflowRun(GithubActionsEnrichedWorkflowRun enrichedWorkflowRun, String customer,
                                                                     String integrationId, List<String> pushedArtifactIds, List<String> paramIds) throws SQLException {
        GithubActionsWorkflowRun workflowRun = enrichedWorkflowRun.getWorkflowRun();
        UUID cicdInstanceId = getCiCdInstanceId(customer, integrationId);
        try {
            String ciCdJobId = insertIntoCiCdJob(workflowRun, customer, cicdInstanceId);
            log.debug("inserted into cicd jobs for job name {} ", workflowRun.getName());
            CICDJobRun ciCdJobRun = insertIntoCiCdJobRuns(workflowRun, enrichedWorkflowRun, customer, ciCdJobId);
            List<String> artifactIds = insertArtifacts(customer, integrationId, ciCdJobRun.getId().toString(), workflowRun, pushedArtifactIds);
            paramIds.addAll(insertParams(customer, integrationId, ciCdJobRun.getId().toString(), workflowRun));
            try {
                cicdJobRunArtifactCorrelationService.mapCicdJob(customer, ciCdJobRun, artifactIds);
            } catch (Exception e) {
                log.error("Failed to map cicd job run to artifacts: " + e);
            }
        } catch(SQLException e) {
            log.error("Failed to insert to cicd jobs with job name " + workflowRun.getName(), e);
        }
    }
    private String insertIntoCiCdJob(GithubActionsWorkflowRun workflowRun, String customer, UUID cicdInstanceId) throws SQLException {
        String jobFullName = workflowRun.getRepository()!= null ? workflowRun.getRepository().getFullName()
                + "/" + workflowRun.getWorkflowName() : workflowRun.getWorkflowName();
        CICDJob cicdJob = CICDJob.builder()
                .projectName(workflowRun.getRepository() != null ? workflowRun.getRepository().getFullName() : null)
                .jobName(workflowRun.getWorkflowName())
                .jobFullName(jobFullName)
                .jobNormalizedFullName(jobFullName)
                .branchName(workflowRun.getHeadBranch())
                .cicdInstanceId(cicdInstanceId)
                .scmUrl(workflowRun.getRepository().getHtmlUrl())
                .scmUserId(workflowRun.getRepository().getOwner().getLogin())
                .build();
        return ciCdJobsDatabaseService.insert(customer, cicdJob);
    }

    private CICDJobRun insertIntoCiCdJobRuns(GithubActionsWorkflowRun workflowRun, GithubActionsEnrichedWorkflowRun enrichedWorkflowRun,
                                             String customer, String ciCdJobId) throws SQLException {
        Long started = workflowRun.getRunStartedAt() != null ? workflowRun.getRunStartedAt().toInstant().getEpochSecond() : 0;
        Long finished = workflowRun.getUpdatedAt() != null ? workflowRun.getUpdatedAt().toInstant().getEpochSecond() : started;

        String jobRunId;
        Optional<CICDJobRun> dbJobRun = IterableUtils.getFirst(ciCdJobRunsDatabaseService.listByFilter(customer, 0, 1, null, List.of(UUID.fromString(ciCdJobId)), List.of(workflowRun.getRunNumber())).getRecords());
        CICDJobRun cicdJobRun = CICDJobRun.builder()
                .cicdJobId(UUID.fromString(ciCdJobId))
                .jobRunNumber(workflowRun.getRunNumber())
                .status(workflowRun.getConclusion())
                .startTime(workflowRun.getRunStartedAt().toInstant())
                .duration((Long.valueOf(finished - started).intValue()))
                .endTime(workflowRun.getUpdatedAt().toInstant())
                .cicdUserId(workflowRun.getTriggeringActor().getLogin())
                .scmCommitIds(List.of(workflowRun.getHeadSha()))
                .ci(null)
                .cd(null)
                .params(List.of())
                .build();
        if (dbJobRun.isEmpty()) {
            jobRunId = ciCdJobRunsDatabaseService.insert(customer, cicdJobRun);
            cicdJobRun = cicdJobRun.toBuilder().id(UUID.fromString(jobRunId)).build();
            log.debug("inserted into cicd job runs for job run id {} ", workflowRun.getId());
            insertStageAndStep(enrichedWorkflowRun, customer, cicdJobRun);
        } else {
            jobRunId = dbJobRun.get().getId().toString();
            cicdJobRun = cicdJobRun.toBuilder().id(dbJobRun.get().getId()).build();
            if (needsUpdate(dbJobRun.get(), cicdJobRun)) {
                ciCdJobRunsDatabaseService.update(customer, cicdJobRun);
                log.debug("updated into cicd job runs for job run id {} ", workflowRun.getId());
                insertStageAndStep(enrichedWorkflowRun, customer, cicdJobRun);
            }
        }
        log.info("Github Actions - cicdJobRun = {}", cicdJobRun);
        return cicdJobRun;
    }

    private void insertStageAndStep(GithubActionsEnrichedWorkflowRun enrichedWorkflowRun, String customer, CICDJobRun ciCdJobRun) throws SQLException {
        ListUtils.emptyIfNull(enrichedWorkflowRun.getJobs()).forEach(job -> {
            try {
                String stageId = insertJobRunStage(job, customer, ciCdJobRun.getId());
                ListUtils.emptyIfNull(job.getSteps()).forEach(step -> insertJobRunStageStep(step, customer, stageId));
                log.debug("inserted into cicd stages and steps with job run id {} ", ciCdJobRun.getId());
            } catch (SQLException e) {
                throw new RuntimeException("Failed to insert job run stage for cicdJobRunId " + ciCdJobRun.getId(), e);
            }
        });
    }
    private String insertJobRunStage(GithubActionsWorkflowRunJob workflowRunJob, String customer, UUID ciCdJobRunId) throws SQLException {
        Long started = workflowRunJob.getStartedAt() != null ? workflowRunJob.getStartedAt().toInstant().getEpochSecond() : 0;
        Long finished = workflowRunJob.getCompletedAt() != null ? workflowRunJob.getCompletedAt().toInstant().getEpochSecond() : started;

        JobRunStage jobRunStage = JobRunStage.builder()
                .ciCdJobRunId(ciCdJobRunId)
                .stageId(String.valueOf(workflowRunJob.getId()))
                .state(workflowRunJob.getStatus())
                .childJobRuns(Set.of())
                .name(workflowRunJob.getName())
                .result(workflowRunJob.getConclusion())
                .duration(Long.valueOf(finished - started).intValue())
                .logs(StringUtils.EMPTY)
                .startTime(workflowRunJob.getStartedAt().toInstant())
                .url(workflowRunJob.getHtmlUrl())
                .fullPath(Set.of())
                .build();
        return ciCdJobRunStageDatabaseService.insert(customer, jobRunStage);
    }

    private void insertJobRunStageStep(GithubActionsWorkflowRunJobStep workflowRunJobStep, String customer, String stageId) {
        Long started = workflowRunJobStep.getStartedAt() != null ? workflowRunJobStep.getStartedAt().toInstant().getEpochSecond() : 0;
        Long stopped = workflowRunJobStep.getCompletedAt() != null ? workflowRunJobStep.getCompletedAt().toInstant().getEpochSecond() : started;

        JobRunStageStep jobRunStageStep = JobRunStageStep.builder()
                .cicdJobRunStageId(UUID.fromString(stageId))
                .stepId(String.valueOf(workflowRunJobStep.getNumber()))
                .displayName(workflowRunJobStep.getName())
                .startTime((started != 0) ? workflowRunJobStep.getStartedAt().toInstant() : null)
                .result(workflowRunJobStep.getConclusion())
                .state(workflowRunJobStep.getStatus())
                .duration(Long.valueOf(stopped - started).intValue())
                .build();
        try {
            ciCdJobRunStageStepsDatabaseService.insert(customer, jobRunStageStep);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert job run stage step for stage id " + stageId, e);
        }
    }

    private List<String> insertArtifacts(String customer, String integrationId, String ciCdJobRunId, GithubActionsWorkflowRun workflowRun, List<String> pushedArtifactIds) {
        try {
            return ciCdPushedArtifactsDatabaseService.insertCiCdJobRunArtifactsFromPushedArtifacts(customer, integrationId, UUID.fromString(ciCdJobRunId), workflowRun.getWorkflowName(), workflowRun.getRunNumber(), workflowRun.getRepository().getFullName(), pushedArtifactIds);
        } catch (RuntimeException | SQLException e) {
            throw new RuntimeException("Failed to insert artifacts to cicd_job_run_artifacts", e);
        }
    }
    private List<String> insertParams(String customer, String integrationId, String ciCdJobRunId, GithubActionsWorkflowRun workflowRun) {
        try {
            return ciCdPushedParamsDatabaseService.insertCiCdJobRunParamsFromPushedParams(customer, integrationId, UUID.fromString(ciCdJobRunId), workflowRun.getWorkflowName(), workflowRun.getRunNumber(), workflowRun.getRepository().getFullName());
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to insert params to cicd_job_run_params", e);
        }
    }


    private UUID getCiCdInstanceId(String company, String integrationId) throws SQLException {
        DbListResponse<CICDInstance> dbListResponse = ciCdInstancesDatabaseService
                .list(company,
                        CICDInstanceFilter.builder()
                                .integrationIds(List.of(integrationId))
                                .types(List.of(CICD_TYPE.github_actions))
                                .build(), null, null, null);
        if (CollectionUtils.isNotEmpty(dbListResponse.getRecords())) {
            return dbListResponse.getRecords().get(0).getId();
        } else {
            log.warn("CiCd instance response is empty for company " + company + "and integration id" + integrationId);
            throw new RuntimeException("Error listing the cicd instances for integration id " + integrationId + " type "
                    + CICD_TYPE.github_actions);
        }
    }

    public int cleanUpPushedArtifactData(String company, List<UUID> artifactIds) {
        return ciCdPushedArtifactsDatabaseService.cleanUpPushedArtifactsData(company, artifactIds);
    }
    public int cleanUpPushedParamsData(String company, List<UUID> paramIds) {
        return ciCdPushedParamsDatabaseService.cleanUpPushedParamsData(company, paramIds);
    }

    private boolean needsUpdate(CICDJobRun dbJobRun, CICDJobRun jobRun) {
        Instant endTime = jobRun.getEndTime();
        Instant dbEndTime = dbJobRun.getEndTime();
        List<String> dbJobRunParamValues = org.apache.commons.collections4.ListUtils.emptyIfNull(dbJobRun.getParams()).stream().map(CICDJobRun.JobRunParam::getValue).collect(Collectors.toList());
        List<String> jobRunParamValues = org.apache.commons.collections4.ListUtils.emptyIfNull(jobRun.getParams()).stream().map(CICDJobRun.JobRunParam::getValue).collect(Collectors.toList());
        jobRunParamValues.removeAll(dbJobRunParamValues);
        if ((dbEndTime == null) ^ (endTime == null)) {
            return true;
        }
        if (endTime != null && endTime.isAfter(dbEndTime)) {
            return true;
        }
        if (CollectionUtils.size(jobRun.getParams()) != CollectionUtils.size(dbJobRun.getParams())) {
            return true;
        }
        if (jobRunParamValues.size() != 0) {
            return true;
        }
        log.debug("Skipping releaseId={}, already in DB: dbEndTime={} endTime={}", jobRun.getJobRunNumber(), dbEndTime, endTime);
        return false;
    }
}
