package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.BitbucketControllers;
import io.levelops.ingestion.agent.ingestion.BitbucketIterativeScanController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.bitbucket.client.BitbucketClientFactory;
import io.levelops.integrations.bitbucket.sources.BitbucketRepositoryDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

import static io.levelops.integrations.bitbucket.sources.BitbucketRepositoryDataSource.Enrichment.COMMITS;
import static io.levelops.integrations.bitbucket.sources.BitbucketRepositoryDataSource.Enrichment.PULL_REQUESTS;
import static io.levelops.integrations.bitbucket.sources.BitbucketRepositoryDataSource.Enrichment.TAGS;

@Configuration
public class BitbucketConfig {

    @Value("${bitbucket.onboarding_in_days:7}")
    private Integer bitbucketOnboardingInDays;

    @Bean
    public BitbucketClientFactory bitbucketClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        return new BitbucketClientFactory(inventoryService, objectMapper, okHttpClient);
    }

    @Bean("bitbucketRepositoryController")
    public IntegrationController<BitbucketRepositoryDataSource.BitbucketRepositoryQuery> bitbucketRepositoryController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            BitbucketClientFactory bitbucketClientFactory) {

        BitbucketRepositoryDataSource repositoryDataSource = ingestionEngine.add("BitbucketRepositoryDataSource",
                new BitbucketRepositoryDataSource(bitbucketClientFactory));

        return ingestionEngine.add("BitbucketRepositoryController", BitbucketControllers.repositoryController()
                .objectMapper(objectMapper)
                .dataSource(repositoryDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("bitbucketCommitController")
    public IntegrationController<BitbucketRepositoryDataSource.BitbucketRepositoryQuery> bitbucketCommitController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            BitbucketClientFactory bitbucketClientFactory) {

        BitbucketRepositoryDataSource bitbucketCommitDataSource = ingestionEngine.add("BitbucketCommitDataSource",
                new BitbucketRepositoryDataSource(bitbucketClientFactory, EnumSet.of(COMMITS)));

        return ingestionEngine.add("BitbucketCommitController", BitbucketControllers.commitController()
                .objectMapper(objectMapper)
                .dataSource(bitbucketCommitDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("bitbucketPullRequestController")
    public IntegrationController<BitbucketRepositoryDataSource.BitbucketRepositoryQuery> bitbucketPullRequestController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            BitbucketClientFactory bitbucketClientFactory) {

        BitbucketRepositoryDataSource bitbucketPullRequestDataSource = ingestionEngine.add("BitbucketPullRequestDataSource",
                new BitbucketRepositoryDataSource(bitbucketClientFactory, EnumSet.of(PULL_REQUESTS)));

        return ingestionEngine.add("BitbucketPullRequestController", BitbucketControllers.pullRequestController()
                .objectMapper(objectMapper)
                .dataSource(bitbucketPullRequestDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("bitbucketTagController")
    public IntegrationController<BitbucketRepositoryDataSource.BitbucketRepositoryQuery> bitbucketTagController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            BitbucketClientFactory bitbucketClientFactory) {

        BitbucketRepositoryDataSource bitbucketTagDataSource = ingestionEngine.add("BitbucketTagDataSource",
                new BitbucketRepositoryDataSource(bitbucketClientFactory, EnumSet.of(TAGS)));

        return ingestionEngine.add("BitbucketTagController", BitbucketControllers.tagController()
                .objectMapper(objectMapper)
                .dataSource(bitbucketTagDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean
    public BitbucketIterativeScanController bitbucketIterativeScanController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            @Qualifier("bitbucketRepositoryController") IntegrationController<BitbucketRepositoryDataSource.BitbucketRepositoryQuery> bitbucketRepositoryController,
            @Qualifier("bitbucketCommitController") IntegrationController<BitbucketRepositoryDataSource.BitbucketRepositoryQuery> bitbucketCommitController,
            @Qualifier("bitbucketPullRequestController") IntegrationController<BitbucketRepositoryDataSource.BitbucketRepositoryQuery> bitbucketPullRequestController,
            @Qualifier("bitbucketTagController") IntegrationController<BitbucketRepositoryDataSource.BitbucketRepositoryQuery> bitbucketTagController,
            InventoryService inventoryService) {

        return ingestionEngine.add("BitbucketIterativeScanController", BitbucketIterativeScanController.builder()
                .objectMapper(objectMapper)
                .repositoryController(bitbucketRepositoryController)
                .commitController(bitbucketCommitController)
                .tagController(bitbucketTagController)
                .pullRequestController(bitbucketPullRequestController)
                .onboardingInDays(bitbucketOnboardingInDays)
                .inventoryService(inventoryService)
                .build());
    }

}