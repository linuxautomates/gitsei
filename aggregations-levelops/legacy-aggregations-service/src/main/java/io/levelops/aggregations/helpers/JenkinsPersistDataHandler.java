package io.levelops.aggregations.helpers;

import io.levelops.aggregations.models.jenkins.JenkinsMonitoringResult;
import io.levelops.aggregations.models.jenkins.JobAllConfigChanges;
import io.levelops.aggregations.models.jenkins.JobAllRuns;
import io.levelops.aggregations.models.jenkins.JobConfigChangeDetail;
import io.levelops.aggregations.models.jenkins.JobRunDetails;
import io.levelops.aggregations.models.jenkins.JobRunParam;
import io.levelops.aggregations.services.CiCdJobRunDetailsService;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobConfigChange;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobConfigChangesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log4j2
public class JenkinsPersistDataHandler {
    private static final Integer DELETE_JOBS_WITHOUT_CICD_INSTANCE_BATCH_SIZE = 50;

    private final CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private final CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private final CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private final CiCdJobConfigChangesDatabaseService ciCdJobConfigChangesDatabaseService;
    private final CiCdJobRunDetailsService ciCdJobRunDetailsService;


    @Autowired
    public JenkinsPersistDataHandler(CiCdInstancesDatabaseService ciCdInstancesDatabaseService, CiCdJobsDatabaseService ciCdJobsDatabaseService, CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService, CiCdJobConfigChangesDatabaseService ciCdJobConfigChangesDatabaseService, CiCdJobRunDetailsService ciCdJobRunDetailsService) {
        this.ciCdInstancesDatabaseService = ciCdInstancesDatabaseService;
        this.ciCdJobsDatabaseService = ciCdJobsDatabaseService;
        this.ciCdJobRunsDatabaseService = ciCdJobRunsDatabaseService;
        this.ciCdJobConfigChangesDatabaseService = ciCdJobConfigChangesDatabaseService;
        this.ciCdJobRunDetailsService = ciCdJobRunDetailsService;
    }

    private List<String> persistJenkinsAllJobsAllRuns(String company, List<JobAllRuns> allJobsAllRuns, UUID cicdInstanceId, Map<String, Map<Long, UUID>> jobRunAndIdMap, String messageId) {
        if (CollectionUtils.isEmpty(allJobsAllRuns)) {
            return Collections.emptyList();
        }
        List<String> jobNames = new ArrayList<>();
        for (JobAllRuns currentJobAllRuns : allJobsAllRuns) {
            String jobName = currentJobAllRuns.getJobName();
            String jobFullName = currentJobAllRuns.getJobFullName();
            CICDJob cicdJob = CICDJob.builder()
                    .cicdInstanceId(cicdInstanceId)
                    .jobName(jobName)
                    .jobFullName(jobFullName)
                    .jobNormalizedFullName(currentJobAllRuns.getJobNormalizedFullName())
                    .branchName(currentJobAllRuns.getBranchName())
                    .moduleName(currentJobAllRuns.getModuleName())
                    .scmUrl(CICDJob.sanitizeScmUrl(currentJobAllRuns.getScmUrl()))
                    .scmUserId(currentJobAllRuns.getScmUserId())
                    .build();
            String cicdJobIdString = null;
            try {
                cicdJobIdString = ciCdJobsDatabaseService.insert(company, cicdJob);
                jobNames.add(jobName);
            } catch (SQLException e) {
                log.error("SQL Exception!!", e);
            }
            if (cicdJobIdString == null) {
                continue;
            }
            UUID cicdJobId = UUID.fromString(cicdJobIdString);

            for (JobRunDetails currentJobRun : currentJobAllRuns.getRuns()) {
                CICDJobRun.CICDJobRunBuilder bldr = CICDJobRun.builder()
                        .cicdJobId(cicdJobId)
                        .jobRunNumber(currentJobRun.getNumber())
                        .status(currentJobRun.getStatus())
                        .startTime(Instant.ofEpochSecond(currentJobRun.getStartTime()))
                        .duration(currentJobRun.getDuration().intValue())
                        .cicdUserId(currentJobRun.getUserId())
                        .source(CICDJobRun.Source.ANALYTICS_PERIODIC_PUSH)
                        .referenceId(messageId)
                        .scmCommitIds(currentJobRun.getCommitIds());
                if (currentJobRun.getTriggers() != null) {
                    bldr.triggers(currentJobRun.getTriggers());
                }
                if (CollectionUtils.isNotEmpty(currentJobRun.getParams())) {
                    List<CICDJobRun.JobRunParam> params = new ArrayList<>();
                    for (JobRunParam currentParam : currentJobRun.getParams()) {
                        CICDJobRun.JobRunParam sanitized = CICDJobRun.JobRunParam.builder()
                                .type(currentParam.getType())
                                .name(currentParam.getName())
                                .value(currentParam.getValue())
                                .build();
                        params.add(sanitized);
                    }
                    bldr.params(params);
                }

                CICDJobRun cicdJobRun = bldr.build();
                String jobRunIdString = null;
                try {
                    jobRunIdString = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
                    UUID jobRunId = UUID.fromString(jobRunIdString);
                    if (!jobRunAndIdMap.containsKey(jobFullName)) {
                        jobRunAndIdMap.put(jobFullName, new HashMap<>());
                    }
                    jobRunAndIdMap.get(jobFullName).put(cicdJobRun.getJobRunNumber(), jobRunId);
                } catch (SQLException e) {
                    log.error("SQL Exception!!", e);
                }
            }
        }
        return jobNames;
    }

