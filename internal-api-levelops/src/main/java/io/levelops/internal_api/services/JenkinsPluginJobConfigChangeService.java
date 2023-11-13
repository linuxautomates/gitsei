package io.levelops.internal_api.services;

import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobConfigChange;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobConfigChangesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.internal_api.models.JobConfigChangeRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class JenkinsPluginJobConfigChangeService {

    private static final String CICD_JENKINS_TYPE = "jenkins";

    private final CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private final CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private final CiCdJobConfigChangesDatabaseService ciCdJobConfigChangesDatabaseService;

    @Autowired
    public JenkinsPluginJobConfigChangeService(CiCdInstancesDatabaseService ciCdInstancesDatabaseService, CiCdJobsDatabaseService ciCdJobsDatabaseService, CiCdJobConfigChangesDatabaseService ciCdJobConfigChangesDatabaseService) {
        this.ciCdInstancesDatabaseService = ciCdInstancesDatabaseService;
        this.ciCdJobsDatabaseService = ciCdJobsDatabaseService;
        this.ciCdJobConfigChangesDatabaseService = ciCdJobConfigChangesDatabaseService;
    }

    private Optional<UUID> persistJenkinsInstance(String company, UUID jenkinsInstanceGuid, String jenkinsInstanceName, String jenkinsInstanceUrl) {
        if (jenkinsInstanceGuid == null) {
            return Optional.empty();
        }
        CICDInstance cicdInstance = CICDInstance.builder()
                .id(jenkinsInstanceGuid).name(jenkinsInstanceName).url(jenkinsInstanceUrl).type(CICD_JENKINS_TYPE).build();
        try {
            String cicdInstanceIdString = ciCdInstancesDatabaseService.insert(company, cicdInstance);
            return Optional.ofNullable(UUID.fromString(cicdInstanceIdString));
        } catch (SQLException e) {
            log.warn("Failed to save cicd instance to db! {}", cicdInstance, e);
            return Optional.empty();
        }
    }

    @Async("jenkinsPluginJobConfigChangeTaskExecutor")
    public void processJobConfigChange(String company, final JobConfigChangeRequest request) {
        //Persist CICD Instance
        Optional<UUID> optionalCiCdInstanceId = persistJenkinsInstance(company, UUID.fromString(request.getJenkinsInstanceGuid()), request.getJenkinsInstanceName(), request.getJenkinsInstanceUrl());
        UUID cicdInstanceId = optionalCiCdInstanceId.orElse(null);
        log.info("cicdInstanceId = {}", cicdInstanceId);

        //Persist CICD Job
        CICDJob cicdJob = CICDJob.builder()
                .cicdInstanceId(cicdInstanceId)
                .jobName(request.getJobName())
                .jobFullName(request.getJobFullName())
                .jobNormalizedFullName(request.getJobNormalizedFullName())
                .branchName(request.getBranchName())
                .moduleName(request.getModuleName())
                .scmUrl(request.getRepoUrl())
                .scmUserId(request.getScmUserId())
                .build();
        String cicdJobIdString = null;
        try {
            cicdJobIdString = ciCdJobsDatabaseService.insertOnly(company, cicdJob);
        } catch (SQLException e) {
            log.error("SQL Exception!!", e);
        }
        if (cicdJobIdString == null) {
            log.info("Cannot process job config change failed to persist job! company {}, request {}", company, request);
            return;
        }
        UUID cicdJobId = UUID.fromString(cicdJobIdString);
        log.info("cicdJobId = {}", cicdJobId);

        //Persist Job Config changes
        String cicdJobConfigChangeId = null;
        CICDJobConfigChange configChange = CICDJobConfigChange.builder()
                .cicdJobId(cicdJobId)
                .changeTime(Instant.ofEpochMilli(request.getChangeTime()))
                .changeType(request.getChangeType())
                .cicdUserId(request.getUserId())
                .build();
        try {
            cicdJobConfigChangeId = ciCdJobConfigChangesDatabaseService.insert(company, configChange);
        } catch (SQLException e) {
            log.error("SQL Exception!!", e);
        }
        log.info("cicdJobConfigChangeId = {}", cicdJobConfigChangeId);
    }
}
