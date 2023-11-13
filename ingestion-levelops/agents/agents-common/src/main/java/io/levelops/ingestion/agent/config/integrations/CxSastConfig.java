package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.agent.ingestion.CxSastController;
import io.levelops.agent.ingestion.CxSastIterativeScanController;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.checkmarx.client.cxsast.CxSastClientFactory;
import io.levelops.integrations.checkmarx.sources.cxsast.CxSastProjectDataSource;
import io.levelops.integrations.checkmarx.sources.cxsast.CxSastScanDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CxSastConfig {

    private static final String CxSast_PROJECT_CONTROLLER = "CxSastProjectController";
    private static final String CxSast_SCAN_CONTROLLER = "CxSastScanController";

    private static final String CxSAST_PROJECT_DATA_SOURCE = "CxSastProjectDataSource";
    private static final String CxSAST_SCAN_DATA_SOURCE = "CxSastScanDataSource";

    @Value("${cxsast.onboarding_in_days:90}")
    private Integer cxsastOnboardingInDays;

    @Bean
    public CxSastClientFactory cxSastClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                                   OkHttpClient okHttpClient) {
        return CxSastClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .build();
    }

    @Bean("cXSastProjectController")
    public IntegrationController<CxSastProjectDataSource.CxSastProjectQuery> cxSastProjectController(IngestionEngine ingestionEngine,
                                                                                                     ObjectMapper objectMapper, StorageDataSink storageDataSink,
                                                                                                     CxSastClientFactory clientFactory) {
        CxSastProjectDataSource dataSource = ingestionEngine.add(CxSAST_PROJECT_DATA_SOURCE,
                new CxSastProjectDataSource(clientFactory));
        return ingestionEngine.add(CxSast_PROJECT_CONTROLLER, CxSastController.projectController()
                .storageDataSink(storageDataSink)
                .dataSource(dataSource)
                .objectMapper(objectMapper)
                .build());
    }

    @Bean("cXSastScanController")
    public IntegrationController<CxSastScanDataSource.CxSastScanQuery> cxSastScanController(IngestionEngine ingestionEngine, ObjectMapper mapper,
                                                                                            StorageDataSink sink, CxSastClientFactory clientFactory) {
        CxSastScanDataSource dataSource = ingestionEngine.add(CxSAST_SCAN_DATA_SOURCE,
                new CxSastScanDataSource(clientFactory));
        return ingestionEngine.add(CxSast_SCAN_CONTROLLER, CxSastController.scanController()
                .objectMapper(mapper)
                .dataSource(dataSource)
                .storageDataSink(sink)
                .build());
    }

    @Bean
    public CxSastIterativeScanController cxSastIterativeScanController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            InventoryService inventoryService,
            @Qualifier("cXSastProjectController")
                    IntegrationController<CxSastProjectDataSource.CxSastProjectQuery> cXSastProjectController,
            @Qualifier("cXSastScanController")
                    IntegrationController<CxSastScanDataSource.CxSastScanQuery> cXSastScanController) {
        return ingestionEngine.add("CxSastIterativeScanController", CxSastIterativeScanController.builder()
                .objectMapper(objectMapper)
                .inventoryService(inventoryService)
                .onboardingInDays(cxsastOnboardingInDays)
                .projectController(cXSastProjectController)
                .scanController(cXSastScanController)
                .build());
    }
}


