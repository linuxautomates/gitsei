package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.gerrit.models.ProjectInfo;
import io.levelops.integrations.gerrit.sources.GerritRepositoryDataSource;
import lombok.Builder;
import org.apache.commons.collections4.CollectionUtils;

@SuppressWarnings("unused")
public class GerritControllers {

    @Builder(builderMethodName = "repositoryController", builderClassName = "GerritRepositoryControllerBuilder")
    private static IntegrationController<GerritRepositoryDataSource.GerritRepositoryQuery> buildRepositoryController(
            GerritRepositoryDataSource repositoryDataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<ProjectInfo, GerritRepositoryDataSource.GerritRepositoryQuery>builder()
                .queryClass(GerritRepositoryDataSource.GerritRepositoryQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<ProjectInfo, GerritRepositoryDataSource.GerritRepositoryQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(repositoryDataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("gerrit")
                        .dataType("repositories")
                        .build())
                .build();
    }

    @Builder(builderMethodName = "pullRequestController", builderClassName = "GerritPullRequestControllerBuilder")
    private static IntegrationController<GerritRepositoryDataSource.GerritRepositoryQuery> buildPullRequestController(
            GerritRepositoryDataSource repositoryDataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<ProjectInfo, GerritRepositoryDataSource.GerritRepositoryQuery>builder()
                .queryClass(GerritRepositoryDataSource.GerritRepositoryQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<ProjectInfo, GerritRepositoryDataSource.GerritRepositoryQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(repositoryDataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("gerrit")
                        .dataType("pull_requests")
                        .outputPageSize(1) // 1 repo per page
                        .skipEmptyResults(true)
                        .customEmptyPagePredicate(page -> page.stream()
                                .map(ProjectInfo::getChanges)
                                .allMatch(CollectionUtils::isEmpty))
                        .build())
                .build();
    }

}
