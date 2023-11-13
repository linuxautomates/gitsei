package io.levelops.internal_api.services.plugins.preprocess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.models.messages.JenkinsPluginResultsPreProcessMessage;
import io.levelops.commons.databases.models.database.PluginResultDTO;
import io.levelops.internal_api.services.MessagePubService;
import io.levelops.plugins.services.JenkinsPluginResultPreprocessStorageService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Log4j2
@Service
public class JenkinsPluginResultPreProcessService {
    private final JenkinsPluginResultPreprocessStorageService jenkinsPluginResultPreprocessStorageService;
    private final MessagePubService messagePubService;
    private final ObjectMapper objectMapper;

    @Autowired
    public JenkinsPluginResultPreProcessService(JenkinsPluginResultPreprocessStorageService jenkinsPluginResultPreprocessStorageService,
                                                MessagePubService messagePubService, ObjectMapper objectMapper) {
        this.jenkinsPluginResultPreprocessStorageService = jenkinsPluginResultPreprocessStorageService;
        this.messagePubService = messagePubService;
        this.objectMapper = objectMapper;
    }

    @Async("jenkinsPluginResultsPreProcessTaskExecutor")
    public void submitJenkinsResultsForPreProcess(String company, final UUID resultId, MultipartFile jsonFile, MultipartFile resultFile, PluginResultDTO pluginResultDTO) {
        //Save jsonFile to GCP
        String jsonFilePath;
        try {
            log.debug("Company {} resultId {} Save json file to gcp starting", company, resultId);
            jsonFilePath = jenkinsPluginResultPreprocessStorageService.uploadJsonFile(company, pluginResultDTO.getTool(), resultId.toString(), "application/json", jsonFile.getInputStream());
            log.debug("Company {} resultId {} Save json file to gcp completed", company, resultId);
        } catch (IOException e) {
            log.error("Error uploading json file to gcs, cannot pre process results!", e);
            return;
        }

        //Save resultFile to GCP
        String resultFilePath;
        try {
            log.debug("Company {} resultId {} Save result zip file to gcp starting", company, resultId);
            resultFilePath = jenkinsPluginResultPreprocessStorageService.uploadResultsZipFile(company, pluginResultDTO.getTool(), resultId.toString(), resultFile.getContentType(), resultFile.getInputStream());
            log.debug("Company {} resultId {} Save result zip file to gcp completed", company, resultId);
        } catch (IOException e) {
            log.error("Error uploading result zip file to gcs, cannot pre process results!", e);
            return;
        }

        //Send PubSub msg
        JenkinsPluginResultsPreProcessMessage pubSubMessage = JenkinsPluginResultsPreProcessMessage.builder()
                .messageId(resultId.toString())
                .customer(company)
                .outputBucket(jenkinsPluginResultPreprocessStorageService.getBucketName())
                .jsonFilePath(jsonFilePath)
                .resultFilePath(resultFilePath).build();
        log.debug("pubSubMessage = {}", pubSubMessage);
        try {
            log.debug("Company {} resultId {} Send pub sum message starting", company, resultId);
            messagePubService.publishJenkinsPreProcessMessage(pubSubMessage);
            log.debug("Company {} resultId {} Send pub sum message completed", company, resultId);
        } catch (JsonProcessingException e) {
            log.error("Error sending Jenkins Plugin Pre Process Results pub sub message, cannot pre process results!", e);
        }
    }
}