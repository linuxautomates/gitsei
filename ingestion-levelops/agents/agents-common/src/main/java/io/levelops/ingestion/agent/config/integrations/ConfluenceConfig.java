package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.ConfluenceSearchController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.confluence.client.ConfluenceClientFactory;
import io.levelops.integrations.confluence.sources.ConfluenceSearchDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfluenceConfig {

    @Value("${CONFLUENCE_OUTPUT_PAGE_SIZE:}")
    private Integer outputPageSize;

    @Bean
    public ConfluenceClientFactory confluenceClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        return new ConfluenceClientFactory(inventoryService, objectMapper, okHttpClient);
    }

    @Bean
    public ConfluenceSearchController confluenceSearchController(IngestionEngine ingestionEngine,
                                                                 ObjectMapper objectMapper,
                                                                 StorageDataSink storageDataSink,
                                                                 ConfluenceClientFactory confluenceClientFactory) {
        ConfluenceSearchDataSource confluenceSearchDataSource = ingestionEngine.add("ConfluenceSearchDataSource",
                new ConfluenceSearchDataSource(confluenceClientFactory));

        return ingestionEngine.add("ConfluenceSearchController", ConfluenceSearchController.builder()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .searchDataSource(confluenceSearchDataSource)
                .outputPageSize(outputPageSize)
                .build());
    }

}
