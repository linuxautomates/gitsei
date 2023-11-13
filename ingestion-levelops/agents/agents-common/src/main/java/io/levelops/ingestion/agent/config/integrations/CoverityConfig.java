package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.CoverityControllers;
import io.levelops.ingestion.agent.ingestion.CoverityIterativeScanController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.coverity.client.CoverityClientFactory;
import io.levelops.integrations.coverity.models.CoverityIterativeScanQuery;
import io.levelops.integrations.coverity.sources.CoverityMergedDefectDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoverityConfig {

    @Value("${coverity.onboarding_in_days:7}")
    private Integer coverityOnBoardingInDays;

    @Bean
    public CoverityClientFactory coverityClientFactory(InventoryService inventoryService,
                                                       @Qualifier("allowUnsafeSSLCoverity") Boolean allowUnsafeSSL) {
        return CoverityClientFactory.builder()
                .inventoryService(inventoryService)
                .allowUnsafeSSL(allowUnsafeSSL)
                .build();
    }

    @Bean("coverityDefectsController")
    public IntegrationController<CoverityIterativeScanQuery> coverityDefectsController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            CoverityClientFactory coverityClientFactory) {

        CoverityMergedDefectDataSource repositoryDataSource = ingestionEngine.add("CoverityDefectsDataSource",
                new CoverityMergedDefectDataSource(coverityClientFactory));

        return ingestionEngine.add("CoverityDefectsController", CoverityControllers.defectsController()
                .objectMapper(objectMapper)
                .dataSource(repositoryDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean
    public CoverityIterativeScanController coverityIterativeScanController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            @Qualifier("coverityDefectsController") IntegrationController<CoverityIterativeScanQuery> coverityDefectsController) {

        return ingestionEngine.add("CoverityIterativeScanController", CoverityIterativeScanController.builder()
                .objectMapper(objectMapper)
                .defectsController(coverityDefectsController)
                .onBoardingInDays(coverityOnBoardingInDays)
                .build());
    }
}
