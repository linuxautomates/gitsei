package io.levelops.commons.databases.services;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.azuredevops.DbAzureDevopsBuild;
import io.levelops.commons.databases.models.database.azuredevops.DbAzureDevopsPipelineRun;
import io.levelops.commons.databases.models.database.azuredevops.DbAzureDevopsProject;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.integrations.azureDevops.models.AzureDevopsPipelineRunStageStep;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Log4j2
@Service
public class AzureDevopsProjectService {

    private String bucketName;
    public static final String PARAMETER_VALUE = "StringParameterValue";
    public static final String UNKNOWN = "UNKNOWN";
    private final CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private final CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private final CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService;
    private final CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;
    private final UserIdentityService userIdentityService;
    private Storage storage;

    @Autowired
    protected AzureDevopsProjectService(final DataSource dataSource,
                                        final CiCdJobsDatabaseService ciCdJobsDatabaseService,
                                        final CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService,
                                        final CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService,
                                        final CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService,
                                        final UserIdentityService userIdentityService) {
        this.ciCdJobsDatabaseService = ciCdJobsDatabaseService;
        this.ciCdJobRunsDatabaseService = ciCdJobRunsDatabaseService;
        this.ciCdJobRunStageDatabaseService = ciCdJobRunStageDatabaseService;
        this.ciCdJobRunStageStepsDatabaseService = ciCdJobRunStageStepsDatabaseService;
        this.userIdentityService = userIdentityService;
    }

    public String insert(String company, UUID instanceId, DbAzureDevopsProject project) throws SQLException {
        List<String> collect = project.getPipelineRuns().stream()
                .map(pipelineRun -> {
                    String jobId;
                    String jobRunId = null;
                    try {
                        jobId = ciCdJobsDatabaseService.insert(company, CICDJob.builder()
                                .projectName(project.getName())
                                .jobName(pipelineRun.getPipelineName())
                                .jobFullName(project.getName() + "/" + pipelineRun.getPipelineName())
                                .jobNormalizedFullName(project.getName() + "/" + pipelineRun.getPipelineName())
                                .scmUserId(UNKNOWN)
                                .cicdInstanceId(instanceId)
                                .build());
                        UUID cicdJobUuid = UUID.fromString(jobId);
                        Optional<CICDJobRun> dbJobRun = IterableUtils.getFirst(ciCdJobRunsDatabaseService.listByFilter(company, 0, 1, null, List.of(cicdJobUuid), List.of((long) pipelineRun.getRunId())).getRecords());
                        CICDJobRun jobRun = CICDJobRun.builder()
                                .cicdJobId(UUID.fromString(jobId))
                                .jobRunNumber((long) pipelineRun.getRunId())
                                .status(pipelineRun.getResult())
                                .startTime(pipelineRun.getCreatedDate())
                                .scmCommitIds(ListUtils.emptyIfNull(pipelineRun.getCommitIds()))
                                .cicdUserId(UNKNOWN)
                                .duration(Long.valueOf(TimeUnit.MILLISECONDS.toSeconds((pipelineRun
                                        .getFinishedDate().toEpochMilli() - pipelineRun.getCreatedDate().toEpochMilli()))).intValue())
                                .endTime(pipelineRun.getFinishedDate())
                                .params(MapUtils.emptyIfNull(pipelineRun.getVariables()).entrySet().stream()
                                        .map(entrySet -> CICDJobRun.JobRunParam.builder()
                                                .name(entrySet.getKey())
                                                .value((entrySet.getValue().getValue() != null) ? entrySet.getValue().getValue() : EMPTY)
                                                .type(PARAMETER_VALUE).build())
                                        .collect(Collectors.toList()))
                                .build();
                        if (dbJobRun.isEmpty()) {
                            jobRunId = ciCdJobRunsDatabaseService.insert(company, jobRun);
                        } else {
                            jobRunId = dbJobRun.get().getId().toString();
                            jobRun = jobRun.toBuilder().id(dbJobRun.get().getId()).build();
                            if(needsUpdate(dbJobRun.get(), jobRun)){
                                ciCdJobRunsDatabaseService.update(company, jobRun);
                            }
                        }
                        insertJobRunStages(company, pipelineRun, jobRunId);
                    } catch (SQLException e) {
                        log.error("Failed to insert job with id {}", pipelineRun.getId(), e);
                    }
                    return jobRunId;
                }).collect(Collectors.toList());
        return collect.stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(EMPTY);
    }

