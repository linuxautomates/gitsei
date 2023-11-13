package io.levelops.aggregations.services;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import io.levelops.aggregations.models.jenkins.JenkinsMonitoringResult;
import io.levelops.aggregations.models.jenkins.JobRun;
import io.levelops.aggregations.models.jenkins.Node;
import io.levelops.commons.databases.models.database.CICDJobRunDetails;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.services.CiCdJobRunDetailsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class CiCdJobRunDetailsService {
    private final CiCdJobRunDetailsDatabaseService ciCdJobRunDetailsDatabaseService;
    private final CiCdJobRunStageDatabaseService stagesService;
    private final String bucketName;
    private final Storage storage;

    @Autowired
    public CiCdJobRunDetailsService(CiCdJobRunDetailsDatabaseService ciCdJobRunDetailsDatabaseService,
                                    @Value("${CICD_JOB_RUNS_DETAILS_BUCKET}") String bucketName,
                                    Storage storage, final CiCdJobRunStageDatabaseService stagesService) {
        this.ciCdJobRunDetailsDatabaseService = ciCdJobRunDetailsDatabaseService;
        this.bucketName = bucketName;
        this.storage = storage;
        this.stagesService = stagesService;
    }

    public void saveCiCdJobDetails(final String company, final JenkinsMonitoringResult jenkinsMonitoringResult, final UUID cicdJobRunId, final File cicdJobRunDetailsFile) {
        if((cicdJobRunId == null) || (cicdJobRunDetailsFile == null)) {
            log.debug("cicdJobRunId or cicdJobRunDetailsFile is null!");
            return;
        }
        DbListResponse<CICDJobRunDetails> dbListResponse = null;
        try {
            dbListResponse = ciCdJobRunDetailsDatabaseService.listByFilter(company, 0, 10, null, List.of(cicdJobRunId));
        } catch (SQLException e) {
            log.warn("Error fetching job run details!", e);
            return;
        }
        if((dbListResponse != null) && (CollectionUtils.isNotEmpty(dbListResponse.getRecords()))) {
            log.debug("for company = {}, cicdJobRunId = {}, cicd job run details already exist in db!", company, cicdJobRunId);
            return;
        }
        if(!cicdJobRunDetailsFile.exists()){
            log.debug("for company = {}, cicdJobRunDetailsFile {} does not exist!", company, cicdJobRunDetailsFile);
            return;
        }
        byte[] content = new byte[0];
        try {
            content = Files.readAllBytes(cicdJobRunDetailsFile.toPath());
            log.info("Content length: {}", content.length);
        } catch (IOException e) {
            log.warn("Error reading data from job run details file!!", e);
            return;
        }

        String path = generatePath(company, Instant.now(), cicdJobRunId.toString());
        uploadDataToGcs(bucketName, path, content);

        CICDJobRunDetails cicdJobRunDetailsObj = CICDJobRunDetails.builder()
                .cicdJobRunId(cicdJobRunId).gcsPath(path).build();
        try {
            String id = ciCdJobRunDetailsDatabaseService.insert(company, cicdJobRunDetailsObj);
            log.debug("for company = {}, cicd job run details id = {}", company, id);
        } catch (SQLException e) {
            log.warn("Error inserting cicd job run details to db", e);
        }

        // insert stages
        List<Node> stages = List.of();
        try {
            var jobRun = DefaultObjectMapper.get().readValue(content, JobRun.class);
            
            stages = jobRun.getStages();
        } catch (IOException | NullPointerException e1) {
            log.error("unable to parse the json doc to build a jobRun object. content length: {}", content.length, e1);
        }

        log.info("Stages in the job run '{}': {}", cicdJobRunDetailsObj.getCicdJobRunId(), stages.size());

        CollectionUtils.emptyIfNull(stages).forEach(node -> {
            try {
                stagesService.insert(company, JobRunStage.builder()
                    .stageId(node.getId())
                    .name(node.getDisplayName())
                    .description(node.getDisplayDescription())
                    .result(node.getResult())
                    .state(node.getState())
                    .duration((int) TimeUnit.MILLISECONDS.toSeconds(node.getDurationInMillis()))
                    .startTime(DateUtils.parseDateTime(node.getStartTime()))
                    .url(jenkinsMonitoringResult.getJenkinsInstanceUrl() + node.getLinks().getSelf().getHref())
                    // .logs(node.getLog()) // TODO: add log path
                    // .childJobRuns(null) // TODO: add child_job_runs db ids
                    .build()
                );
                // send message to queue for log analysis 
            } catch (SQLException e) {
                log.error("unable to insert stage in the db for job run '{}': stage={}", cicdJobRunId, node, e);
            }
        });
    }

    private Blob uploadDataToGcs(String bucketName, String gcsPath, byte[] content) {
        BlobId blobId = BlobId.of(bucketName, gcsPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("application/json")
                .build();
        log.info("Uploading content to {}:{}", bucketName, gcsPath);
        return storage.create(blobInfo, content);
    }

    private static String generatePath(String tenantId, Instant date, String cicdJobRunId) {
        return String.format("cicd-job-run-details/tenant-%s/%s/%s",
                tenantId,
                DateTimeFormatter.ofPattern("uuuu/MM/dd", Locale.US).format(date.atZone(ZoneOffset.UTC)),
                cicdJobRunId);
    }
}
