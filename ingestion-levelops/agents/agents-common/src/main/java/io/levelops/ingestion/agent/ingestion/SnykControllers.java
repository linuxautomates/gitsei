package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.SinglePageIntegrationController;
import io.levelops.ingestion.controllers.generic.BaseIntegrationQuery;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.snyk.models.SnykDepGraph;
import io.levelops.integrations.snyk.models.SnykIssues;
import io.levelops.integrations.snyk.models.SnykOrg;
import io.levelops.integrations.snyk.models.SnykProject;
import io.levelops.integrations.snyk.sources.SnykAllProjectsDataSource;
import io.levelops.integrations.snyk.sources.SnykDepGraphDataSource;
import io.levelops.integrations.snyk.sources.SnykIssueDataSource;
import io.levelops.integrations.snyk.sources.SnykOrgDataSource;
import io.levelops.integrations.snyk.sources.SnykProjectDataSource;
import lombok.Builder;

public class SnykControllers {
    @Builder(builderMethodName = "orgsController", builderClassName = "SnykOrgsControllerBuilder")
    private static IntegrationController<BaseIntegrationQuery> buildSnykOrgsController(
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SnykOrgDataSource dataSource) {
        // 1 GCS file for all orgs
        return SinglePageIntegrationController.<SnykOrg, BaseIntegrationQuery>builder()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .dataSource(dataSource)
                .queryClass(BaseIntegrationQuery.class)
                .integrationType("snyk")
                .dataType("orgs")
                .build();
    }

    @Builder(builderMethodName = "projectsController", builderClassName = "SnykProjectsControllerBuilder")
    private static IntegrationController<SnykProjectDataSource.SnykProjectQuery> buildSnykProjectsController(
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SnykProjectDataSource dataSource) {
        // split projects into multiple GCS files (default size: 500)
        return PaginatedIntegrationController.<SnykProject, SnykProjectDataSource.SnykProjectQuery>builder()
                .queryClass(SnykProjectDataSource.SnykProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<SnykProject, SnykProjectDataSource.SnykProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("snyk")
                        .dataType("projects")
                        .build())
                .build();
    }

    @Builder(builderMethodName = "allProjectsController", builderClassName = "SnykAllProjectsControllerBuilder")
    private static IntegrationController<BaseIntegrationQuery> buildSnykAllProjectsController(
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SnykAllProjectsDataSource dataSource) {
        // split projects into multiple GCS files (default size: 500)
        return PaginatedIntegrationController.<SnykProject, BaseIntegrationQuery>builder()
                .queryClass(BaseIntegrationQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<SnykProject, BaseIntegrationQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("snyk")
                        .dataType("projects")
                        .build())
                .build();
    }

    @Builder(builderMethodName = "issuesController", builderClassName = "SnykIssuesControllerBuilder")
    private static IntegrationController<BaseIntegrationQuery> buildSnykIssuesController(
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SnykIssueDataSource dataSource) {
        return PaginatedIntegrationController.<SnykIssues, BaseIntegrationQuery>builder()
                .queryClass(BaseIntegrationQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<SnykIssues, BaseIntegrationQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("snyk")
                        .dataType("issues")
                        .outputPageSize(1) // 1 GCS file per project
                        .build())
                .build();
    }

    @Builder(builderMethodName = "depGraphController", builderClassName = "SnykDepGraphControllerBuilder")
    private static IntegrationController<BaseIntegrationQuery> buildSnykDepGraphController(
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SnykDepGraphDataSource dataSource) {
        return PaginatedIntegrationController.<SnykDepGraph, BaseIntegrationQuery>builder()
                .queryClass(BaseIntegrationQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<SnykDepGraph, BaseIntegrationQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("snyk")
                        .dataType("depGraph")
                        .outputPageSize(1) // 1 GCS file per project
                        .build())
                .build();
    }
}
