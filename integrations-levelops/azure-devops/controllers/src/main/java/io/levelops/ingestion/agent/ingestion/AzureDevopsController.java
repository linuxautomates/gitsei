package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.azureDevops.models.AzureDevopsIterativeScanQuery;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.sources.AzureDevopsProjectDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AzureDevopsController {

    private static final String PIPELINES_DATATYPE = "pipelines";
    private static final String COMMITS_DATATYPE = "commits";
    private static final String PULL_REQUESTS_DATATYPE = "pullrequests";
    private static final String BUILDS_DATATYPE = "builds";
    private static final String RELEASES_DATATYPE = "releases";
    private static final String WORK_ITEMS_DATATYPE = "workitems";
    private static final String CHANGESETS_DATATYPE = "changesets";
    private static final String METADATA_DATATYPE = "metadata";
    private static final String BRANCHES = "tfvc-branches";
    private static final String LABELS = "tfvc-labels";
    private static final String ITERATIONS = "iterations";
    private static final String WORK_ITEMS_HISTORIES_DATATYPE = "workitemshistories";
    private static final String WORK_ITEMS_FIELDS_DATATYPE = "workitemsfields";
    private static final String TEAMS = "teams";
    private static final String TAGS = "tags";
    private static final String INTEGRATION_TYPE = "azuredevops";

    @Builder(builderMethodName = "commitController", builderClassName = "AzureDevopsCommitsControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildCommitController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(COMMITS_DATATYPE)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .outputPageSize(9) //storing 9 batches, each having max 100 records to keep the file size under limit.
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "pullRequestsController", builderClassName = "AzureDevopsPullRequestsControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildPullRequestController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(PULL_REQUESTS_DATATYPE)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .outputPageSize(5) //storing 5 batches, each having max 100 records to keep the file size under limit.
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "piplineRunsController", builderClassName = "AzureDevopsPiplineRunsControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildPipelineRunsController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(PIPELINES_DATATYPE)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .outputPageSize(1) //storing 1 batches, each having max 5 records to keep the file size under limit.
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }
    @Builder(builderMethodName = "buildsController", builderClassName = "AzureDevopsBuildsControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildBuildsController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(BUILDS_DATATYPE)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .outputPageSize(6) //storing 6 batches, each having max 100 records to keep the file size under limit.
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "releasesController", builderClassName = "AzureDevopsReleasesControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildReleasesController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(RELEASES_DATATYPE)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .outputPageSize(1) //storing 1 batches, each having max 5 records to keep the file size under limit.
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "workItemsController", builderClassName = "AzureDevopsWorkItemsControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildWorkItemsController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(WORK_ITEMS_DATATYPE)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .outputPageSize(2) // storing 2 batches, each having max 150 records to keep the file size under limit.
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "iterationsController", builderClassName = "AzureDevopsIterationsControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildIterationsController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(ITERATIONS)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .outputPageSize(2) // storing 2 batches, each having max 500 records to keep the file size under limit.
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "changesetsController", builderClassName = "AzureDevopsChangesetsControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildChangesetsController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(CHANGESETS_DATATYPE)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .outputPageSize(20) // storing 20 batches, each having max 100 records to keep the file size under limit.
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "metadataController", builderClassName = "AzureDevopsMetadataControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildMetadataController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(METADATA_DATATYPE)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "branchesController", builderClassName = "AzureDevopsBranchesControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildBranchesController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(BRANCHES)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .outputPageSize(10) // storing 10 batches, each having max 100 records to keep the file size under limit.
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "labelsController", builderClassName = "AzureDevopslabelsControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildLabelsController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(LABELS)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .outputPageSize(9) // storing 9 batches, each having max 100 records to keep the file size under limit.
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "workItemsHistoriesController", builderClassName = "AzureDevopsWorkItemsHistoriesControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildWorkItemsHistoriesController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(WORK_ITEMS_HISTORIES_DATATYPE)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .outputPageSize(3) // storing 3 batches, each having max 600 records to keep the file size under limit.
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "workItemsFieldsController", builderClassName = "AzureDevopsWorkItemsFieldsControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildWorkItemsFieldsController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(WORK_ITEMS_FIELDS_DATATYPE)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .outputPageSize(10) // storing 10 batches, each having max 100 records to keep the file size under limit.
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "teamsController", builderClassName = "AzureDevopsTeamsControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildTeamsController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(TEAMS)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .outputPageSize(15) // storing 15 batches, each having max 100 records to keep the file size under limit.
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "tagsController", builderClassName = "AzureDevopsTagsControllerBuilder")
    private static IntegrationController<AzureDevopsIterativeScanQuery> buildTagsController(
            AzureDevopsProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                .queryClass(AzureDevopsIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, AzureDevopsIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(TAGS)
                        .integrationType(INTEGRATION_TYPE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .outputPageSize(15) // storing 15 batches, each having max 100 records to keep the file size under limit.
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }
}