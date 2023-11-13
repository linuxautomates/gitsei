package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.SalesforceIngestionController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.salesforce.client.SalesforceClientFactory;
import io.levelops.integrations.salesforce.sources.SalesforceCaseDataSource;
import io.levelops.integrations.salesforce.sources.SalesforceCaseHistoryDataSource;
import io.levelops.integrations.salesforce.sources.SalesforceContractDataSource;
import io.levelops.integrations.salesforce.sources.SalesforceSolutionDataSource;
import io.levelops.integrations.salesforce.sources.SalesforceCaseCommentDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SalesforceConfig {
    private static final String SALES_FORCE_CASE_DATA_SOURCE = "SalesforceCaseDataSource";
    private static final String SALES_FORCE_SOLUTION_DATA_SOURCE = "SalesforceSolutionDataSource";
    private static final String SALES_FORCE_CONTRACT_DATA_SOURCE = "SalesforceContractDataSource";
    private static final String SALES_FORCE_CASE_HISTORY_DATA_SOURCE = "SalesforceCaseHistoryDataSource";
    private static final String SALES_FORCE_CASE_COMMENT_DATA_SOURCE = "SalesforceCaseCommentDataSource";
    private static final String SALES_FORCE_INGESTION_CONTROLLER = "SalesforceIngestionController";

    @Bean
    public SalesforceClientFactory salesforceClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                                           OkHttpClient okHttpClient,
                                                           @Value("${salesforce_response_page_size:10000}") int pageSize) {
        return SalesforceClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .pageSize(pageSize)
                .build();
    }

    @Bean
    public SalesforceCaseDataSource salesforceCaseDataSource(IngestionEngine ingestionEngine,
                                                             SalesforceClientFactory salesForceClientFactory) {
        return ingestionEngine.add(SALES_FORCE_CASE_DATA_SOURCE, new SalesforceCaseDataSource(salesForceClientFactory));
    }

    @Bean
    public SalesforceSolutionDataSource salesforceSolutionDataSource(IngestionEngine ingestionEngine,
                                                                     SalesforceClientFactory salesForceClientFactory) {
        return ingestionEngine.add(SALES_FORCE_SOLUTION_DATA_SOURCE, new SalesforceSolutionDataSource(salesForceClientFactory));
    }

    @Bean
    public SalesforceContractDataSource salesforceContractDataSource(IngestionEngine ingestionEngine,
                                                                     SalesforceClientFactory salesForceClientFactory) {
        return ingestionEngine.add(SALES_FORCE_CONTRACT_DATA_SOURCE, new SalesforceContractDataSource(salesForceClientFactory));
    }

    @Bean
    public SalesforceCaseHistoryDataSource salesforceCaseHistoryDataSource(IngestionEngine ingestionEngine,
                                                                           SalesforceClientFactory salesForceClientFactory) {
        return ingestionEngine.add(SALES_FORCE_CASE_HISTORY_DATA_SOURCE, new SalesforceCaseHistoryDataSource(salesForceClientFactory));
    }

    @Bean
    public SalesforceCaseCommentDataSource salesforceCaseCommentDataSource(IngestionEngine ingestionEngine,
                                                           SalesforceClientFactory salesForceClientFactory) {
        return ingestionEngine.add(SALES_FORCE_CASE_COMMENT_DATA_SOURCE, new SalesforceCaseCommentDataSource(salesForceClientFactory));
    }

    @Bean
    public SalesforceIngestionController salesforceIngestionController(IngestionEngine ingestionEngine,
                                                                       ObjectMapper objectMapper,
                                                                       StorageDataSink storageDataSink,
                                                                       SalesforceCaseDataSource salesForceCaseDataSource,
                                                                       SalesforceSolutionDataSource salesForceSolutionDataSource,
                                                                       SalesforceContractDataSource salesForceContractDataSource,
                                                                       SalesforceCaseHistoryDataSource salesForceCaseHistoryDataSource,
                                                                       SalesforceCaseCommentDataSource salesforceCaseCommentDataSource,
                                                                       @Qualifier("onboardingInDays") int onboardingInDays) {
        return ingestionEngine.add(SALES_FORCE_INGESTION_CONTROLLER, SalesforceIngestionController.builder()
                .salesForceCaseDataSource(salesForceCaseDataSource)
                .salesForceSolutionDataSource(salesForceSolutionDataSource)
                .salesForceContractDataSource(salesForceContractDataSource)
                .salesForceCaseHistoryDataSource(salesForceCaseHistoryDataSource)
                .salesforceCaseCommentDataSource(salesforceCaseCommentDataSource)
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .onboardingScanInDays(onboardingInDays)
                .build());
    }
}
