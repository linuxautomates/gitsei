package io.levelops.commons.databases.services;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.azuredevops.DbAzureDevopsProject;
import io.levelops.commons.databases.models.database.azuredevops.DbAzureDevopsRelease;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.integrations.azureDevops.models.AzureDevopsReleaseStep;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Log4j2
@Service
public class AzureDevopsReleaseService {
    private String bucketName;
    public static final String PARAMETER_VALUE = "StringParameterValue";
    private final CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private final CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private final CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService;
    private final CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;
    private final UserIdentityService userIdentityService;
    private Storage storage;

    @Autowired
    protected AzureDevopsReleaseService(final CiCdJobsDatabaseService ciCdJobsDatabaseService,
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

    public void insert(String company, UUID instanceId, DbAzureDevopsProject project) throws SQLException {
        for (DbAzureDevopsRelease release : project.getReleases()) {
            String projectName = project.getName();
            String organization = project.getOrganization();
            String jobName = release.getDefinition().getName();
            String fullName = organization + "/" + projectName + "/" + jobName;

            String jobId = ciCdJobsDatabaseService.insert(company, CICDJob.builder()
                    .projectName(project.getName())
                    .jobName(jobName)
                    .jobFullName(fullName)
                    .jobNormalizedFullName(fullName)
                    .cicdInstanceId(instanceId)
                    .build());
            insertJobRun(company, release, jobId);
        }
    }

    public String insertJobRun(String company, DbAzureDevopsRelease release, String jobId) throws SQLException {
        String releaseName = release.getName();
        String[] splitReleaseName = releaseName.split("-");
        if (splitReleaseName.length != 2) {
            log.warn("Skipping the release as unable to get the job run number from the release id={}", release.getReleaseId());
            return null;
        }
        Instant startTime = release.getStartTime();
        Instant finishTime = release.getFinishTime() != null ? release.getFinishTime() : startTime;
        Long jobRunNumber = Long.parseLong(splitReleaseName[1]);
        String jobRunId;
        Optional<CICDJobRun> dbJobRun = IterableUtils.getFirst(ciCdJobRunsDatabaseService.listByFilter(company, 0, 1, null, List.of(UUID.fromString(jobId)), List.of(jobRunNumber)).getRecords());
        CICDJobRun jobRun = CICDJobRun.builder()
                .cicdJobId(UUID.fromString(jobId))
                .jobRunNumber(jobRunNumber)
                .status(release.getStatus())
                .startTime(startTime)
                .scmCommitIds(List.of())
                .cicdUserId(release.getCreatedBy())
                .duration(Long.valueOf(TimeUnit.MILLISECONDS.toSeconds((finishTime.toEpochMilli() - startTime.toEpochMilli()))).intValue())
                .endTime(finishTime)
                .cd(true)
                .params(MapUtils.emptyIfNull(release.getVariables()).entrySet().stream()
                        .map(entrySet -> CICDJobRun.JobRunParam.builder()
                                .name(entrySet.getKey())
                                .value((entrySet.getValue().getValue() != null) ? entrySet.getValue().getValue() : EMPTY)
                                .type(PARAMETER_VALUE).build())
                        .collect(Collectors.toList()))
                .build();
        if (dbJobRun.isEmpty()) {
            jobRunId = ciCdJobRunsDatabaseService.insert(company, jobRun);
            insertJobRunStages(company, release, jobRunId);
        } else {
            jobRunId = dbJobRun.get().getId().toString();
            jobRun = jobRun.toBuilder().id(dbJobRun.get().getId()).build();
            if (needsUpdate(dbJobRun.get(), jobRun)) {
                ciCdJobRunsDatabaseService.update(company, jobRun);
                insertJobRunStages(company, release, jobRunId);
            }
        }
        return jobRunId;
    }

    public void insertJobRunStages(String company, DbAzureDevopsRelease release, String jobRunId) {
        ListUtils.emptyIfNull(release.getStages()).forEach(stage -> {
            Instant started = stage.getCreatedOn();
            if (started == null) {
                return;
            }
            String jobStageId;
            try {
                jobStageId = ciCdJobRunStageDatabaseService.insert(company, JobRunStage.builder()
                        .ciCdJobRunId(UUID.fromString(jobRunId))
                        .childJobRuns(Set.of())
                        .stageId(stage.getStageId() != null ? stage.getStageId().toString() : "")
                        .state(stage.getStatus())
                        .result(stage.getStatus())
                        .logs(EMPTY)
                        .url(EMPTY)
                        .startTime(started)
                        .duration(stage.getTimeToDeploy() != null ? (int) (stage.getTimeToDeploy() * 60) : 0)
                        .name(stage.getName())
                        .fullPath(Set.of())
                        .build());
                ListUtils.emptyIfNull(stage.getSteps()).forEach(step -> insertJobRunStageStep(company, step, jobStageId));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void insertJobRunStageStep(String company, AzureDevopsReleaseStep step, String jobStageId) {
        Instant startTime = DateUtils.parseDateTime(step.getStartTime());
        if (startTime == null) {
            return;
        }
        Instant finishTime = DateUtils.parseDateTime(step.getFinishTime());
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
                    .result(step.getStatus())
                    .state(step.getStatus())
                    .gcsPath(gcsPath)
                    .duration(Long.valueOf(TimeUnit.MINUTES.toSeconds((finishTime.toEpochMilli() - startTime.toEpochMilli()))).intValue())
                    .build());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean needsUpdate(CICDJobRun dbJobRun, CICDJobRun jobRun) {
        Instant endTime = jobRun.getEndTime();
        Instant dbEndTime = dbJobRun.getEndTime();
        List<String> dbJobRunParamValues = ListUtils.emptyIfNull(dbJobRun.getParams()).stream().map(CICDJobRun.JobRunParam::getValue).collect(Collectors.toList());
        List<String> jobRunParamValues = ListUtils.emptyIfNull(jobRun.getParams()).stream().map(CICDJobRun.JobRunParam::getValue).collect(Collectors.toList());
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
