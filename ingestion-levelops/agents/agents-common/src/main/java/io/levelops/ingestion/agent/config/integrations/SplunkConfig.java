package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.SplunkControllers;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.splunk.client.SplunkClientFactory;
import io.levelops.integrations.splunk.sources.SplunkSearchDataSource;
import io.levelops.integrations.splunk.sources.SplunkSearchDataSource.SplunkSearchQuery;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SplunkConfig {

    @Bean
    public SplunkClientFactory splunkClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        return new SplunkClientFactory(inventoryService, objectMapper, okHttpClient);
    }

    @Bean
    public IntegrationController<SplunkSearchQuery> splunkSearchController(
            IngestionEngine ingestionEngine,
            @Qualifier("onboardingInDays") int onboardingInDays,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SplunkClientFactory splunkClientFactory) {

        SplunkSearchDataSource splunkSearchDataSource = ingestionEngine.add("SplunkSearchDataSource",
                new SplunkSearchDataSource(splunkClientFactory));
        return ingestionEngine.add("SplunkSearchController", SplunkControllers.splunkSearchController()
                .dataSource(splunkSearchDataSource)
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .build());
    }

}
