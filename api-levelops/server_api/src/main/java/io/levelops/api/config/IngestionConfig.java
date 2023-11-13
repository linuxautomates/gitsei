package io.levelops.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.integrations.slack.services.SlackIngestionService;
import io.levelops.ingestion.integrations.slack.services.SlackUserIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.services.IngestionAgentControlClient;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Log4j2
@Configuration
public class IngestionConfig {

    private static final String DEFAULT_CONTROL_PLANE_URL = "http://ingestion-control-plane-lb";
    private static final String DEFAULT_CONTROL_PLANE_BASE_PATH = DEFAULT_CONTROL_PLANE_URL + "/control-plane/v1";

    @Bean
    public ControlPlaneService controlPlaneService(ObjectMapper objectMapper,
                                                   OkHttpClient client) {
        return ControlPlaneService
                .builder()
                .controlPlaneUrl(DEFAULT_CONTROL_PLANE_URL)
                .objectMapper(objectMapper)
                .okHttpClient(client)
                .build();
    }


    @Bean
    public SlackIngestionService slackIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        return new SlackIngestionService(controlPlaneService, inventoryService);
    }

    @Bean
    public SlackUserIngestionService slackUserIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        return new SlackUserIngestionService(controlPlaneService, inventoryService);
    }

    @Bean
    public IngestionAgentControlClient ingestionAgentControlClient(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        return IngestionAgentControlClient.builder()
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .controlPlaneUrl(DEFAULT_CONTROL_PLANE_BASE_PATH)
                .build();
    }

    @Bean
    public IngestionTriggerSettings ingestionTriggerSettings(@Value("${DEFAULT_INGESTION_TRIGGER_FREQUENCY:60}") int defaultIngestionTriggerFrequency,
                                                             @Value("#{${APP_SPECIFIC_INGESTION_TRIGGER_FREQUENCY:{" +
                                                                     "github:1440," +
                                                                     "snyk:720," +
                                                                     "testrails:1440," +
                                                                     "azure_devops:1440," +
                                                                     "sonarqube:720" +
                                                                     "}}}") Map<String, Integer> appSpecificTriggerFrequency) {
        IngestionTriggerSettings settings = IngestionTriggerSettings.builder()
                .defaultTriggerFrequency(defaultIngestionTriggerFrequency) // minutes
                .appSpecificTriggerFrequency(MapUtils.emptyIfNull(appSpecificTriggerFrequency))
                .build();
        log.info("Ingestion trigger settings: {}", settings);
        return settings;
    }

    @lombok.Value
    @Builder(toBuilder = true)
    public static class IngestionTriggerSettings {
        int defaultTriggerFrequency;
        Map<String, Integer> appSpecificTriggerFrequency;

        public int getTriggerFrequency(String application) {
            return MoreObjects.firstNonNull(MapUtils.emptyIfNull(appSpecificTriggerFrequency).getOrDefault(application, defaultTriggerFrequency), 60);
        }
    }
}
