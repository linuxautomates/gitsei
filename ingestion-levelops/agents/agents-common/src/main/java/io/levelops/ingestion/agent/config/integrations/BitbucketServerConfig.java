package io.levelops.ingestion.agent.config.integrations;

import java.util.EnumSet;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.BitbucketServerControllers;
import io.levelops.ingestion.agent.ingestion.BitbucketServerIterativeScanController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientFactory;
import io.levelops.integrations.bitbucket_server.sources.BitbucketServerProjectDataSource;
import okhttp3.OkHttpClient;

@Configuration
public class BitbucketServerConfig {

    @Value("${bitbucket-server.onboarding_in_days:7}")
    private Integer bitbucketServerOnboardingInDays;

    @Bean
    public BitbucketServerClientFactory bitbucketServerClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                                                     OkHttpClient okHttpClient,
                                                                     @Value("${bitbucket_server_response_page_size:200}") int pageSize,
                                                                     @Qualifier("allowUnsafeSSLBitbucketServer") Boolean allowUnsafeSSL) {
        return BitbucketServerClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .allowUnsafeSSL(allowUnsafeSSL)
                .pageSize(pageSize)
                .build();
    }

    @Bean("bitbucketServerRepositoryController")
    public IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> bitbucketRepositoryController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            BitbucketServerClientFactory bitbucketServerClientFactory) {

        BitbucketServerProjectDataSource repositoryDataSource = ingestionEngine.add("BitbucketServerProjectDataSource",
                new BitbucketServerProjectDataSource(bitbucketServerClientFactory, EnumSet.of(BitbucketServerProjectDataSource.Enrichment.REPOSITORIES)));

        return ingestionEngine.add("BitbucketServerRepositoryController", BitbucketServerControllers.repositoryController()
                .objectMapper(objectMapper)
                .dataSource(repositoryDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("bitbucketServerCommitController")
    public IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> bitbucketCommitServerController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            BitbucketServerClientFactory bitbucketServerClientFactory) {

        BitbucketServerProjectDataSource bitbucketServerCommitDataSource = ingestionEngine.add("BitbucketServerCommitDataSource",
                new BitbucketServerProjectDataSource(bitbucketServerClientFactory, EnumSet.of(BitbucketServerProjectDataSource.Enrichment.COMMITS)));

        return ingestionEngine.add("BitbucketServerCommitController", BitbucketServerControllers.commitController()
                .objectMapper(objectMapper)
                .dataSource(bitbucketServerCommitDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("bitbucketServerPullRequestController")
    public IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> bitbucketPullRequestController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            BitbucketServerClientFactory bitbucketServerClientFactory) {

        BitbucketServerProjectDataSource bitbucketServerPullRequestDataSource = ingestionEngine.add("BitbucketServerPullRequestDataSource",
                new BitbucketServerProjectDataSource(bitbucketServerClientFactory, EnumSet.of(BitbucketServerProjectDataSource.Enrichment.PULL_REQUESTS)));

        return ingestionEngine.add("BitbucketServerPullRequestController", BitbucketServerControllers.pullRequestController()
                .objectMapper(objectMapper)
                .dataSource(bitbucketServerPullRequestDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("bitbucketServerTagController")
    public IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> bitbucketServerTagController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            BitbucketServerClientFactory bitbucketServerClientFactory) {

        BitbucketServerProjectDataSource bitbucketServerTagDataSource = ingestionEngine.add("BitbucketServerTagDataSource",
                new BitbucketServerProjectDataSource(bitbucketServerClientFactory, EnumSet.of(BitbucketServerProjectDataSource.Enrichment.TAGS)));

        return ingestionEngine.add("BitbucketServerTagController", BitbucketServerControllers.tagController()
                .objectMapper(objectMapper)
                .dataSource(bitbucketServerTagDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean
    public BitbucketServerIterativeScanController bitbucketServerIterativeScanController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            @Qualifier("bitbucketServerRepositoryController") IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> bitbucketServerRepositoryController,
            @Qualifier("bitbucketServerCommitController") IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> bitbucketServerCommitController,
            @Qualifier("bitbucketServerPullRequestController") IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> bitbucketServerPullRequestController,
            @Qualifier("bitbucketServerTagController") IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> bitbucketServerTagController,
            InventoryService inventoryService) {

        return ingestionEngine.add("BitbucketServerIterativeScanController", BitbucketServerIterativeScanController.builder()
                .objectMapper(objectMapper)
                .repositoryController(bitbucketServerRepositoryController)
                .commitController(bitbucketServerCommitController)
                .pullRequestController(bitbucketServerPullRequestController)
                .tagController(bitbucketServerTagController)
                .onboardingInDays(bitbucketServerOnboardingInDays)
                .inventoryService(inventoryService)
                .build());
    }
}
