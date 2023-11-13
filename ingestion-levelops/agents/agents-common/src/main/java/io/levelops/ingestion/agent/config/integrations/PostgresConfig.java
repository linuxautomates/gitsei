package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.PostgresControllers;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.postgres.client.PostgresClientFactory;
import io.levelops.integrations.postgres.sources.PostgresQueryDataSource;
import io.levelops.integrations.postgres.sources.PostgresQueryDataSource.PostgresQuery;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostgresConfig {

    @Bean
    public PostgresClientFactory postgresClientFactory(InventoryService inventoryService) {
        return new PostgresClientFactory(inventoryService);
    }

    @Bean
    public IntegrationController<PostgresQuery> postgresController(
            IngestionEngine ingestionEngine,
            @Qualifier("onboardingInDays") int onboardingInDays,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            PostgresClientFactory postgresClientFactory) {

        PostgresQueryDataSource postgresDataSource = ingestionEngine.add("PostgresDataSource",
                new PostgresQueryDataSource(postgresClientFactory));
        return ingestionEngine.add("PostgresController", PostgresControllers.postgresController()
                .dataSource(postgresDataSource)
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .build());
    }

}
