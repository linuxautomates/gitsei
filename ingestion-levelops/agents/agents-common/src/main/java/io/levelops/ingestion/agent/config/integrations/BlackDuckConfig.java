package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.agent.ingestion.BlackDuckController;
import io.levelops.agent.ingestion.BlackDuckIterativeScanController;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.blackduck.BlackDuckClientFactory;
import io.levelops.integrations.blackduck.models.BlackDuckIterativeScanQuery;
import io.levelops.sources.BlackDuckDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlackDuckConfig {

    @Value("${blackDuck.onboarding_in_days:30}")
    private Integer blackDuckOnBoardingInDays;

    @Bean
    public BlackDuckClientFactory blackDuckClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                                               OkHttpClient okHttpClient,
                                                               @Value("${blackduck_response_page_size:200}") int pageSize,
                                                               @Qualifier("allowUnsafeSSLBlackDuck") Boolean allowUnsafeSSL) {
        return BlackDuckClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .allowUnsafeSSL(allowUnsafeSSL)
                .pageSize(pageSize)
                .build();
    }

    @Bean("blackDuckProjectController")
    public IntegrationController<BlackDuckIterativeScanQuery> blackDuckProjectsController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            BlackDuckClientFactory blackDuckClientFactory) {

        BlackDuckDataSource repositoryDataSource = ingestionEngine.add("BlackDuckProjectDataSource",
                new BlackDuckDataSource(blackDuckClientFactory));

        return ingestionEngine.add("BlackDuckProjectController", BlackDuckController.projectController()
                .objectMapper(objectMapper)
                .dataSource(repositoryDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean
    public BlackDuckIterativeScanController blackDuckIterativeScanController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            @Qualifier("blackDuckProjectController") IntegrationController<BlackDuckIterativeScanQuery> blackDuckProjectController) {

        return ingestionEngine.add("BlackDuckIterativeScanController", BlackDuckIterativeScanController.builder()
                .objectMapper(objectMapper)
                .projectController(blackDuckProjectController)
                .onBoardingInDays(blackDuckOnBoardingInDays)
                .build());
    }
}