    private boolean needsUpdate(CICDJobRun dbJobRun, CICDJobRun jobRun){
        Instant endTime = jobRun.getEndTime();
        Instant dbEndTime = dbJobRun.getEndTime();
        List<String> dbJobRunParamValues = ListUtils.emptyIfNull(dbJobRun.getParams()).stream().map(CICDJobRun.JobRunParam::getValue).collect(Collectors.toList());
        List<String> jobRunParamValues = ListUtils.emptyIfNull(jobRun.getParams()).stream().map(CICDJobRun.JobRunParam::getValue).collect(Collectors.toList());
        jobRunParamValues.removeAll(dbJobRunParamValues);
        int endTimeIsNull = (dbEndTime == null) ? 1: 0;
        int dbEndTimeIsNull = (endTime == null) ? 1: 0;
        if (endTimeIsNull + dbEndTimeIsNull == 1) {
            return true;
        }
        if(endTime != null && endTime.isAfter(dbEndTime)){
            return true;
        }
        if(CollectionUtils.size(jobRun.getParams()) != CollectionUtils.size(dbJobRun.getParams())){
            return true;
        }
        if(jobRunParamValues.size() != 0){
            return true;
        }
        log.debug("Skipping pipelineRunId={}, already in DB: dbEndTime={} endTime={}", jobRun.getJobRunNumber(), dbEndTime, endTime);
        return false;
    }

