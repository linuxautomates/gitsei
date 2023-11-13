package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.github.models.GithubProject;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubUser;
import io.levelops.integrations.github.sources.GithubProjectDataSource;
import io.levelops.integrations.github.sources.GithubProjectDataSource.GithubProjectQuery;
import io.levelops.integrations.github.sources.GithubRepositoryDataSource;
import io.levelops.integrations.github.sources.GithubUserDataSource;
import lombok.Builder;
import org.apache.commons.collections4.CollectionUtils;

import static io.levelops.integrations.github.sources.GithubRepositoryDataSource.GithubRepositoryQuery;

@SuppressWarnings("unused")
public class GithubControllers {

    @Builder(builderMethodName = "repositoryController", builderClassName = "GithubRepositoryControllerBuilder")
    private static IntegrationController<GithubRepositoryQuery> buildRepositoryController(
            GithubRepositoryDataSource repositoryDataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<GithubRepository, GithubRepositoryQuery>builder()
                .queryClass(GithubRepositoryQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GithubRepository, GithubRepositoryQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(repositoryDataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("github")
                        .dataType("repositories")
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "commitController", builderClassName = "GithubCommitControllerBuilder")
    private static IntegrationController<GithubRepositoryQuery> buildCommitController(
            GithubRepositoryDataSource enrichedRepositoryDataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<GithubRepository, GithubRepositoryQuery>builder()
                .queryClass(GithubRepositoryQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GithubRepository, GithubRepositoryQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(enrichedRepositoryDataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("github")
                        .dataType("commits")
                        .outputPageSize(1) // 1 repo per page
                        .skipEmptyResults(true)
                        .customEmptyPagePredicate(page -> page.stream()
                                .map(GithubRepository::getEvents)
                                .allMatch(CollectionUtils::isEmpty))
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "pullRequestController", builderClassName = "GithubPullRequestControllerBuilder")
    private static IntegrationController<GithubRepositoryQuery> buildPullRequestController(
            GithubRepositoryDataSource enrichedRepositoryDataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<GithubRepository, GithubRepositoryQuery>builder()
                .queryClass(GithubRepositoryQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GithubRepository, GithubRepositoryQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(enrichedRepositoryDataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("github")
                        .dataType("pull_requests")
                        .outputPageSize(1) // 1 repo per page
                        .skipEmptyResults(true)
                        .customEmptyPagePredicate(page -> page.stream()
                                .map(GithubRepository::getPullRequests)
                                .allMatch(CollectionUtils::isEmpty))
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "tagController", builderClassName = "GithubTagControllerBuilder")
    private static IntegrationController<GithubRepositoryQuery> buildTagController(
            GithubRepositoryDataSource enrichedRepositoryDataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<GithubRepository, GithubRepositoryQuery>builder()
                .queryClass(GithubRepositoryQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GithubRepository, GithubRepositoryQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(enrichedRepositoryDataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("github")
                        .dataType("tags")
                        .outputPageSize(1) // 1 repo per page
                        .skipEmptyResults(true)
                        .customEmptyPagePredicate(page -> page.stream()
                                .map(GithubRepository::getTags)
                                .allMatch(CollectionUtils::isEmpty))
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "issueController", builderClassName = "GithubIssueControllerBuilder")
    private static IntegrationController<GithubRepositoryQuery> buildIssueController(
            GithubRepositoryDataSource enrichedRepositoryDataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<GithubRepository, GithubRepositoryQuery>builder()
                .queryClass(GithubRepositoryQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GithubRepository, GithubRepositoryQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(enrichedRepositoryDataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("github")
                        .dataType("issues")
                        .outputPageSize(1) // 1 repo per page
                        .skipEmptyResults(true)
                        .customEmptyPagePredicate(page -> page.stream()
                                .map(GithubRepository::getIssues)
                                .allMatch(CollectionUtils::isEmpty))
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "projectController", builderClassName = "GithubProjectControllerBuilder")
    private static IntegrationController<GithubProjectQuery> buildProjectController(
            GithubProjectDataSource projectDataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<GithubProject, GithubProjectQuery>builder()
                .queryClass(GithubProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GithubProject, GithubProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(projectDataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("github")
                        .dataType("projects")
                        .uniqueOutputFiles(true) // because of partial data after retrying
                        .build())
                .build();
    }

    @Builder(builderMethodName = "userController", builderClassName = "GithubUserControllerBuilder")
    private static IntegrationController<GithubUserDataSource.GithubUserQuery> buildUserController(
            GithubUserDataSource githubUserDataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<GithubUser, GithubUserDataSource.GithubUserQuery>builder()
                .queryClass(GithubUserDataSource.GithubUserQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GithubUser, GithubUserDataSource.GithubUserQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(githubUserDataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("github")
                        .dataType("users")
                        .outputPageSize(100) // 100 users per page
                        .skipEmptyResults(true)
                        .uniqueOutputFiles(true)
                        .build())
                .build();
    }

}