    private List<String> persistJenkinsAllJobsAllConfigChanges(String company, List<JobAllConfigChanges> allJobsAllConfigChanges, UUID cicdInstanceId) {
        if (CollectionUtils.isEmpty(allJobsAllConfigChanges)) {
            return Collections.emptyList();
        }
        List<String> jobNames = new ArrayList<>();
        for (JobAllConfigChanges currentJobAllConfigChanges : allJobsAllConfigChanges) {
            String jobName = currentJobAllConfigChanges.getJobName();
            String jobFullName = currentJobAllConfigChanges.getJobFullName();
            CICDJob cicdJob = CICDJob.builder()
                    .cicdInstanceId(cicdInstanceId)
                    .jobName(jobName)
                    .jobFullName(jobFullName)
                    .jobNormalizedFullName(currentJobAllConfigChanges.getJobNormalizedFullName())
                    .branchName(currentJobAllConfigChanges.getBranchName())
                    .moduleName(currentJobAllConfigChanges.getModuleName())
                    .scmUrl(null)
                    .scmUserId(null)
                    .build();
            String cicdJobIdString = null;
            try {
                cicdJobIdString = ciCdJobsDatabaseService.insertOnly(company, cicdJob);
                jobNames.add(jobName);
            } catch (SQLException e) {
                log.error("SQL Exception!!", e);
            }
            if (cicdJobIdString == null) {
                continue;
            }
            UUID cicdJobId = UUID.fromString(cicdJobIdString);

            for (JobConfigChangeDetail currentConfigChange : currentJobAllConfigChanges.getConfigChangeDetails()) {
                CICDJobConfigChange configChange = CICDJobConfigChange.builder()
                        .cicdJobId(cicdJobId)
                        .changeTime(Instant.ofEpochSecond(currentConfigChange.getChangeTime()))
                        .changeType(currentConfigChange.getOperation())
                        .cicdUserId(currentConfigChange.getUserId())
                        .build();
                try {
                    ciCdJobConfigChangesDatabaseService.insert(company, configChange);
                } catch (SQLException e) {
                    log.error("SQL Exception!!", e);
                }
            }
        }
        return jobNames;
    }

