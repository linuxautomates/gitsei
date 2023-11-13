package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.Rapid7Controller;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.rapid7.client.Rapid7ClientFactory;
import io.levelops.integrations.rapid7.sources.Rapid7VulnerabilityDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Rapid7Config {

    @Value("${RAPID7_OUTPUT_PAGE_SIZE:}")
    private Integer outputPageSize;

    @Bean
    public Rapid7ClientFactory rapid7ClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        return new Rapid7ClientFactory(inventoryService, objectMapper, okHttpClient);
    }

    @Bean
    public Rapid7Controller rapid7Controller(
            IngestionEngine ingestionEngine,
            @Qualifier("onboardingInDays") int onboardingInDays,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            Rapid7ClientFactory rapid7ClientFactory) {

        Rapid7VulnerabilityDataSource rapid7VulnerabilityDataSource = ingestionEngine.add("Rapid7DataSource",
                new Rapid7VulnerabilityDataSource(rapid7ClientFactory));
        return ingestionEngine.add("Rapid7Controller", Rapid7Controller.builder()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .vulnerabilityDataSource(rapid7VulnerabilityDataSource)
                .onboardingInDays(onboardingInDays)
                .outputPageSize(outputPageSize)
                .build());

    }

}
