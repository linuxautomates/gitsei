package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.TenableIngestionController;
import io.levelops.ingestion.agent.ingestion.TenableMetadataIngestionController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.tenable.client.TenableClientFactory;
import io.levelops.integrations.tenable.sources.TenableAssetDataSource;
import io.levelops.integrations.tenable.sources.TenableNetworkDataSource;
import io.levelops.integrations.tenable.sources.TenableScannerDataSource;
import io.levelops.integrations.tenable.sources.TenableScannerPoolDataSource;
import io.levelops.integrations.tenable.sources.TenableVulnerabilityDataSource;
import io.levelops.integrations.tenable.sources.TenableWASDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for registering datasources and controller with ingestion engine
 */
@Configuration
public class TenableConfig {

    private static final String TENABLE_ASSET_DATA_SOURCE = "TenableAssetDataSource";
    private static final String TENABLE_VULNERABILITY_DATA_SOURCE = "TenableVulnerabilityDataSource";
    private static final String TENABLE_INGESTION_CONTROLLER = "TenableIngestionController";
    private static final String TENABLE_METADATA_INGESTION_CONTROLLER = "TenableMetadataIngestionController";
    private static final String TENABLE_SCANNER_DATA_SOURCE = "TenableScannerDataSource";
    private static final String TENABLE_NETWORK_DATA_SOURCE = "TenableNetworkDataSource";
    private static final String TENABLE_SCANNER_POOL_DATA_SOURCE = "TenableScannerPoolDataSource";
    private static final String TENABLE_WAS_DATA_SOURCE = "TenableWASDataSource";


    @Bean
    public TenableClientFactory tenableClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                                     OkHttpClient okHttpClient,
                                                     @Value("${tenable_response_page_size:200}") int pageSize) {
        return TenableClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .pageSize(pageSize)
                .build();
    }

    @Bean
    public TenableAssetDataSource tenableAssetDataSource(IngestionEngine ingestionEngine,
                                                         TenableClientFactory tenableClientFactory) {
        return ingestionEngine.add(TENABLE_ASSET_DATA_SOURCE, new TenableAssetDataSource(tenableClientFactory));
    }

    @Bean
    public TenableVulnerabilityDataSource tenableVulnerabilityDataSource(IngestionEngine ingestionEngine,
                                                                         TenableClientFactory tenableClientFactory) {
        return ingestionEngine.add(TENABLE_VULNERABILITY_DATA_SOURCE, new TenableVulnerabilityDataSource(tenableClientFactory));
    }

    @Bean
    public TenableNetworkDataSource tenableNetworkDataSource(IngestionEngine ingestionEngine,
                                                                         TenableClientFactory tenableClientFactory) {
        return ingestionEngine.add(TENABLE_NETWORK_DATA_SOURCE, new TenableNetworkDataSource(tenableClientFactory));
    }

    @Bean
    public TenableScannerDataSource tenableScannerDataSource(IngestionEngine ingestionEngine,
                                                                         TenableClientFactory tenableClientFactory) {
        return ingestionEngine.add(TENABLE_SCANNER_DATA_SOURCE, new TenableScannerDataSource(tenableClientFactory));
    }

    @Bean
    public TenableScannerPoolDataSource tenableScannerPoolDataSource(IngestionEngine ingestionEngine,
                                                                         TenableClientFactory tenableClientFactory) {
        return ingestionEngine.add(TENABLE_SCANNER_POOL_DATA_SOURCE, new TenableScannerPoolDataSource(tenableClientFactory));
    }

    @Bean
    public TenableWASDataSource tenableWASDataSource(IngestionEngine ingestionEngine,
                                                             TenableClientFactory tenableClientFactory) {
        return ingestionEngine.add(TENABLE_WAS_DATA_SOURCE, new TenableWASDataSource(tenableClientFactory));
    }

    @Bean
    public TenableIngestionController tenableIngestionController(IngestionEngine ingestionEngine,
                                                                 ObjectMapper objectMapper, StorageDataSink storageDataSink,
                                                                 TenableAssetDataSource tenableAssetDataSource,
                                                                 TenableVulnerabilityDataSource tenableVulnerabilityDataSource,
                                                                 TenableWASDataSource tenableWASDataSource,
                                                                 @Qualifier("onboardingInDays") int onboardingInDays) {
        return ingestionEngine.add(TENABLE_INGESTION_CONTROLLER, TenableIngestionController.builder()
                .tenableAssetDataSource(tenableAssetDataSource)
                .tenableVulnerabilityDataSource(tenableVulnerabilityDataSource)
                .tenableWASDataSource(tenableWASDataSource)
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .onboardingScanInDays(onboardingInDays)
                .build());
    }

    @Bean
    public TenableMetadataIngestionController tenableMetadataIngestionController(IngestionEngine ingestionEngine,
                                                                                 ObjectMapper objectMapper,
                                                                                 StorageDataSink storageDataSink,
                                                                                 TenableScannerDataSource tenableScannerDataSource,
                                                                                 TenableNetworkDataSource tenableNetworkDataSource,
                                                                                 TenableScannerPoolDataSource tenableScannerPoolDataSource) {
        return ingestionEngine.add(TENABLE_METADATA_INGESTION_CONTROLLER, TenableMetadataIngestionController.builder()
                .tenableNetworkDataSource(tenableNetworkDataSource)
                .tenableScannerDataSource(tenableScannerDataSource)
                .tenableScannerPoolDataSource(tenableScannerPoolDataSource)
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .build());
    }
}
