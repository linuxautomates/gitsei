package io.levelops.internal_api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.models.messages.JenkinsPluginJobRunCompleteMessage;
import io.levelops.commons.databases.models.database.CiCDPreProcessTask;
import io.levelops.commons.databases.services.CiCdPreProcessTaskService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.plugins.services.JenkinsPluginJobRunCompleteStorageService;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;

@Log4j2
@Service
public class JenkinsPluginJobRunCompleteService {
    private final JenkinsPluginJobRunCompleteStorageService jenkinsPluginJobRunCompleteStorageService;
    private final MessagePubService messagePubService;
    private final CiCdPreProcessTaskService ciCdPreProcessTaskService;

    @Autowired
    public JenkinsPluginJobRunCompleteService(JenkinsPluginJobRunCompleteStorageService jenkinsPluginJobRunCompleteStorageService,
                                              MessagePubService messagePubService, CiCdPreProcessTaskService ciCdPreProcessTaskService) {
        this.jenkinsPluginJobRunCompleteStorageService = jenkinsPluginJobRunCompleteStorageService;
        this.messagePubService = messagePubService;
        this.ciCdPreProcessTaskService = ciCdPreProcessTaskService;
    }

    /**
     * Uploads json + zip file to GCS, then queue a processing task for aggregation.
     * <p>
     * LEV-2751: Removing @Async("jenkinsPluginJobRunCompleteTaskExecutor") as the MultipartFile is out of scope
     * and can be garbage collected anytime by Tomcat. See:
     * https://stackoverflow.com/questions/36565597/spring-async-file-upload-and-processing
     * <p>
     * If we need to make this async again, we need to copy the multipart file to a temp file manually
     * before passing it to this method.
     */
    public void submitJenkinsResultsForPreProcess(String company, final UUID resultId, String json, MultipartFile resultFile) throws JsonProcessingException {
        //Save jsonFile to GCP
        String jsonFilePath = null;
        if (StringUtils.isNotBlank(json)) {
            log.info("Company {} resultId {} Save json to gcp starting", company, resultId);
            jsonFilePath = jenkinsPluginJobRunCompleteStorageService.uploadJsonFile(company, resultId.toString(), "application/json", json.getBytes(StandardCharsets.UTF_8));
            log.info("Company {} resultId {} Save json to gcp completed", company, resultId);
        }

        //Save resultFile to GCP
        String resultFilePath = null;
        if (resultFile != null) {
            try {
                log.info("Company {} resultId {} Save result zip file to gcp starting", company, resultId);
                resultFilePath = jenkinsPluginJobRunCompleteStorageService.uploadResultsZipFile(company, resultId.toString(), resultFile.getContentType(), resultFile.getInputStream());
                log.info("Company {} resultId {} Save result zip file to gcp completed", company, resultId);
            } catch (IOException e) {
                log.error("Company " + company + " resultId " + resultId +
                        " Error uploading result zip file to gcs, cannot pre process.", e);
                // Note: even if this fails, we still want to process the json data
            }
        } else {
            log.info("Company " + company + " resultId " + resultId + " Blank values for result path." +
                    " Zip file not ingested to GCS..");
        }

        if ((StringUtils.isBlank(jsonFilePath)) && (StringUtils.isBlank(resultFilePath))) {
            log.error("Company " + company + " resultId " + resultId + " Blank values for json path and result path");
            return;
        }
        JenkinsPluginJobRunCompleteMessage pubSubMessage = JenkinsPluginJobRunCompleteMessage.builder()
                .messageId(resultId.toString())
                .customer(company)
                .outputBucket(jenkinsPluginJobRunCompleteStorageService.getBucketName())
                .jsonFilePath(jsonFilePath)
                .resultFilePath(resultFilePath)
                .build();
        //Pre-process the data.
        log.info("CICD Pre process task insertion started");
        String metaData = DefaultObjectMapper.get().writeValueAsString(pubSubMessage);
        CiCDPreProcessTask ciCDPreProcessTask = CiCDPreProcessTask.builder().tenantId(company).metaData(metaData).attemptCount(0).status("SCHEDULED").build();
        String taskId = null;
        try {
            taskId = ciCdPreProcessTaskService.insert(company, ciCDPreProcessTask);

        } catch (SQLException ex) {
            log.error("Error while storing the pre-process data for company {}", company, ex);
        }

        //Send PubSub msg
        pubSubMessage = pubSubMessage.toBuilder().taskId(taskId).build();
        log.info("Company {} resultId {} taskId {} Send pub sum message completed", company, resultId, taskId);
        log.debug("pubSubMessage = {}", pubSubMessage);
        try {
            log.debug("Company {} resultId {} Send pub sum message starting", company, resultId);
            messagePubService.publishJenkinsPluginJobRunCompleteMessage(pubSubMessage);
            log.debug("Company {} resultId {} Send pub sum message completed", company, resultId);
        } catch (JsonProcessingException e) {
            log.error("Error sending Jenkins Plugin Pre Process Results pub sub message, cannot pre process results!", e);
        }
    }
}
