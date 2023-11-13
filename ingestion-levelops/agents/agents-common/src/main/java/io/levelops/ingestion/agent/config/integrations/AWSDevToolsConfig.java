package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.AWSDevToolsController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClientFactory;
import io.levelops.integrations.awsdevtools.services.AWSDevToolsEnrichmentService;
import io.levelops.integrations.awsdevtools.sources.AWSDevToolsBuildBatchDataSource;
import io.levelops.integrations.awsdevtools.sources.AWSDevToolsBuildDataSource;
import io.levelops.integrations.awsdevtools.sources.AWSDevToolsProjectDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AWSDevToolsConfig {

    private static final String AWS_DEV_TOOLS_CONTROLLER = "AWSDevToolsController";
    private static final String AWS_DEV_TOOLS_PROJECT_DATA_SOURCE = "AWSDevToolsProjectDataSource";
    private static final String AWS_DEV_TOOLS_BUILD_DATA_SOURCE = "AWSDevToolsBuildDataSource";
    private static final String AWS_DEV_TOOLS_BUILD_BATCH_DATA_SOURCE = "AWSDevToolsBuildBatchDataSource";

    @Bean
    public AWSDevToolsClientFactory awsDevToolsClientFactory(InventoryService inventoryService,
                                                             @Value("${awsdevtools_response_page_size:100}") int pageSize) {
        return AWSDevToolsClientFactory.builder()
                .inventoryService(inventoryService)
                .pageSize(pageSize)
                .build();
    }

    @Bean
    public AWSDevToolsEnrichmentService awsDevToolsEnrichmentService(@Value("${awsdevtools_fork_threshold:32}") int forkThreshold,
                                                                     @Value("${awsdevtools_thread_count:8}") int threadCount) {
        return new AWSDevToolsEnrichmentService(threadCount, forkThreshold);
    }

    @Bean
    public AWSDevToolsProjectDataSource awsDevToolsProjectDataSource(IngestionEngine ingestionEngine,
                                                                     AWSDevToolsClientFactory clientFactory) {
        return ingestionEngine.add(AWS_DEV_TOOLS_PROJECT_DATA_SOURCE, new AWSDevToolsProjectDataSource(clientFactory));
    }

    @Bean
    public AWSDevToolsBuildDataSource awsDevToolsBuildDataSource(IngestionEngine ingestionEngine,
                                                                 AWSDevToolsClientFactory clientFactory,
                                                                 AWSDevToolsEnrichmentService enrichmentService) {
        return ingestionEngine.add(AWS_DEV_TOOLS_BUILD_DATA_SOURCE, new AWSDevToolsBuildDataSource(clientFactory, enrichmentService));
    }

    @Bean
    public AWSDevToolsBuildBatchDataSource awsDevToolsBuildBatchDataSource(IngestionEngine ingestionEngine,
                                                                           AWSDevToolsClientFactory clientFactory,
                                                                           AWSDevToolsEnrichmentService enrichmentService) {
        return ingestionEngine.add(AWS_DEV_TOOLS_BUILD_BATCH_DATA_SOURCE, new AWSDevToolsBuildBatchDataSource(clientFactory, enrichmentService));
    }

    @Bean
    public AWSDevToolsController awsDevToolsController(IngestionEngine ingestionEngine,
                                                       AWSDevToolsProjectDataSource projectDataSource,
                                                       AWSDevToolsBuildDataSource buildDataSource,
                                                       AWSDevToolsBuildBatchDataSource buildBatchDataSource,
                                                       ObjectMapper objectMapper, StorageDataSink storageDataSink,
                                                       @Qualifier("onboardingInDays") int onboardingInDays) {
        return ingestionEngine.add(AWS_DEV_TOOLS_CONTROLLER, AWSDevToolsController.builder()
                .projectDataSource(projectDataSource)
                .buildDataSource(buildDataSource)
                .buildBatchDataSource(buildBatchDataSource)
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .onboardingScanInDays(onboardingInDays)
                .build());
    }
}
