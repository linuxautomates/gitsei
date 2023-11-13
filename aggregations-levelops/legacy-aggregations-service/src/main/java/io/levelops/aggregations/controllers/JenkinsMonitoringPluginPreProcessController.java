package io.levelops.aggregations.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.StorageException;
import io.levelops.aggregations.helpers.JenkinsPersistDataHandler;
import io.levelops.aggregations.models.jenkins.JenkinsMonitoringResult;
import io.levelops.aggregations.models.messages.JenkinsPluginResultsPreProcessMessage;
import io.levelops.aggregations.services.PluginResultPreProcessorService;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.PluginResultDTO;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.plugins.clients.PluginResultsClient;
import io.levelops.plugins.models.StoredPluginResultDTO;
import io.levelops.plugins.services.JenkinsPluginResultPreprocessStorageService;
import io.levelops.plugins.services.PluginResultsStorageService;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Service
public class JenkinsMonitoringPluginPreProcessController implements AggregationsController<JenkinsPluginResultsPreProcessMessage> {
    private static final String PLUGIN_RESULT_CONTENT_TYPE = "application/json";
    private final String subscriptionName;
    private final JenkinsPluginResultPreprocessStorageService jenkinsPluginResultPreprocessStorageService;
    private final PluginResultPreProcessorService pluginResultPreProcessorService;
    private final PluginResultsClient pluginResultsClient;
    private final ObjectMapper objectMapper;
    private final JenkinsPersistDataHandler jenkinsPersistDataHandler;
    private final PluginResultsStorageService pluginResultsStorageService;

    @Autowired
    public JenkinsMonitoringPluginPreProcessController(
            @Value("${JENKINS_PRE_PROCESS_SUB}") String subscriptionName,
            JenkinsPluginResultPreprocessStorageService jenkinsPluginResultPreprocessStorageService,
            PluginResultPreProcessorService pluginResultPreProcessorService, PluginResultsClient pluginResultsClient,
            @Qualifier("custom") ObjectMapper mapper, JenkinsPersistDataHandler jenkinsPersistDataHandler, PluginResultsStorageService pluginResultsStorageService) {
        this.subscriptionName = subscriptionName;
        this.jenkinsPluginResultPreprocessStorageService = jenkinsPluginResultPreprocessStorageService;
        this.pluginResultPreProcessorService = pluginResultPreProcessorService;
        this.pluginResultsClient = pluginResultsClient;
        this.objectMapper = mapper;
        this.jenkinsPersistDataHandler = jenkinsPersistDataHandler;
        this.pluginResultsStorageService = pluginResultsStorageService;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.JENKINS;
    }

    @Override
    public Class<JenkinsPluginResultsPreProcessMessage> getMessageType() {
        return JenkinsPluginResultsPreProcessMessage.class;
    }

    @Override
    public String getSubscriptionName() {
        return this.subscriptionName;
    }

    private String submitPluginResult(String company, PluginResultDTO pluginResultDTO) {
        try {
            //submit only the combined plugin results, do not submit binary file again
            PluginResultDTO pluginResultDTOWithoutResults = pluginResultDTO.toBuilder().results(Map.of()).build();
            log.debug("pluginResultDTOWithoutResults = {}", pluginResultDTOWithoutResults);
            UUID resultId = UUID.randomUUID();
            log.debug("resultId = {}", resultId);

            String resultString = objectMapper.writeValueAsString(pluginResultDTO.getResults());
            byte[] content = resultString.getBytes(StandardCharsets.UTF_8);
            log.debug("company {}, resultString length {}, content length {}", company, resultString.length(), content.length);
            String gcsPath = pluginResultsStorageService.uploadResults(company, pluginResultDTOWithoutResults.getTool(), resultId.toString(), PLUGIN_RESULT_CONTENT_TYPE, content);
            log.debug("gcsPath = {}", gcsPath);

            StoredPluginResultDTO storedPluginResultDTO = StoredPluginResultDTO.builder()
                    .pluginResult(pluginResultDTOWithoutResults).resultId(resultId).pluginResultStoragePath(gcsPath)
                    .build();

            String pluginResultId = pluginResultsClient.createStoredPluginResult(company, storedPluginResultDTO);
            log.info("company {} saved plugin result id = {}", company, pluginResultId);
            return pluginResultId;
        } catch (JsonProcessingException | StorageException | PluginResultsClient.PluginResultsClientException e) {
            log.info("Error saving stored plugin results for company {}", company, e);
            return null;
        }
    }

