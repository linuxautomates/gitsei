package io.levelops.integrations.azureDevops.sources;

import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.services.IngestionCachingService;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.azureDevops.client.AzureDevopsClient;
import io.levelops.integrations.azureDevops.client.AzureDevopsClientFactory;
import io.levelops.integrations.azureDevops.models.AzureDevopsIntermediateState;
import io.levelops.integrations.azureDevops.models.AzureDevopsIterativeScanQuery;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchBranchService;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchBuildsService;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchChangeSetsService;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchCommitsService;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchIterationsService;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchLabelsService;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchPipelineService;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchPullRequestService;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchReleaseService;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchTagService;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchTeamsService;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchWorkItemFieldsService;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchWorkItemHistoriesService;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchWorkItemMetadataService;
import io.levelops.integrations.azureDevops.services.AzureDevopsFetchWorkItemsService;
import lombok.extern.log4j.Log4j2;

import java.util.EnumSet;
import java.util.stream.Stream;

@Log4j2
public class AzureDevopsProjectDataSource implements DataSource<EnrichedProjectData, AzureDevopsIterativeScanQuery> {

    private final AzureDevopsClientFactory azureDevopsClientFactory;
    private final IngestionCachingService ingestionCachingService;
    private final AzureDevopsFetchCommitsService azureDevopsFetchCommitsService;
    private final AzureDevopsFetchPullRequestService azureDevopsFetchPullRequestService;
    private final AzureDevopsFetchPipelineService azureDevopsFetchPipelineService;
    private final AzureDevopsFetchBuildsService azureDevopsFetchBuildsService;
    private final AzureDevopsFetchReleaseService azureDevopsFetchReleaseService;
    private final AzureDevopsFetchIterationsService azureDevopsFetchIterationsService;
    private final AzureDevopsFetchWorkItemsService azureDevopsFetchWorkItemsService;
    private final AzureDevopsFetchChangeSetsService azureDevopsFetchChangeSetsService;
    private final AzureDevopsFetchWorkItemMetadataService azureDevopsFetchMetadataService;
    private final AzureDevopsFetchBranchService azureDevopsFetchBranchService;
    private final AzureDevopsFetchLabelsService azureDevopsFetchLabelsService;
    private final AzureDevopsFetchWorkItemHistoriesService azureDevopsFetchWorkItemHistoriesService;
    private final AzureDevopsFetchWorkItemFieldsService azureDevopsFetchWorkItemFieldsService;
    private final AzureDevopsFetchTeamsService azureDevopsFetchTeamsService;
    private final AzureDevopsFetchTagService azureDevopsFetchTagsService;
    private final EnumSet<Enrichment> enrichments;

    public enum Enrichment {
        COMMITS, PULL_REQUESTS, PIPELINE_RUNS, BUILDS, RELEASES,
        WORK_ITEMS, CHANGESETS, METADATA, BRANCHES, LABELS,
        WORK_ITEMS_HISTORIES, WORK_ITEMS_FIELDS, ITERATIONS,
        TEAMS, TAGS;
    }

    public AzureDevopsProjectDataSource(AzureDevopsClientFactory azureDevopsClientFactory,
                                        IngestionCachingService ingestionCachingService,
                                        EnumSet<Enrichment> enrichments) {
        this.azureDevopsClientFactory = azureDevopsClientFactory;
        this.ingestionCachingService = ingestionCachingService;
        this.azureDevopsFetchWorkItemFieldsService = new AzureDevopsFetchWorkItemFieldsService();
        this.azureDevopsFetchCommitsService = new AzureDevopsFetchCommitsService();
        this.azureDevopsFetchPullRequestService = new AzureDevopsFetchPullRequestService();
        this.azureDevopsFetchPipelineService = new AzureDevopsFetchPipelineService();
        this.azureDevopsFetchBuildsService = new AzureDevopsFetchBuildsService();
        this.azureDevopsFetchReleaseService = new AzureDevopsFetchReleaseService();
        this.azureDevopsFetchIterationsService = new AzureDevopsFetchIterationsService();
        this.azureDevopsFetchWorkItemsService = new AzureDevopsFetchWorkItemsService();
        this.azureDevopsFetchWorkItemHistoriesService = new AzureDevopsFetchWorkItemHistoriesService();
        this.azureDevopsFetchChangeSetsService = new AzureDevopsFetchChangeSetsService();
        this.azureDevopsFetchMetadataService = new AzureDevopsFetchWorkItemMetadataService();
        this.azureDevopsFetchBranchService = new AzureDevopsFetchBranchService();
        this.azureDevopsFetchLabelsService = new AzureDevopsFetchLabelsService();
        this.azureDevopsFetchTeamsService = new AzureDevopsFetchTeamsService();
        this.azureDevopsFetchTagsService = new AzureDevopsFetchTagService();
        this.enrichments = enrichments;
    }