    public void insertJobRunStages(String company, DbAzureDevopsPipelineRun pipelineRun, String jobRunId) {
        ListUtils.emptyIfNull(pipelineRun.getStages()).forEach(stage -> {
            Instant started = stage.getStartTime() != null ? stage.getStartTime().toInstant() : null;
            if(started == null){
                return;
            }
            Instant stopped = stage.getFinishTime() != null ? stage.getFinishTime().toInstant() : null;
            stopped = stopped != null ? stopped : started;
            String jobStageId;
            try {
                jobStageId = ciCdJobRunStageDatabaseService.insert(company, JobRunStage.builder()
                        .ciCdJobRunId(UUID.fromString(jobRunId))
                        .childJobRuns(Set.of())
                        .stageId(stage.getId() != null ? stage.getId().toString() : "")
                        .state(stage.getState())
                        .result(stage.getResult())
                        .logs(EMPTY)
                        .url(EMPTY)
                        .startTime(started)
                        .duration(Long.valueOf(TimeUnit.MILLISECONDS.toSeconds((stopped.toEpochMilli() - started.toEpochMilli()))).intValue())
                        .name(stage.getName())
                        .fullPath(Set.of())
                        .build());
                ListUtils.emptyIfNull(stage.getSteps()).forEach(step -> insertJobRunStageStep(company, step, jobStageId));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void insertJobRunStageStep(String company, AzureDevopsPipelineRunStageStep step, String jobStageId) {
        Instant startTime = step.getStartTime() != null ? step.getStartTime().toInstant() : null;
        if(startTime == null){
            return;
        }
        Instant finishTime = step.getFinishTime() != null ? step.getFinishTime().toInstant() : null;
        finishTime = finishTime == null ? startTime : finishTime;
        if (storage == null) {
            log.error("Failed to insert logs as storage of GCP is not set.");
        }
        if (bucketName == null) {
            log.error("Failed to insert logs as bucket name is not set.");
        }
        String gcsPath = storage != null && bucketName != null ? uploadLogsToGCS(storage, bucketName, company, jobStageId, step.getId().toString(), step.getStepLogs()) : null;
        try {
            ciCdJobRunStageStepsDatabaseService.insert(company, JobRunStageStep.builder()
                    .cicdJobRunStageId(UUID.fromString(jobStageId))
                    .displayName(step.getName())
                    .stepId(step.getId() != null ? step.getId().toString() : "")
                    .startTime(startTime)
                    .result(step.getResult())
                    .state(step.getState())
                    .gcsPath(gcsPath)
                    .duration(Long.valueOf(TimeUnit.MINUTES.toSeconds((finishTime.toEpochMilli() - startTime.toEpochMilli()))).intValue())
                    .build());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public int processBuilds(String company, UUID instanceId, DbAzureDevopsBuild build) {
        int updatedRows = 0;
        List<String> automatedTriggers = List.of("Microsoft.VisualStudio.Services.TFS", "GitHub");
        try {
            if (!automatedTriggers.contains(build.getRequestedBy()) && (StringUtils.isNotBlank(build.getRequestedBy()) || StringUtils.isNotBlank(build.getCloudId()))) { // skipping automated triggers and incomplete records
                userIdentityService.insert(company, DbScmUser.builder()
                        .integrationId(build.getIntegrationId())
                        .cloudId(StringUtils.isNotBlank(build.getCloudId()) ? build.getCloudId() : build.getRequestedBy())
                        .displayName(StringUtils.isNotBlank(build.getRequestedBy()) ? build.getRequestedBy() : build.getCloudId())
                        .originalDisplayName(StringUtils.isNotBlank(build.getRequestedBy()) ? build.getRequestedBy() : build.getCloudId())
                        .build());
            }
            DbListResponse<CICDJob> cicdJobDbListResponse = ciCdJobsDatabaseService.listByFilter(company, 0, 1, null, null, List.of(build.getProjectName() + "/" + build.getPipelineName()), null, List.of(instanceId));
            if(CollectionUtils.size(cicdJobDbListResponse.getRecords()) == 1){
                UUID cicdJobId = cicdJobDbListResponse.getRecords().get(0).getId();
                updatedRows = ciCdJobRunsDatabaseService.updateJobCiCdUserId(company, CICDJobRun.builder()
                        .cicdUserId(build.getRequestedByUniqueName())
                        .jobRunNumber(Long.valueOf(build.getBuildId()))
                        .cicdJobId(cicdJobId)
                        .build());
                log.info("Updated rows {} for cicd_user_id={} of ADO Pipelines by job_run_number={} cicd_job_id={}", updatedRows, build.getRequestedByUniqueName(), build.getBuildId(), cicdJobId);
            }
            updatedRows += ciCdJobsDatabaseService.updateJobScmUrl(company, CICDJob.builder()
                    .cicdInstanceId(instanceId)
                    .projectName(build.getProjectName())
                    .jobName(build.getPipelineName())
                    .scmUrl(build.getRepositoryUrl())
                    .build());
            log.debug("Updated {} rows with scm url", updatedRows);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update job with job_name " + build.getPipelineName(), e);
        }
        return updatedRows;
    }
    public static String uploadLogsToGCS(Storage storage, String bucketName, String company, String stageId, String stepId, String stepLogs) {
        String path;
        try {
            if (StringUtils.isNotEmpty(stepLogs)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(stepLogs);
                byte[] bytes = bos.toByteArray();
                log.debug("Uploading step log to gcs starting");
                path = generateJobRunStageStepLogsPath(company, Instant.now(), stageId, stepId);
                uploadDataToGcs(bucketName, path, bytes, storage);
                log.debug("Uploading step log to gcs completed");

                return path;
            } else {
                log.debug("step log is null or unzipFolder is null, cannot upload logs to gcs!");
                return null;
            }
        } catch (IOException e) {
            log.error("Failed to upload step log file to gcs");
            return null;
        }
    }

    private static String generateJobRunStageStepLogsPath(String tenantId, Instant date, String stageId, String stepId) {
        return String.format("%s/tenant-%s/%s/%s/%s.log",
                "cicd-job-run-stage-step-logs",
                tenantId,
                DateTimeFormatter.ofPattern("uuuu/MM/dd", Locale.US).format(date.atZone(ZoneOffset.UTC)),
                stageId,
                stepId);
    }

    public static void uploadDataToGcs(String bucketName, String gcsPath, byte[] content, Storage storage) {
        BlobId blobId = BlobId.of(bucketName, gcsPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("application/json")
                .build();
        log.info("Uploading content to {}:{}", bucketName, gcsPath);
        storage.create(blobInfo, content);
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

}