    @Override
    @Async("jenkinsPluginPreProcessTaskExecutor")
    public void doTask(JenkinsPluginResultsPreProcessMessage task) {
        String company = task.getCustomer();
        File jsonFile = null;
        File resultZipFile = null;
        String pluginResultId = null;
        File unzipFolder = null;
        try {
            LoggingUtils.setupThreadLocalContext(task.getMessageId(), task.getCustomer(), "jenkins_pre_process", null);
            log.info("Starting jenkins pre processing for task = {}", task.getMessageId());

            jsonFile = Files.createTempFile("test", ".json").toFile();
            log.debug("company {} Create json file temp file", company);
            jenkinsPluginResultPreprocessStorageService.downloadResult(task.getJsonFilePath(), jsonFile);
            log.debug("company {} Create json file with gcp dta", company);

            resultZipFile = Files.createTempFile("test", ".zip").toFile();
            log.debug("company {} Create result zip file temp file", company);
            jenkinsPluginResultPreprocessStorageService.downloadResult(task.getResultFilePath(), resultZipFile);
            log.debug("company {} Create zip file with gcp dta", company);

            unzipFolder = Files.createTempDirectory("jenkinsPluginUnzip").toFile();
            log.debug("company {} Creates unzip folder {}", company, unzipFolder);

            //Use json file & result binary file to get combined plugin results
            Map<String, Map<Long, File>> jobRunDetailsMap = new HashMap<>();
            PluginResultDTO pluginResultDTO = pluginResultPreProcessorService.preProcess(company, task.getMessageId(), jsonFile, resultZipFile, unzipFolder, jobRunDetailsMap);
            log.debug("company {} created plugin result from zip file", company);
            log.debug("jobRunDetailsMap = {}", jobRunDetailsMap);

            if (pluginResultDTO == null) {
                return;
            }

            if (pluginResultDTO.getResults() != null) {
                //Persist data to db - This should be done before saving plugin results
                JenkinsMonitoringResult jenkinsMonitoringResult = parsePluginResult(pluginResultDTO);
                jenkinsPersistDataHandler.persistJenkinsPluginData(company, jenkinsMonitoringResult, jobRunDetailsMap, task.getMessageId());
            }

            //Submit PluginResultDTO to GCS & Internal Api
            pluginResultId = submitPluginResult(company, pluginResultDTO);

        } catch (BadRequestException | IOException e) {
            log.error("Error pre processing Jenkins Results!", e);
        } finally {
            log.info("Completed work on Agg: {} ", pluginResultId);
            if (unzipFolder != null && unzipFolder.exists() && !FileSystemUtils.deleteRecursively(unzipFolder)) {
                log.warn("Unable to delete unzip directory: {}", unzipFolder);
            }
            if (jsonFile != null) {
                try {
                    Files.deleteIfExists(jsonFile.toPath());
                } catch (IOException e) {
                    log.warn("Unable to delete json file: {}", jsonFile, e);
                }
            }
            if (resultZipFile != null) {
                try {
                    Files.deleteIfExists(resultZipFile.toPath());
                } catch (IOException e) {
                    log.warn("Unable to delete zip file: {}", resultZipFile, e);
                }
            }
            LoggingUtils.clearThreadLocalContext();
        }
    }

    private JenkinsMonitoringResult parsePluginResult(PluginResultDTO pluginResult) throws JsonProcessingException {
        Object jenkinPluginResult = pluginResult.getResults().get("jenkins_config");
        String serialized = objectMapper.writeValueAsString(jenkinPluginResult);
        log.debug("serialized = {}", serialized);
        JenkinsMonitoringResult jenkinsMonitoringResult = objectMapper.readValue(objectMapper.writeValueAsString(jenkinPluginResult), JenkinsMonitoringResult.class);
        return jenkinsMonitoringResult;
    }
}
