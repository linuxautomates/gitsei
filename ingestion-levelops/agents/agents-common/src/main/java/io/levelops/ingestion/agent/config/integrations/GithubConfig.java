package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.GithubControllers;
import io.levelops.ingestion.agent.ingestion.GithubIterativeScanController;
import io.levelops.ingestion.agent.ingestion.GithubPRController;
import io.levelops.ingestion.agent.ingestion.GithubWebhookController;
import io.levelops.ingestion.agent.ingestion.GithubWebhookEnrichmentController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.github.GithubDataSource;
import io.levelops.integrations.github.GithubWebhookEnrichDataSource;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.services.GithubOrganizationService;
import io.levelops.integrations.github.services.GithubRepositoryService;
import io.levelops.integrations.github.services.GithubUserService;
import io.levelops.integrations.github.sources.GithubProjectDataSource;
import io.levelops.integrations.github.sources.GithubProjectDataSource.GithubProjectQuery;
import io.levelops.integrations.github.sources.GithubRepositoryDataSource;
import io.levelops.integrations.github.sources.GithubRepositoryDataSource.GithubRepositoryQuery;
import io.levelops.integrations.github.sources.GithubUserDataSource;
import io.levelops.integrations.github.sources.GithubUserDataSource.GithubUserQuery;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

import static io.levelops.integrations.github.sources.GithubRepositoryDataSource.Enrichment.COMMITS;
import static io.levelops.integrations.github.sources.GithubRepositoryDataSource.Enrichment.ISSUES;
import static io.levelops.integrations.github.sources.GithubRepositoryDataSource.Enrichment.ISSUE_EVENTS;
import static io.levelops.integrations.github.sources.GithubRepositoryDataSource.Enrichment.LANGUAGES;
import static io.levelops.integrations.github.sources.GithubRepositoryDataSource.Enrichment.PULL_REQUESTS;
import static io.levelops.integrations.github.sources.GithubRepositoryDataSource.Enrichment.TAGS;

@Configuration
public class GithubConfig {

    @Value("${github.onboarding_in_days:7}")
    private Integer githubOnboardingInDays;

    @Bean
    public GithubClientFactory githubClientFactory(InventoryService inventoryService,
                                                   ObjectMapper objectMapper,
                                                   OkHttpClient okHttpClient,
                                                   @Qualifier("githubThrottlingMs") int githubThrottlingMs) {
        return new GithubClientFactory(inventoryService, objectMapper, okHttpClient, githubThrottlingMs);
    }

    @Bean
    public GithubOrganizationService githubOrganizationService(@Qualifier("githubEnableCaching") boolean enableCaching,
                                                               @Qualifier("githubOrgCacheMaxSize") long cacheMaxSize,
                                                               @Qualifier("githubOrgCacheExpiryInHours") long cacheExpiryInHours,
                                                               GithubClientFactory GithubClientFactory,
                                                               InventoryService inventoryService) {
        return new GithubOrganizationService(enableCaching, cacheMaxSize, cacheExpiryInHours, GithubClientFactory, inventoryService);
    }

    @Bean
    public GithubRepositoryService githubRepositoryService(@Qualifier("githubEnableCaching") boolean enableCaching,
                                                           @Qualifier("githubRepoCacheMaxSize") long cacheMaxSize,
                                                           @Qualifier("githubRepoCacheExpiryInHours") long cacheExpiryInHours,
                                                           GithubClientFactory GithubClientFactory) {
        return new GithubRepositoryService(enableCaching, cacheMaxSize, cacheExpiryInHours, GithubClientFactory);
    }

    @Bean
    public GithubUserService githubUserService(GithubClientFactory githubClientFactory) {
        return new GithubUserService(githubClientFactory);
    }

