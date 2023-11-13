package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.HarnessNGController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.harnessng.client.HarnessNGClientFactory;
import io.levelops.integrations.harnessng.source.HarnessNGEnrichPipelineDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class HarnessNGConfig {

    private static final String HARNESSNG_CONTROLLER = "HarnessNGController";
    private static final String HARNESSNG_PIPELINE_DATA_SOURCE = "HarnessNGEnrichPipelineDataSource";

    @Bean
    public HarnessNGClientFactory harnessNGClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                                     OkHttpClient okHttpClient) {
        return HarnessNGClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .build();
    }

    @Bean
    public HarnessNGController harnessNGController(IngestionEngine ingestionEngine,
                                                   HarnessNGEnrichPipelineDataSource pipelineDataSource,
                                                   InventoryService inventoryService,
                                                   ObjectMapper objectMapper, StorageDataSink storageDataSink,
                                                   @Qualifier("onboardingInDays") int onboardingInDays,
                                                   @Value("${HARNESSNG_EXECUTED_PIPELINES_TIME_WINDOW_IN_MILLIS:}") Long executedPipelinesTimeWindowInMillis) {
        return ingestionEngine.add(HARNESSNG_CONTROLLER, HarnessNGController.builder()
                .pipelineDataSource(pipelineDataSource)
                .objectMapper(objectMapper)
                .inventoryService(inventoryService)
                .storageDataSink(storageDataSink)
                .onboardingScanInDays(onboardingInDays)
                .executedPipelinesTimeWindowInMillis(executedPipelinesTimeWindowInMillis)
                .build());
    }

    @Bean
    public HarnessNGEnrichPipelineDataSource pipelineDataSource(IngestionEngine ingestionEngine,
                                                              HarnessNGClientFactory harnessNGClientFactory) {
        return ingestionEngine.add(HARNESSNG_PIPELINE_DATA_SOURCE, new HarnessNGEnrichPipelineDataSource(harnessNGClientFactory));
    }


}


