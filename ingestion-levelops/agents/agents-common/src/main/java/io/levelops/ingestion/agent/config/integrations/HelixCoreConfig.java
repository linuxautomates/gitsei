package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.HelixCoreControllers;
import io.levelops.ingestion.agent.ingestion.HelixCoreGetChangeListController;
import io.levelops.ingestion.agent.ingestion.HelixCoreIterativeScanController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.helixcore.client.HelixCoreClientFactory;
import io.levelops.integrations.helixcore.models.HelixCoreIterativeQuery;
import io.levelops.integrations.helixcore.services.HelixCoreChangeListFetchService;
import io.levelops.integrations.helixcore.sources.HelixCoreChangeListDataSource;
import io.levelops.integrations.helixcore.sources.HelixCoreDepotDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

/**
 * Configuration class for registering datasources and controller with ingestion engine
 */
@Configuration
public class HelixCoreConfig {

    @Value("${JIRA_OUTPUT_PAGE_SIZE:}")
    private Integer helixCoreOutputPageSize;

    public static final String HELIX_CORE_DEPOT_DATA_SOURCE = "HelixCoreDepotDataSource";
    public static final String HELIX_CORE_DEPOT_CONTROLLER = "HelixCoreDepotController";
    public static final String HELIX_CORE_CHANGE_LIST_DATA_SOURCE = "HelixCoreChangeListDataSource";
    public static final String HELIX_CORE_CHANGE_LIST_CONTROLLER = "HelixCoreChangeListController";
    public static final String HELIX_CORE_ITERATIVE_SCAN_CONTROLLER = "HelixCoreIterativeScanController";

    @Bean
    public HelixCoreClientFactory helixCoreClientFactory(InventoryService inventoryService) {
        return HelixCoreClientFactory.builder()
                .inventoryService(inventoryService)
                .build();
    }

    @Bean("helixCoreDepotController")
    public IntegrationController<HelixCoreIterativeQuery> helixCoreDepotController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            HelixCoreClientFactory helixCoreClientFactory) {

        HelixCoreDepotDataSource depotDataSource = ingestionEngine.add(HELIX_CORE_DEPOT_DATA_SOURCE,
                new HelixCoreDepotDataSource(helixCoreClientFactory));

        return ingestionEngine.add(HELIX_CORE_DEPOT_CONTROLLER, HelixCoreControllers.helixCoreDepotController()
                .objectMapper(objectMapper)
                .helixCoreDepotDataSource(depotDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean
    public HelixCoreChangeListDataSource HelixCoreChangeListDataSource(IngestionEngine ingestionEngine,
                                                                       HelixCoreClientFactory helixCoreClientFactory,
                                                                       @Qualifier("helixMaxFileSize") int helixMaxFileSize,
                                                                       @Qualifier("helixZoneId") ZoneId helixZoneId) {
        return ingestionEngine.add(HELIX_CORE_CHANGE_LIST_DATA_SOURCE,
                new HelixCoreChangeListDataSource(helixCoreClientFactory, new HelixCoreChangeListFetchService(), helixMaxFileSize, helixZoneId));
    }

    @Bean("helixCoreChangeListController")
    public IntegrationController<HelixCoreIterativeQuery> helixCoreChangeListController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            HelixCoreChangeListDataSource changeListDataSource) {

        return ingestionEngine.add(HELIX_CORE_CHANGE_LIST_CONTROLLER, HelixCoreControllers.helixCoreChangeListController()
                .objectMapper(objectMapper)
                .helixCoreChangeListDataSource(changeListDataSource)
                .storageDataSink(storageDataSink)
                .outputPageSize(helixCoreOutputPageSize)
                .build());
    }

    @Bean
    public HelixCoreIterativeScanController helixCoreIterativeScanController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            @Qualifier("helixCoreDepotController") IntegrationController<HelixCoreIterativeQuery> helixCoreDepotController,
            @Qualifier("helixCoreChangeListController") IntegrationController<HelixCoreIterativeQuery> helixCoreChangeListController,
            @Qualifier("onboardingInDays") int onboardingInDays,
            @Value("${pageSizeInDays:1}") int pageSizeInDays) {

        return ingestionEngine.add(HELIX_CORE_ITERATIVE_SCAN_CONTROLLER, HelixCoreIterativeScanController.builder()
                .objectMapper(objectMapper)
                .helixCoreChangeListController(helixCoreChangeListController)
                .helixCoreDepotController(helixCoreDepotController)
                .onboardingInDays(onboardingInDays)
                .pageSizeInDays(pageSizeInDays)
                .build());
    }

    @Bean
    public HelixCoreGetChangeListController helixCoreGetChangeListController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            HelixCoreChangeListDataSource changeListDataSource) {
        return ingestionEngine.add("HelixCoreGetChangeListController", new HelixCoreGetChangeListController(objectMapper, changeListDataSource));
    }
}
