package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.AzureDevopsController;
import io.levelops.ingestion.agent.ingestion.AzureDevopsIterativeScanController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.services.IngestionCachingService;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.azureDevops.client.AzureDevopsClientFactory;
import io.levelops.integrations.azureDevops.models.AzureDevopsIterativeScanQuery;
import io.levelops.integrations.azureDevops.sources.AzureDevopsProjectDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

@Configuration
public class AzureDevopsConfig {

    @Bean
    public AzureDevopsClientFactory azureDevopsClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                                             OkHttpClient okHttpClient,
                                                             @Value("${azure_devops_response_page_size:25}") int pageSize,
                                                             @Value("${azure_devops_throttling_interval_ms:200}") int throttlingIntervalMs) {
        return AzureDevopsClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .pageSize(pageSize)
                .throttlingIntervalMs(throttlingIntervalMs)
                .build();
    }

    @Bean("azureDevopsCommitController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsCommitController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsCommitsDataSource = ingestionEngine.add("AzureDevopsCommitsDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.COMMITS)));
        return ingestionEngine.add("AzureDevopsCommitController", AzureDevopsController.commitController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsCommitsDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsPullRequestController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsPullRequestController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsPullRequestDataSource = ingestionEngine.add("AzureDevopsPullRequestDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.PULL_REQUESTS)));
        return ingestionEngine.add("AzureDevopsPullRequestController", AzureDevopsController.pullRequestsController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsPullRequestDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsPipelineRunsController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsPipelineRunsController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsPipelineRunsDataSource = ingestionEngine.add("AzureDevopsPipelineRunsDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.PIPELINE_RUNS)));
        return ingestionEngine.add("AzureDevopsPipelineRunsController", AzureDevopsController.piplineRunsController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsPipelineRunsDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsBuildsController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsBuildsController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsBuildsDataSource = ingestionEngine.add("AzureDevopsBuildsDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.BUILDS)));
        return ingestionEngine.add("AzureDevopsBuildsController", AzureDevopsController.buildsController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsBuildsDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsReleasesController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsReleasesController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsReleasesDataSource = ingestionEngine.add("AzureDevopsReleasesDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.RELEASES)));
        return ingestionEngine.add("AzureDevopsReleasesController", AzureDevopsController.releasesController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsReleasesDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsWorkItemsController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsWorkItemsController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsWorkItemsDataSource = ingestionEngine.add("AzureDevopsWorkItemsDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.WORK_ITEMS)));
        return ingestionEngine.add("AzureDevopsWorkItemsController", AzureDevopsController.workItemsController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsWorkItemsDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsChangesetsController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsChangesetsController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsChangesetsDataSource = ingestionEngine.add("AzureDevopsChangesetsDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.CHANGESETS)));
        return ingestionEngine.add("AzureDevopsChangesetsController", AzureDevopsController.changesetsController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsChangesetsDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsMetadataController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsMetadataController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsMetadataDataSource = ingestionEngine.add("AzureDevopsMetadataDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.METADATA)));
        return ingestionEngine.add("AzureDevopsMetadataController", AzureDevopsController.metadataController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsMetadataDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsBranchesController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsBranchesController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsBranchesDataSource = ingestionEngine.add("AzureDevopsBranchesDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.BRANCHES)));
        return ingestionEngine.add("AzureDevopsBranchesController", AzureDevopsController.branchesController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsBranchesDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsIterationsController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsIterationsController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsIterationsDataSource = ingestionEngine.add("AzureDevopsIterationsDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.ITERATIONS)));
        return ingestionEngine.add("AzureDevopsIterationsController", AzureDevopsController.iterationsController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsIterationsDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsLabelsController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsLabelsController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsLabelsDataSource = ingestionEngine.add("AzureDevopsLabelsDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.LABELS)));
        return ingestionEngine.add("AzureDevopsLabelsController", AzureDevopsController.labelsController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsLabelsDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsWorkItemsHistoriesController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsWorkItemsHistoriesController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsWorkItemsHistoriesDataSource = ingestionEngine.add("AzureDevopsWorkItemsHistoriesDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.WORK_ITEMS_HISTORIES)));
        return ingestionEngine.add("AzureDevopsWorkItemsHistoriesController", AzureDevopsController.workItemsHistoriesController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsWorkItemsHistoriesDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsWorkItemsFieldsController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsWorkItemsFieldsController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsWorkItemsFieldsDataSource = ingestionEngine.add("AzureDevopsWorkItemsFieldsDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.WORK_ITEMS_FIELDS)));
        return ingestionEngine.add("AzureDevopsWorkItemsFIeldsController", AzureDevopsController.workItemsFieldsController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsWorkItemsFieldsDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsTeamsController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsTeamsController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsFetchTeamsDataSource = ingestionEngine.add("AzureDevopsFetchTeamsDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.TEAMS)));
        return ingestionEngine.add("AzureDevopsTeamsController", AzureDevopsController.teamsController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsFetchTeamsDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsTagsController")
    public IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsTagsController(
            IngestionCachingService ingestionCachingService,
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            AzureDevopsClientFactory azureDevopsClientFactory) {
        AzureDevopsProjectDataSource azureDevopsFetchTagsDataSource = ingestionEngine.add("AzureDevopsFetchTagsDataSource",
                new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(AzureDevopsProjectDataSource.Enrichment.TAGS)));
        return ingestionEngine.add("AzureDevopsTagsController", AzureDevopsController.tagsController()
                .objectMapper(objectMapper)
                .dataSource(azureDevopsFetchTagsDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("azureDevopsIterativeScanControllerController")
    public AzureDevopsIterativeScanController azureDevopsIterativeScanControllerController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            InventoryService inventoryService,
            @Qualifier("azureDevopsCommitController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsCommitController,
            @Qualifier("azureDevopsPullRequestController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsPullRequestController,
            @Qualifier("azureDevopsPipelineRunsController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsPipelineRunsController,
            @Qualifier("azureDevopsReleasesController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsReleasesController,
            @Qualifier("azureDevopsBuildsController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsBuildsController,
            @Qualifier("azureDevopsWorkItemsController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsWorkItemsController,
            @Qualifier("azureDevopsWorkItemsHistoriesController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsWorkItemsHistoriesController,
            @Qualifier("azureDevopsWorkItemsFieldsController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsWorkItemsFieldsController,
            @Qualifier("azureDevopsChangesetsController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsChangesetsController,
            @Qualifier("azureDevopsMetadataController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsMetadataController,
            @Qualifier("azureDevopsBranchesController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsBranchesController,
            @Qualifier("azureDevopsLabelsController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsLabelsController,
            @Qualifier("azureDevopsIterationsController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsIterationsController,
            @Qualifier("azureDevopsTeamsController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsTeamsController,
            @Qualifier("azureDevopsTagsController") IntegrationController<AzureDevopsIterativeScanQuery> azureDevopsTagsController) {
        return ingestionEngine.add("AzureDevopsIterativeScanController", AzureDevopsIterativeScanController.builder()
                .objectMapper(objectMapper)
                .inventoryService(inventoryService)
                .commitController(azureDevopsCommitController)
                .pullRequestController(azureDevopsPullRequestController)
                .pipelineController(azureDevopsPipelineRunsController)
                .releaseController(azureDevopsReleasesController)
                .buildsController(azureDevopsBuildsController)
                .workItemsController(azureDevopsWorkItemsController)
                .changesetsController(azureDevopsChangesetsController)
                .metadataController(azureDevopsMetadataController)
                .branchesController(azureDevopsBranchesController)
                .labelsController(azureDevopsLabelsController)
                .workItemsHistoriesController(azureDevopsWorkItemsHistoriesController)
                .workItemsFieldsController(azureDevopsWorkItemsFieldsController)
                .iterationsController(azureDevopsIterationsController)
                .teamsController(azureDevopsTeamsController)
                .tagsController(azureDevopsTagsController)
                .onboardingInDays(90)
                .build());
    }
}
