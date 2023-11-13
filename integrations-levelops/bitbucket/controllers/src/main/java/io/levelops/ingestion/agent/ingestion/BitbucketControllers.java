package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.bitbucket.models.BitbucketRepository;
import io.levelops.integrations.bitbucket.sources.BitbucketRepositoryDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

@Log4j2
public class BitbucketControllers {

    @Builder(builderMethodName = "commitController", builderClassName = "BitbucketCommitControllerBuilder")
    private static IntegrationController<BitbucketRepositoryDataSource.BitbucketRepositoryQuery> buildCommitController(
            BitbucketRepositoryDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<BitbucketRepository, BitbucketRepositoryDataSource.BitbucketRepositoryQuery>builder()
                .queryClass(BitbucketRepositoryDataSource.BitbucketRepositoryQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<BitbucketRepository, BitbucketRepositoryDataSource.BitbucketRepositoryQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("bitbucket")
                        .dataType("commits")
                        .outputPageSize(1) // 1 repo per page
                        .skipEmptyResults(true)
                        .customEmptyPagePredicate(page -> page.stream()
                                .map(BitbucketRepository::getCommits)
                                .allMatch(CollectionUtils::isEmpty))
                        .build())
                .build();
    }

    @Builder(builderMethodName = "pullRequestController", builderClassName = "BitbucketPullRequestControllerBuilder")
    private static IntegrationController<BitbucketRepositoryDataSource.BitbucketRepositoryQuery> buildPullRequestController(
            BitbucketRepositoryDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<BitbucketRepository, BitbucketRepositoryDataSource.BitbucketRepositoryQuery>builder()
                .queryClass(BitbucketRepositoryDataSource.BitbucketRepositoryQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<BitbucketRepository, BitbucketRepositoryDataSource.BitbucketRepositoryQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("bitbucket")
                        .dataType("pull_requests")
                        .outputPageSize(1) // 1 repo per page
                        .skipEmptyResults(true)
                        .customEmptyPagePredicate(page -> page.stream()
                                .map(BitbucketRepository::getPullRequests)
                                .allMatch(CollectionUtils::isEmpty))
                        .build())
                .build();
    }

    @Builder(builderMethodName = "tagController", builderClassName = "BitbucketTagControllerBuilder")
    private static IntegrationController<BitbucketRepositoryDataSource.BitbucketRepositoryQuery> buildTagController(
            BitbucketRepositoryDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<BitbucketRepository, BitbucketRepositoryDataSource.BitbucketRepositoryQuery>builder()
                .queryClass(BitbucketRepositoryDataSource.BitbucketRepositoryQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<BitbucketRepository, BitbucketRepositoryDataSource.BitbucketRepositoryQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("bitbucket")
                        .dataType("tags")
                        .outputPageSize(1) // 1 repo per page
                        .skipEmptyResults(true)
                        .customEmptyPagePredicate(page -> page.stream()
                                .map(BitbucketRepository::getTags)
                                .allMatch(CollectionUtils::isEmpty))
                        .build())
                .build();
    }

    @Builder(builderMethodName = "repositoryController", builderClassName = "BitbucketRepositoryController")
    private static IntegrationController<BitbucketRepositoryDataSource.BitbucketRepositoryQuery> buildRepositoryController(
            BitbucketRepositoryDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<BitbucketRepository, BitbucketRepositoryDataSource.BitbucketRepositoryQuery>builder()
                .queryClass(BitbucketRepositoryDataSource.BitbucketRepositoryQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<BitbucketRepository, BitbucketRepositoryDataSource.BitbucketRepositoryQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("bitbucket")
                        .dataType("repositories")
                        .build())
                .build();
    }
}