    @Bean("githubRepositoryController")
    public IntegrationController<GithubRepositoryQuery> githubRepositoryController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GithubClientFactory githubClientFactory,
            GithubOrganizationService githubOrganizationService,
            GithubRepositoryService githubRepositoryService) {

        GithubRepositoryDataSource repositoryDataSource = ingestionEngine.add("GithubRepositoryDataSource",
                new GithubRepositoryDataSource(objectMapper, githubClientFactory, githubOrganizationService, githubRepositoryService, EnumSet.of(LANGUAGES)));

        return ingestionEngine.add("GithubRepositoryController", GithubControllers.repositoryController()
                .objectMapper(objectMapper)
                .repositoryDataSource(repositoryDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("githubCommitController")
    public IntegrationController<GithubRepositoryQuery> githubCommitController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GithubClientFactory githubClientFactory,
            GithubOrganizationService githubOrganizationService,
            GithubRepositoryService githubRepositoryService) {

        GithubRepositoryDataSource githubCommitDataSource = ingestionEngine.add("GithubCommitDataSource",
                new GithubRepositoryDataSource(objectMapper, githubClientFactory, githubOrganizationService, githubRepositoryService,
                        EnumSet.of(COMMITS)));

        return ingestionEngine.add("GithubCommitController", GithubControllers.commitController()
                .objectMapper(objectMapper)
                .enrichedRepositoryDataSource(githubCommitDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("githubPullRequestController")
    public IntegrationController<GithubRepositoryQuery> githubPullRequestController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GithubClientFactory githubClientFactory,
            GithubOrganizationService githubOrganizationService,
            GithubRepositoryService githubRepositoryService) {

        GithubRepositoryDataSource githubPullRequestDataSource = ingestionEngine.add("GithubPullRequestDataSource",
                new GithubRepositoryDataSource(objectMapper, githubClientFactory, githubOrganizationService, githubRepositoryService,
                        EnumSet.of(PULL_REQUESTS)));

        return ingestionEngine.add("GithubPullRequestController", GithubControllers.pullRequestController()
                .objectMapper(objectMapper)
                .enrichedRepositoryDataSource(githubPullRequestDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("githubTagController")
    public IntegrationController<GithubRepositoryQuery> githubTagController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GithubClientFactory githubClientFactory,
            GithubOrganizationService githubOrganizationService,
            GithubRepositoryService githubRepositoryService) {

        GithubRepositoryDataSource githubTagDataSource = ingestionEngine.add("GithubTagDataSource",
                new GithubRepositoryDataSource(objectMapper, githubClientFactory, githubOrganizationService, githubRepositoryService,
                        EnumSet.of(TAGS)));

        return ingestionEngine.add("GithubTagController", GithubControllers.tagController()
                .objectMapper(objectMapper)
                .enrichedRepositoryDataSource(githubTagDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("githubIssueController")
    public IntegrationController<GithubRepositoryQuery> githubIssueController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GithubClientFactory githubClientFactory,
            GithubOrganizationService githubOrganizationService,
            GithubRepositoryService githubRepositoryService) {

        GithubRepositoryDataSource githubPullRequestDataSource = ingestionEngine.add("GithubIssueDataSource",
                new GithubRepositoryDataSource(objectMapper, githubClientFactory, githubOrganizationService, githubRepositoryService,
                        EnumSet.of(ISSUES, ISSUE_EVENTS)));

        return ingestionEngine.add("GithubIssueController", GithubControllers.issueController()
                .objectMapper(objectMapper)
                .enrichedRepositoryDataSource(githubPullRequestDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("githubProjectController")
    public IntegrationController<GithubProjectDataSource.GithubProjectQuery> githubProjectController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GithubClientFactory githubClientFactory,
            GithubOrganizationService githubOrganizationService) {

        GithubProjectDataSource githubProjectDataSource = ingestionEngine.add("GithubProjectDataSource",
                new GithubProjectDataSource(githubClientFactory, githubOrganizationService));

        return ingestionEngine.add("githubProjectController", GithubControllers.projectController()
                .objectMapper(objectMapper)
                .projectDataSource(githubProjectDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean
    public GithubIterativeScanController githubIterativeScanController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            @Qualifier("githubRepositoryController") IntegrationController<GithubRepositoryQuery> githubRepositoryController,
            @Qualifier("githubCommitController") IntegrationController<GithubRepositoryQuery> githubCommitController,
            @Qualifier("githubPullRequestController") IntegrationController<GithubRepositoryQuery> githubPullRequestController,
            @Qualifier("githubTagController") IntegrationController<GithubRepositoryQuery> githubTagController,
            @Qualifier("githubIssueController") IntegrationController<GithubRepositoryQuery> githubIssueController,
            @Qualifier("githubProjectController") IntegrationController<GithubProjectQuery> githubProjectController,
            @Qualifier("githubUserController") IntegrationController<GithubUserQuery> githubUserController,
            InventoryService inventoryService) {

        return ingestionEngine.add("GithubIterativeScanController", GithubIterativeScanController.builder()
                .objectMapper(objectMapper)
                .repositoryController(githubRepositoryController)
                .commitController(githubCommitController)
                .pullRequestController(githubPullRequestController)
                .tagController(githubTagController)
                .issueController(githubIssueController)
                .projectController(githubProjectController)
                .onboardingInDays(githubOnboardingInDays)
                .userController(githubUserController)
                .inventoryService(inventoryService)
                .build());
    }

    @Bean
    public GithubWebhookController githubCreateWebhookController(IngestionEngine ingestionEngine,
                                                                 ObjectMapper objectMapper,
                                                                 GithubClientFactory githubClientFactory) {
        return ingestionEngine.add("GithubWebhookController", GithubWebhookController.builder()
                .objectMapper(objectMapper)
                .clientFactory(githubClientFactory)
                .build());
    }

    @Bean
    public GithubPRController githubPRController(IngestionEngine ingestionEngine,
                                                 ObjectMapper objectMapper,
                                                 GithubClientFactory githubClientFactory) {
        GithubDataSource githubPullRequestDataSource = new GithubDataSource(githubClientFactory,
                GithubDataSource.FetchDataType.PULL_REQUESTS);
        return ingestionEngine.add("GithubPRController", GithubPRController.builder()
                .objectMapper(objectMapper)
                .githubDataSource(githubPullRequestDataSource)
                .build());
    }

    @Bean
    public GithubWebhookEnrichmentController githubWebhookEnrichmentController(IngestionEngine ingestionEngine,
                                                                               ObjectMapper objectMapper,
                                                                               StorageDataSink storageDataSink,
                                                                               GithubClientFactory githubClientFactory) {
        GithubWebhookEnrichDataSource githubWebhookEnrichDataSource = new GithubWebhookEnrichDataSource(githubClientFactory);
        return ingestionEngine.add("GithubWebhookEnrichmentController", GithubWebhookEnrichmentController.builder()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .githubWebhookEnrichDataSource(githubWebhookEnrichDataSource)
                .build());
    }

    @Bean("githubUserController")
    public IntegrationController<GithubUserQuery> githubIssueController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GithubOrganizationService githubOrganizationService,
            GithubUserService githubUserService) {

        GithubUserDataSource githubUserDataSource = ingestionEngine.add("GithubUserDataSource",
                new GithubUserDataSource(githubOrganizationService, githubUserService));

        return ingestionEngine.add("GithubUserController", GithubControllers.userController()
                .objectMapper(objectMapper)
                .githubUserDataSource(githubUserDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }
}