    @Override
    public Data<EnrichedProjectData> fetchOne(AzureDevopsIterativeScanQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<EnrichedProjectData>> fetchMany(AzureDevopsIterativeScanQuery query) {
        throw new UnsupportedOperationException("This should never be reached");
    }

    @Override
    public Stream<Data<EnrichedProjectData>> fetchMany(JobContext jobContext, AzureDevopsIterativeScanQuery query) throws FetchException {
        AzureDevopsClient azureDevopsClient = azureDevopsClientFactory.get(query.getIntegrationKey());
        AzureDevopsIntermediateState intermediateState = AzureDevopsIntermediateState.parseIntermediateState(jobContext.getIntermediateState());
        log.info("Intermediate state project : {}, org : {}", intermediateState.getResumeFromProject(), intermediateState.getResumeFromOrganization());
        Stream<EnrichedProjectData> dataStream;
        if (enrichments.contains(Enrichment.COMMITS) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.COMMITS)) {
            dataStream = azureDevopsFetchCommitsService.fetchCommits(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else if (enrichments.contains(Enrichment.PULL_REQUESTS) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.PRS)) {
            dataStream = azureDevopsFetchPullRequestService.fetchPullRequests(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else if (enrichments.contains(Enrichment.PIPELINE_RUNS) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.PIPELINES)) {
            dataStream = azureDevopsFetchPipelineService.fetchPipelines(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else if (enrichments.contains(Enrichment.BUILDS) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.BUILDS)) {
            dataStream = azureDevopsFetchBuildsService.fetchBuilds(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else if(enrichments.contains(Enrichment.RELEASES) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.RELEASES)) {
            dataStream = azureDevopsFetchReleaseService.fetchReleases(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else if (enrichments.contains(Enrichment.ITERATIONS) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.ITERATIONS)) {
            dataStream = azureDevopsFetchIterationsService.fetchIterations(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else if (enrichments.contains(Enrichment.WORK_ITEMS) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.WORKITEMS)) {
            dataStream = azureDevopsFetchWorkItemsService.fetchWorkItems(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else if (enrichments.contains(Enrichment.CHANGESETS) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.CHANGESETS)) {
            dataStream = azureDevopsFetchChangeSetsService.fetchChangesets(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else if (enrichments.contains(Enrichment.METADATA) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.METADATA)) {
            dataStream = azureDevopsFetchMetadataService.fetchWorkItemsMetadata(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else if (enrichments.contains(Enrichment.LABELS) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.LABELS)) {
            dataStream = azureDevopsFetchLabelsService.fetchTfvcLabels(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else if (enrichments.contains(Enrichment.BRANCHES) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.BRANCHES)) {
            dataStream = azureDevopsFetchBranchService.fetchTfvcBranches(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else if (enrichments.contains(Enrichment.WORK_ITEMS_HISTORIES) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.WORKITEM_HISTORIES)) {
            dataStream = azureDevopsFetchWorkItemHistoriesService.fetchWorkItemHistories(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else if (enrichments.contains(Enrichment.WORK_ITEMS_FIELDS) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.WORKITEM_FIELDS)) {
            dataStream = azureDevopsFetchWorkItemFieldsService.fetchWorkItemsFields(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else if (enrichments.contains(Enrichment.TEAMS) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.TEAMS)) {
            dataStream = azureDevopsFetchTeamsService.fetchTeams(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else if (enrichments.contains(Enrichment.TAGS) && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.TAGS)) {
            dataStream = azureDevopsFetchTagsService.fetchTags(ingestionCachingService, azureDevopsClient, query, intermediateState);
        } else {
            dataStream = Stream.empty();
        }
        return dataStream
                .map(BasicData.mapper(EnrichedProjectData.class));
    }


    @Override
    public String getComponentType() {
        return DataSource.super.getComponentType();
    }

    @Override
    public String getComponentClass() {
        return DataSource.super.getComponentClass();
    }
}
