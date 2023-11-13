package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.HelixSwarmReviewController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClientFactory;
import io.levelops.integrations.helix_swarm.services.HelixSwarmEnrichmentService;
import io.levelops.integrations.helix_swarm.sources.HelixSwarmReviewDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HelixSwarmConfig {

    private static final String HELIX_SWARM_REVIEWS_DATA_SOURCE = "HelixSwarmReviewDataSource";
    private static final String HELIX_SWARM_REVIEWS_CONTROLLER = "HelixSwarmReviewController";

    @Bean
    public HelixSwarmClientFactory helixSwarmClientFactory(
            InventoryService inventoryService,
            ObjectMapper objectMapper,
            OkHttpClient okHttpClient,
            @Value("${helix_swarm_response_page_size:100}") int pageSize) {
        return HelixSwarmClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .inventoryService(inventoryService)
                .build();
    }

    @Bean
    public HelixSwarmEnrichmentService helixSwarmEnrichmentService(
            @Value("${helix_swarm_fork_threshold:16}") int forkThreshold,
            @Value("${helix_swarm_thread_count:8}") int threadCount) {
        return new HelixSwarmEnrichmentService(threadCount, forkThreshold);
    }

    @Bean
    public HelixSwarmReviewDataSource helixSwarmReviewDataSource(IngestionEngine ingestionEngine,
                                                                 HelixSwarmClientFactory clientFactory,
                                                                 HelixSwarmEnrichmentService enrichmentService) {
        return ingestionEngine.add(HELIX_SWARM_REVIEWS_DATA_SOURCE,
                new HelixSwarmReviewDataSource(clientFactory, enrichmentService));
    }

    @Bean
    public HelixSwarmReviewController helixSwarmReviewController(IngestionEngine ingestionEngine,
                                                                 HelixSwarmReviewDataSource dataSource,
                                                                 ObjectMapper objectMapper,
                                                                 StorageDataSink storageDataSink,
                                                                 @Qualifier("onboardingInDays") int onboardingInDays) {
        return ingestionEngine.add(HELIX_SWARM_REVIEWS_CONTROLLER, HelixSwarmReviewController.builder()
                .dataSource(dataSource)
                .objectMapper(objectMapper)
                .onboardingScanInDays(onboardingInDays)
                .storageDataSink(storageDataSink)
                .build());
    }

}
