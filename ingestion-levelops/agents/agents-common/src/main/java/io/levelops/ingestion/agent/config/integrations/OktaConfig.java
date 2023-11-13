package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.OktaIngestionController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.okta.client.OktaClientFactory;
import io.levelops.integrations.okta.services.OktaEnrichmentService;
import io.levelops.integrations.okta.sources.OktaGroupsDataSource;
import io.levelops.integrations.okta.sources.OktaUsersDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OktaConfig {

    private static final String OKTA_USERS_DATA_SOURCE = "OktaUsersDataSource";
    private static final String OKTA_GROUPS_DATA_SOURCE = "OktaGroupsDataSource";
    private static final String OKTA_INGESTION_CONTROLLER = "OktaIngestionController";

    @Bean
    public OktaClientFactory oktaClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                               OkHttpClient okHttpClient,
                                               @Value("${okta_response_page_size:100}") int pageSize) {
        return OktaClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .pageSize(pageSize)
                .build();
    }

    @Bean
    public OktaEnrichmentService oktaService(@Value("${okta_fork_threshold:32}") int forkThreshold,
                                             @Value("${okta_thread_count:8}") int threadCount) {
        return new OktaEnrichmentService(threadCount, forkThreshold);
    }

    @Bean
    public OktaUsersDataSource oktaUsersDataSource(IngestionEngine ingestionEngine,
                                                   OktaClientFactory oktaClientFactory,
                                                   OktaEnrichmentService enrichmentService) {
        return ingestionEngine.add(OKTA_USERS_DATA_SOURCE, new OktaUsersDataSource(oktaClientFactory, enrichmentService));
    }

    @Bean
    public OktaGroupsDataSource oktaGroupsDataSource(IngestionEngine ingestionEngine,
                                                     OktaClientFactory oktaClientFactory,
                                                     OktaEnrichmentService enrichmentService) {
        return ingestionEngine.add(OKTA_GROUPS_DATA_SOURCE, new OktaGroupsDataSource(oktaClientFactory, enrichmentService));
    }

    @Bean
    public OktaIngestionController oktaIngestionController(IngestionEngine ingestionEngine,
                                                           OktaUsersDataSource usersDataSource,
                                                           OktaGroupsDataSource groupsDataSource,
                                                           ObjectMapper objectMapper, StorageDataSink storageDataSink,
                                                           @Qualifier("onboardingInDays") int onboardingInDays) {
        return ingestionEngine.add(OKTA_INGESTION_CONTROLLER, OktaIngestionController.builder()
                .usersDataSource(usersDataSource)
                .groupsDataSource(groupsDataSource)
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .onboardingScanInDays(onboardingInDays)
                .build());
    }
}
