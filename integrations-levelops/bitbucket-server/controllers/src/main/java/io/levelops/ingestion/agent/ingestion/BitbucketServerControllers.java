package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerIterativeScanQuery;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerEnrichedProjectData;
import io.levelops.integrations.bitbucket_server.sources.BitbucketServerProjectDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class BitbucketServerControllers {

    private static final String PULL_REQUEST_DATATYPE = "pull_requests";
    private static final String TAG_DATATYPE = "tags";
    private static final String COMMITS_DATATYPE = "commits";
    private static final String REPOSITORIES_DATATYPE = "repositories";

    @Builder(builderMethodName = "commitController", builderClassName = "BitbucketServerCommitControllerBuilder")
    private static IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> buildCommitController(
            BitbucketServerProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<BitbucketServerEnrichedProjectData, BitbucketServerProjectDataSource.BitbucketServerProjectQuery>builder()
                .queryClass(BitbucketServerProjectDataSource.BitbucketServerProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<BitbucketServerEnrichedProjectData, BitbucketServerProjectDataSource.BitbucketServerProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(IntegrationType.BITBUCKET_SERVER.toString())
                        .dataType(COMMITS_DATATYPE)
                        .skipEmptyResults(true)
                        .build())
                .build();
    }

    @Builder(builderMethodName = "pullRequestController", builderClassName = "BitbucketServerPullRequestControllerBuilder")
    private static IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> buildPullRequestController(
            BitbucketServerProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<BitbucketServerEnrichedProjectData, BitbucketServerProjectDataSource.BitbucketServerProjectQuery>builder()
                .queryClass(BitbucketServerProjectDataSource.BitbucketServerProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<BitbucketServerEnrichedProjectData, BitbucketServerProjectDataSource.BitbucketServerProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(IntegrationType.BITBUCKET_SERVER.toString())
                        .dataType(PULL_REQUEST_DATATYPE)
                        .skipEmptyResults(true)
                        .build())
                .build();
    }

    @Builder(builderMethodName = "tagController", builderClassName = "BitbucketServerTagControllerBuilder")
    private static IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> buildTagController(
            BitbucketServerProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<BitbucketServerEnrichedProjectData, BitbucketServerProjectDataSource.BitbucketServerProjectQuery>builder()
                .queryClass(BitbucketServerProjectDataSource.BitbucketServerProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<BitbucketServerEnrichedProjectData, BitbucketServerProjectDataSource.BitbucketServerProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(IntegrationType.BITBUCKET_SERVER.toString())
                        .dataType(TAG_DATATYPE)
                        .skipEmptyResults(true)
                        .build())
                .build();
    }

    @Builder(builderMethodName = "repositoryController", builderClassName = "BitbucketServerRepositoryController")
    private static IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> buildRepositoryController(
            BitbucketServerProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<BitbucketServerEnrichedProjectData, BitbucketServerProjectDataSource.BitbucketServerProjectQuery>builder()
                .queryClass(BitbucketServerProjectDataSource.BitbucketServerProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<BitbucketServerEnrichedProjectData, BitbucketServerProjectDataSource.BitbucketServerProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(IntegrationType.BITBUCKET_SERVER.toString())
                        .dataType(REPOSITORIES_DATATYPE)
                        .build())
                .build();
    }

}
