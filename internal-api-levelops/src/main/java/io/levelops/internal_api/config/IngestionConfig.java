package io.levelops.internal_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.integrations.slack.services.SlackIngestionService;
import io.levelops.ingestion.integrations.slack.services.SlackInteractiveIngestionService;
import io.levelops.ingestion.integrations.slack.services.SlackUserIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.repomapping.AutoRepoMappingService;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IngestionConfig {

    @Bean
    public ControlPlaneService controlPlaneService(@Value("${CONTROL_PLANE_URL:http://ingestion-control-plane-lb}") String controlPlaneUrl,
                                                   @Qualifier("custom") ObjectMapper objectMapper,
                                                   OkHttpClient client) {
        return ControlPlaneService
                .builder()
                .controlPlaneUrl(controlPlaneUrl)
                .objectMapper(objectMapper)
                .okHttpClient(client)
                .build();
    }

    @Bean
    public SlackIngestionService slackIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        return new SlackIngestionService(controlPlaneService, inventoryService);
    }

    @Bean
    public SlackInteractiveIngestionService slackInteractiveIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        return new SlackInteractiveIngestionService(controlPlaneService, inventoryService);
    }

    @Bean
    public SlackUserIngestionService slackUserIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        return new SlackUserIngestionService(controlPlaneService, inventoryService);
    }

    @Bean
    public AutoRepoMappingService autoRepoMappingService(
            OrgUsersDatabaseService orgUsersDatabaseService,
            ControlPlaneService controlPlaneService,
            InventoryService inventoryService,
            ObjectMapper objectMapper) {
        return new AutoRepoMappingService(orgUsersDatabaseService, controlPlaneService, inventoryService, objectMapper);
    }

}
