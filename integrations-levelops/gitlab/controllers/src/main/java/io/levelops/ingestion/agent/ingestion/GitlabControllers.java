package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.gitlab.models.GitlabGroup;
import io.levelops.integrations.gitlab.models.GitlabIssue;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.levelops.integrations.gitlab.models.GitlabQuery;
import io.levelops.integrations.gitlab.models.GitlabUser;
import io.levelops.integrations.gitlab.sources.GitlabGroupDataSource;
import io.levelops.integrations.gitlab.sources.GitlabIssueDataSource;
import io.levelops.integrations.gitlab.sources.GitlabProjectDataSource;
import io.levelops.integrations.gitlab.sources.GitlabProjectPipelineDataSource;
import io.levelops.integrations.gitlab.sources.GitlabUsersDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

/**
 * Gitlab's implementation of the {@link DataController}
 */
@Log4j2
public class GitlabControllers {

    private static final String GITLAB = "gitlab";
    private static final String COMMITS = "commits";
    private static final String TAGS = "tags";
    private static final String MERGE_REQUESTS = "merge_requests";
    private static final String PROJECTS = "projects";
    private static final String GROUPS = "groups";
    private static final String PIPELINES = "pipelines";
    private static final String BRANCHES = "branches";
    private static final String USERS = "users";
    private static final String MILESTONES = "milestones";
    private static final String ISSUES = "issues";

    @Builder(builderMethodName = "commitController", builderClassName = "GitlabCommitControllerBuilder")
    private static IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> buildCommitController(
            GitlabProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<GitlabProject, GitlabProjectDataSource.GitlabProjectQuery>builder()
                .queryClass(GitlabProjectDataSource.GitlabProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GitlabProject, GitlabProjectDataSource.GitlabProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(GITLAB)
                        .dataType(COMMITS)
                        .outputPageSize(1) // 1 file per project
                        .build())
                .build();
    }

    @Builder(builderMethodName = "tagController", builderClassName = "GitlabTagControllerBuilder")
    private static IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> buildTagController(
            GitlabProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<GitlabProject, GitlabProjectDataSource.GitlabProjectQuery>builder()
                .queryClass(GitlabProjectDataSource.GitlabProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GitlabProject, GitlabProjectDataSource.GitlabProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(GITLAB)
                        .dataType(TAGS)
                        .outputPageSize(1) // 1 file per project
                        .build())
                .build();
    }


    @Builder(builderMethodName = "mergeRequestController", builderClassName = "GitlabMergeRequestControllerBuilder")
    private static IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> buildMergeRequestController(
            GitlabProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<GitlabProject, GitlabProjectDataSource.GitlabProjectQuery>builder()
                .queryClass(GitlabProjectDataSource.GitlabProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GitlabProject, GitlabProjectDataSource.GitlabProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(GITLAB)
                        .dataType(MERGE_REQUESTS)
                        .outputPageSize(1) // 1 file per project
                        .build())
                .build();
    }

    @Builder(builderMethodName = "branchController", builderClassName = "GitlabBranchControllerBuilder")
    private static IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> buildBranchController(
            GitlabProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<GitlabProject, GitlabProjectDataSource.GitlabProjectQuery>builder()
                .queryClass(GitlabProjectDataSource.GitlabProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GitlabProject, GitlabProjectDataSource.GitlabProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(GITLAB)
                        .dataType(BRANCHES)
                        .outputPageSize(1) // 1 file per project
                        .build())
                .build();
    }

    @Builder(builderMethodName = "usersController", builderClassName = "GitlabUserControllerBuilder")
    private static IntegrationController<GitlabUsersDataSource.GitlabUserQuery> buildUserController(
            GitlabUsersDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<GitlabUser, GitlabUsersDataSource.GitlabUserQuery>builder()
                .queryClass(GitlabUsersDataSource.GitlabUserQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GitlabUser, GitlabUsersDataSource.GitlabUserQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(GITLAB)
                        .dataType(USERS)
                        .outputPageSize(500) // 500 users per file
                        .build())
                .build();
    }

    @Builder(builderMethodName = "milestonesController", builderClassName = "GitlabMilestoneControllerBuilder")
    private static IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> buildMilestonesController(
            GitlabProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<GitlabProject, GitlabProjectDataSource.GitlabProjectQuery>builder()
                .queryClass(GitlabProjectDataSource.GitlabProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GitlabProject, GitlabProjectDataSource.GitlabProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(GITLAB)
                        .dataType(MILESTONES)
                        .outputPageSize(1) // 1 file per project
                        .build())
                .build();
    }

    @Builder(builderMethodName = "projectController", builderClassName = "GitlabProjectController")
    private static IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> buildProjectController(
            GitlabProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<GitlabProject, GitlabProjectDataSource.GitlabProjectQuery>builder()
                .queryClass(GitlabProjectDataSource.GitlabProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GitlabProject, GitlabProjectDataSource.GitlabProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(GITLAB)
                        .dataType(PROJECTS)
                        .build())
                .build();
    }

    @Builder(builderMethodName = "groupController", builderClassName = "GitlabGroupController")
    private static IntegrationController<GitlabQuery> buildGroupController(GitlabGroupDataSource dataSource,
                                                                           StorageDataSink storageDataSink,
                                                                           ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<GitlabGroup, GitlabQuery>builder()
                .queryClass(GitlabQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GitlabGroup, GitlabQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(GITLAB)
                        .dataType(GROUPS)
                        .build())
                .build();
    }

    @Builder(builderMethodName = "issueController", builderClassName = "GitlabIssueController")
    private static IntegrationController<GitlabQuery> buildIssueController(GitlabIssueDataSource dataSource,
                                                                           StorageDataSink storageDataSink,
                                                                           ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<GitlabIssue, GitlabQuery>builder()
                .queryClass(GitlabQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GitlabIssue, GitlabQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(GITLAB)
                        .dataType(ISSUES)
                        .outputPageSize(1) // 1 file per project
                        .build())
                .build();
    }

    @Builder(builderMethodName = "projectPipelineController", builderClassName = "GitlabProjectPipelineController")
    private static IntegrationController<GitlabProjectPipelineDataSource.GitlabProjectQuery> buildPipelineController(
            GitlabProjectPipelineDataSource dataSource, StorageDataSink storageDataSink, ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<GitlabProject, GitlabProjectPipelineDataSource.GitlabProjectQuery>builder()
                .queryClass(GitlabProjectPipelineDataSource.GitlabProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<GitlabProject, GitlabProjectPipelineDataSource.GitlabProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(GITLAB)
                        .dataType(PIPELINES)
                        .outputPageSize(1) // 1 file per project
                        .build())
                .build();
    }
}