    private Optional<UUID> persistJenkinsInstance(String company, UUID jenkinsInstanceGuid, String jenkinsInstanceName, String jenkinsInstanceUrl) {
        if (jenkinsInstanceGuid == null) {
            return Optional.empty();
        }
        CICDInstance cicdInstance = CICDInstance.builder()
                .id(jenkinsInstanceGuid)
                .name(jenkinsInstanceName)
                .url(jenkinsInstanceUrl)
                .type(CICD_TYPE.jenkins.toString())
                .build();
        try {
            ciCdInstancesDatabaseService.upsert(company, cicdInstance);
        } catch (Exception e) {
            log.info("persistJenkinsInstance: Unable to upsert cicd instance " +
                    "company {}, Jenkins Instance Id {} ", company, jenkinsInstanceGuid, e);
            return Optional.empty();
        }
        return Optional.of(cicdInstance.getId());
    }

    private void deleteJobsWithoutCiCdInstance(UUID cicdInstanceId, String company, List<String> jobNamesWithConfigChanges, List<String> jobNamesWithJobRuns) {
        if (cicdInstanceId == null) {
            log.debug("For tenant {} current cicdInstanceId is null will not delete jobs without cicdInstanceId", company);
            return;
        }
        List<String> jobNames = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(jobNamesWithConfigChanges)) {
            jobNames.addAll(jobNamesWithConfigChanges);
        }
        if (CollectionUtils.isNotEmpty(jobNamesWithJobRuns)) {
            jobNames.addAll(jobNamesWithJobRuns);
        }
        if (CollectionUtils.isEmpty(jobNames)) {
            log.debug("For tenant {} job names in plugin result is empty, will not delete jobs without cicdInstanceId", company);
            return;
        }
        boolean cleanupRequired = true;
        try {
            Integer jobsWithoutCiCdInstanceCount = ciCdJobsDatabaseService.getCountOfJobsWithoutCiCdInstance(company);
            log.debug("jobsWithoutCiCdInstanceCount = {}", jobsWithoutCiCdInstanceCount);
            cleanupRequired = (jobsWithoutCiCdInstanceCount > 0);
        } catch (SQLException e) {
            log.warn("Error getting count of Jobs Without CiCd Instance, will proceed with deleting jobs without cicd instance id", e);
            cleanupRequired = true;
        }
        if (!cleanupRequired) {
            log.debug("For tenant {} cleanupRequired = false, will not delete jobs without cicdInstanceId", company);
            return;
        }
        List<String> uniqueJobNames = jobNames.stream().distinct().collect(Collectors.toList());
        List<List<String>> partitionedUniqueJobNames = ListUtils.partition(uniqueJobNames, DELETE_JOBS_WITHOUT_CICD_INSTANCE_BATCH_SIZE);
        for (List<String> currentBatch : partitionedUniqueJobNames) {
            try {
                if (!ciCdJobsDatabaseService.deleteByJobsWithoutCiCdInstanceByJobName(company, currentBatch)) {
                    log.warn("Error in current batch of deleting jobs without cicd instance id, skipping this batch, will be deleted in next plugin result push. {}", currentBatch);
                }
            } catch (SQLException e) {
                log.warn("Error in current batch of deleting jobs without cicd instance id, skipping this batch, will be deleted in next plugin result push. {}", currentBatch, e);
            }
        }
        log.info("For tenant {} we deleted jobs without cicd instance id. count = {}", company, uniqueJobNames.size());
    }

    private void persistAllJobRunCompleteDetails(final String company, final JenkinsMonitoringResult jenkinsMonitoringResult, final Map<String, Map<Long, UUID>> jobRunAndIdMap, Map<String, Map<Long, File>> jobRunDetailsMap) {
        if ((jobRunAndIdMap == null) || (jobRunDetailsMap == null)) {
            log.debug("jobRunAndIdMap or jobRunDetailsMap is null, not persisting job run complete details!");
            return;
        }
        for (String jobFullName : jobRunAndIdMap.keySet()) {
            Map<Long, UUID> jobRunNumberAndIdMap = jobRunAndIdMap.get(jobFullName);
            Map<Long, File> jobRunAndCompleteFileMap = jobRunDetailsMap.get(jobFullName);
            if ((jobRunNumberAndIdMap == null) || (jobRunAndCompleteFileMap == null)) {
                log.debug("for jobFullName = {} jobRunNumberAndIdMap or jobRunAndCompleteFileMap is null", jobFullName);
                continue;
            }
            for (Long jobRunNumber : jobRunNumberAndIdMap.keySet()) {
                UUID jobRunId = jobRunNumberAndIdMap.get(jobRunNumber);
                File jobRunCompleteDetailsFile = jobRunAndCompleteFileMap.get(jobRunNumber);
                ciCdJobRunDetailsService.saveCiCdJobDetails(company, jenkinsMonitoringResult, jobRunId, jobRunCompleteDetailsFile);
            }
        }
    }

    /*
    Does the following.
    1) Persists all All Jobs and all their config changes. In this flow Job is only inserted, not updated.
    2) Persists all All Jobs and all their job runs. In this flow Job is both inserted or updated.
    3) For all jobs names with cicd instance id, we delete from db same job names which do NOT have cicd instance id (data from before multiple jenkins instance support).
     */
    public void persistJenkinsPluginData(String company, JenkinsMonitoringResult jenkinsMonitoringResult, Map<String, Map<Long, File>> jobRunDetailsMap, String messageId) {
        Optional<UUID> optionalCiCdInstanceId = persistJenkinsInstance(company, jenkinsMonitoringResult.getJenkinsInstanceGuid(), jenkinsMonitoringResult.getJenkinsInstanceName(), jenkinsMonitoringResult.getJenkinsInstanceUrl());
        if (optionalCiCdInstanceId.isEmpty()) return;
        UUID cicdInstanceId = optionalCiCdInstanceId.get();
        if (!validateCiCdInstance(company, cicdInstanceId)) return;

        //Persist all Job Config changes
        List<String> jobNamesWithConfigChanges = persistJenkinsAllJobsAllConfigChanges(company, jenkinsMonitoringResult.getConfigChanges(), cicdInstanceId);

        //Persist all Job Runs
        Map<String, Map<Long, UUID>> jobRunAndIdMap = new HashMap<>();
        List<String> jobNamesWithJobRuns = persistJenkinsAllJobsAllRuns(company, jenkinsMonitoringResult.getJobRuns(), cicdInstanceId, jobRunAndIdMap, messageId);

        //Persist all Job Run Complete Details
        //Deprecating Job Complete Data (stage, steps & logs) in periodic push flow
        //persistAllJobRunCompleteDetails(company, jenkinsMonitoringResult, jobRunAndIdMap, jobRunDetailsMap);

        deleteJobsWithoutCiCdInstance(cicdInstanceId, company, jobNamesWithConfigChanges, jobNamesWithJobRuns);
    }

    private Boolean validateCiCdInstance(String company, UUID cicdInstanceId) {
        try {
            Optional<CICDInstance> cicdInstance = ciCdInstancesDatabaseService.get(company, cicdInstanceId.toString());
            if (cicdInstance.isEmpty() || Objects.isNull(cicdInstance.get().getIntegrationId())) {
                log.info("persistJenkinsPluginData: cicd instance id {} " +
                        "company {} is not assigned to any integration. " +
                        "Hence dropping the data. ", cicdInstanceId, company);
                return false;
            }
        } catch (Exception e) {
            log.info("persistJenkinsPluginData: Unable to fetch cicd instance " +
                    "company {}, cicd instance id {}", company, cicdInstanceId, e);
            return false;
        }
        return true;
    }

}
