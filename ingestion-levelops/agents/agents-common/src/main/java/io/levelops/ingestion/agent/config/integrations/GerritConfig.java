package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.GerritControllers;
import io.levelops.ingestion.agent.ingestion.GerritIngestionController;
import io.levelops.ingestion.agent.ingestion.GerritIterativeScanController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.gerrit.client.GerritClientFactory;
import io.levelops.integrations.gerrit.sources.GerritAccountsDataSource;
import io.levelops.integrations.gerrit.sources.GerritGroupDataSource;
import io.levelops.integrations.gerrit.sources.GerritRepositoryDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

/**
 * Configuration class for registering datasources and controller with ingestion engine
 */
@Configuration
public class GerritConfig {

    private static final String GERRIT_INGESTION_CONTROLLER = "GerritIngestionController";
    private static final String GERRIT_ACCOUNTS_DATA_SOURCE = "GerritAccountsDataSource";
    private static final String GERRIT_GROUP_DATA_SOURCE = "GerritGroupDataSource";
    private static final String GERRIT_REPOSITORY_DATA_SOURCE = "GerritRepositoryDataSource";
    private static final String GERRIT_REPOSITORY_CONTROLLER = "GerritRepositoryController";
    private static final String GERRIT_PULL_REQUEST_CONTROLLER = "GerritPullRequestController";
    private static final String GERRIT_ITERATIVE_SCAN_CONTROLLER = "GerritIterativeScanController";
    private static final String GERRIT_PULL_REQUEST_DATA_SOURCE = "GerritPullRequestDataSource";

    @Bean
    public GerritClientFactory gerritClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                                   OkHttpClient okHttpClient) {
        return GerritClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .build();
    }

    @Bean("gerritRepositoryController")
    public IntegrationController<GerritRepositoryDataSource.GerritRepositoryQuery> gerritRepositoryController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GerritClientFactory gerritClientFactory) {

        GerritRepositoryDataSource repositoryDataSource = ingestionEngine.add(GERRIT_REPOSITORY_DATA_SOURCE,
                new GerritRepositoryDataSource(gerritClientFactory));

        return ingestionEngine.add(GERRIT_REPOSITORY_CONTROLLER, GerritControllers.repositoryController()
                .objectMapper(objectMapper)
                .repositoryDataSource(repositoryDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("gerritPullRequestController")
    public IntegrationController<GerritRepositoryDataSource.GerritRepositoryQuery> gerritPullRequestController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GerritClientFactory gerritClientFactory) {

        GerritRepositoryDataSource repositoryDataSource = ingestionEngine.add(GERRIT_PULL_REQUEST_DATA_SOURCE,
                new GerritRepositoryDataSource(gerritClientFactory,
                        EnumSet.of(GerritRepositoryDataSource.Enrichment.PULL_REQUESTS, GerritRepositoryDataSource.Enrichment.REVIEWERS)));

        return ingestionEngine.add(GERRIT_PULL_REQUEST_CONTROLLER, GerritControllers.pullRequestController()
                .objectMapper(objectMapper)
                .repositoryDataSource(repositoryDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean
    public GerritIterativeScanController gerritIterativeScanController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            @Qualifier("gerritPullRequestController") IntegrationController<GerritRepositoryDataSource.GerritRepositoryQuery> gerritPullRequestController,
            @Qualifier("gerritRepositoryController") IntegrationController<GerritRepositoryDataSource.GerritRepositoryQuery> gerritRepositoryController,
            InventoryService inventoryService,
            @Qualifier("onboardingInDays") int onboardingInDays) {

        return ingestionEngine.add(GERRIT_ITERATIVE_SCAN_CONTROLLER, GerritIterativeScanController.builder()
                .objectMapper(objectMapper)
                .repositoryController(gerritRepositoryController)
                .pullRequestController(gerritPullRequestController)
                .onboardingInDays(onboardingInDays)
                .inventoryService(inventoryService)
                .build());
    }

    @Bean
    public GerritGroupDataSource gerritGroupDataSource(IngestionEngine ingestionEngine,
                                                       GerritClientFactory gerritClientFactory) {
        return ingestionEngine.add(GERRIT_GROUP_DATA_SOURCE, new GerritGroupDataSource(gerritClientFactory));
    }

    @Bean
    public GerritAccountsDataSource gerritAccountsDataSource(IngestionEngine ingestionEngine,
                                                             GerritClientFactory gerritClientFactory) {
        return ingestionEngine.add(GERRIT_ACCOUNTS_DATA_SOURCE, new GerritAccountsDataSource(gerritClientFactory));
    }


    @Bean
    public GerritIngestionController gerritIngestionController(IngestionEngine ingestionEngine,
                                                               GerritGroupDataSource gerritGroupDataSource,
                                                               GerritAccountsDataSource gerritAccountsDataSource,
                                                               ObjectMapper objectMapper,
                                                               StorageDataSink storageDataSink,
                                                               @Qualifier("onboardingInDays") int onboardingInDays) {
        return ingestionEngine.add(GERRIT_INGESTION_CONTROLLER, GerritIngestionController.builder()
                .objectMapper(objectMapper)
                .gerritAccountsDataSource(gerritAccountsDataSource)
                .gerritGroupDataSource(gerritGroupDataSource)
                .onboardingScanInDays(onboardingInDays)
                .storageDataSink(storageDataSink)
                .build());
    }
}
