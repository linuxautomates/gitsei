package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.DroneCIController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.levelops.integrations.droneci.client.DroneCIClientFactory;
import io.levelops.integrations.droneci.source.DroneCIEnrichRepoDataSource;

@Configuration
public class DroneCIConfig {

    private static final String DRONECI_CONTROLLER = "DroneCIController";
    private static final String DRONECI_ENRICH_REPO_DATA_SOURCE = "DroneCIEnrichRepoDataSource";

    @Bean
    public DroneCIClientFactory droneCIClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                                     OkHttpClient okHttpClient,
                                                     @Value("${droneci_response_page_size:100}") int pageSize) {
        return DroneCIClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .pageSize(pageSize)
                .build();
    }

    @Bean
    public DroneCIController droneCIController(IngestionEngine ingestionEngine, DroneCIEnrichRepoDataSource dataSource,
                                               ObjectMapper objectMapper, StorageDataSink storageDataSink,
                                               InventoryService inventoryService,
                                               @Qualifier("onboardingInDays") int onboardingInDays) {
        return ingestionEngine.add(DRONECI_CONTROLLER, DroneCIController.builder()
                .repoDataSource(dataSource)
                .objectMapper(objectMapper)
                .inventoryService(inventoryService)
                .storageDataSink(storageDataSink)
                .onboardingScanInDays(onboardingInDays)
                .build());
    }

    @Bean
    public DroneCIEnrichRepoDataSource droneCIEnrichRepoDataSource(IngestionEngine ingestionEngine,
                                                                   DroneCIClientFactory droneCIClientFactory) {
        return ingestionEngine.add(DRONECI_ENRICH_REPO_DATA_SOURCE, new DroneCIEnrichRepoDataSource(droneCIClientFactory));
    }

}
