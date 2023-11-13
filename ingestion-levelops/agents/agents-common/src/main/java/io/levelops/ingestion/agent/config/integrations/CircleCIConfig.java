package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.CircleCIController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.circleci.services.CircleCIBuildEnrichmentService;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.levelops.integrations.circleci.client.*;
import io.levelops.integrations.circleci.source.*;

@Configuration
public class CircleCIConfig {

    private static final String CIRCLECI_CONTROLLER = "CircleCIController";
    private static final String CIRCLECI_BUILD_DATA_SOURCE = "CircleCIBuildDataSource";

    @Bean
    public CircleCIClientFactory circleCIClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                                       OkHttpClient okHttpClient) {
        return CircleCIClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .build();
    }

    @Bean
    public CircleCIController circleCIController(IngestionEngine ingestionEngine,
                                                 CircleCIBuildDataSource buildDataSource,
                                                 InventoryService inventoryService,
                                                 ObjectMapper objectMapper, StorageDataSink storageDataSink,
                                                 @Qualifier("onboardingInDays") int onboardingInDays) {
        return ingestionEngine.add(CIRCLECI_CONTROLLER, CircleCIController.builder()
                .buildDataSource(buildDataSource)
                .objectMapper(objectMapper)
                .inventoryService(inventoryService)
                .storageDataSink(storageDataSink)
                .onboardingScanInDays(onboardingInDays)
                .build());
    }

    @Bean
    public CircleCIBuildEnrichmentService circleCIBuildEnrichmentService(
            @Value("${zendesk_fork_threshold:32}") int forkThreshold,
            @Value("${zendesk_thread_count:8}") int threadCount) {
        return new CircleCIBuildEnrichmentService(threadCount, forkThreshold);
    }

    @Bean
    public CircleCIBuildDataSource circleCIBuildDataSource(IngestionEngine ingestionEngine,
                                                           CircleCIClientFactory circleCIClientFactory,
                                                           CircleCIBuildEnrichmentService enrichmentService) {
        return ingestionEngine.add(CIRCLECI_BUILD_DATA_SOURCE, new CircleCIBuildDataSource(circleCIClientFactory,
                enrichmentService));
    }


}
