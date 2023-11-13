package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.SinglePageStrategy;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.sonarqube.models.*;
import io.levelops.integrations.sonarqube.sources.*;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Sonarqube's implementation of the {@link DataController}
 */
@Log4j2
public class SonarQubeControllers {

    public static final String SONARQUBE = "sonarqube";
    public static final String ANALYSES = "analyses";
    public static final String BRANCH = "branch";
    public static final String ISSUES = "issues";
    public static final String PR_ISSUES = "pr-issues";
    public static final String PROJECT = "project";
    public static final String QUALITY_GATES = "quality-gates";
    public static final String USER = "users";
    public static final String USER_GROUP = "user-group";

    @Builder(builderMethodName = "qualityGatesController", builderClassName = "SonarQubeQualityGatesControllerBuilder")
    private static IntegrationController<SonarQubeIterativeScanQuery> buildQualityGatesController(
            SonarQubeQualityGateDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<QualityGate, SonarQubeIterativeScanQuery>builder()
                .queryClass(SonarQubeIterativeScanQuery.class)
                .paginationStrategy(SinglePageStrategy.<QualityGate, SonarQubeIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(SONARQUBE)
                        .dataType(QUALITY_GATES)
                        .build())
                .build();
    }

    @Builder(builderMethodName = "userController", builderClassName = "SonarQubeUserControllerBuilder")
    private static IntegrationController<SonarQubeIterativeScanQuery> buildUserController(
            SonarQubeUserDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<User,SonarQubeIterativeScanQuery>builder()
                .queryClass(SonarQubeIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<User, SonarQubeIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(USER)
                        .integrationType(SONARQUBE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .build())
                .build();
    }

    @Builder(builderMethodName = "userGroupController", builderClassName = "SonarQubeUserGroupControllerBuilder")
    private static IntegrationController<SonarQubeIterativeScanQuery> buildUserGroupController(
            SonarQubeUserGroupsDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<Group,SonarQubeIterativeScanQuery>builder()
                .queryClass(SonarQubeIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<Group, SonarQubeIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(USER_GROUP)
                        .integrationType(SONARQUBE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .build())
                .build();
    }

    @Builder(builderMethodName = "issueController", builderClassName = "SonarQubeIssueControllerBuilder")
    private static IntegrationController<SonarQubeIterativeScanQuery> buildIssueController(
            SonarQubeIssueDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<Issue, SonarQubeIterativeScanQuery>builder()
                .queryClass(SonarQubeIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<Issue, SonarQubeIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataType(ISSUES)
                        .integrationType(SONARQUBE)
                        .storageDataSink(storageDataSink)
                        .dataSource(dataSource)
                        .skipEmptyResults(true)
                        .build())
                .build();
    }

    @Builder(builderMethodName = "projectController", builderClassName = "SonarQubeProjectController")
    private static IntegrationController<SonarQubeProjectDataSource.SonarQubeProjectQuery> buildProjectController(
            SonarQubeProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<Project, SonarQubeProjectDataSource.SonarQubeProjectQuery>builder()
                .queryClass(SonarQubeProjectDataSource.SonarQubeProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<Project, SonarQubeProjectDataSource.SonarQubeProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(SONARQUBE)
                        .dataType(PROJECT)
                        .build())
                .build();
    }

    @Builder(builderMethodName = "analysesController", builderClassName = "SonarQubeAnalysesControllerBuilder")
    private static IntegrationController<SonarQubeProjectDataSource.SonarQubeProjectQuery> buildAnalysesController(
            SonarQubeProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<Project, SonarQubeProjectDataSource.SonarQubeProjectQuery>builder()
                .queryClass(SonarQubeProjectDataSource.SonarQubeProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<Project, SonarQubeProjectDataSource.SonarQubeProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(SONARQUBE)
                        .dataType(ANALYSES)
                        .outputPageSize(1) // 1 project per page
                        .skipEmptyResults(true)
                        .customEmptyPagePredicate(page -> page.stream()
                                .map(Project::getAnalyses)
                                .allMatch(CollectionUtils::isEmpty))
                        .build())
                .build();
    }

    @Builder(builderMethodName = "branchController", builderClassName = "SonarQubeBranchControllerBuilder")
    private static IntegrationController<SonarQubeProjectDataSource.SonarQubeProjectQuery> buildBranchController(
            SonarQubeProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<Project, SonarQubeProjectDataSource.SonarQubeProjectQuery>builder()
                .queryClass(SonarQubeProjectDataSource.SonarQubeProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<Project, SonarQubeProjectDataSource.SonarQubeProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(SONARQUBE)
                        .dataType(BRANCH)
                        .outputPageSize(1) // 1 project per page
                        .skipEmptyResults(true)
                        .customEmptyPagePredicate(page -> page.stream()
                                .map(Project::getBranches)
                                .allMatch(CollectionUtils::isEmpty))
                        .build())
                .build();
    }

    @Builder(builderMethodName = "pullRequestIssueController", builderClassName = "SonarQubePullRequestIssueControllerBuilder")
    private static IntegrationController<SonarQubeProjectDataSource.SonarQubeProjectQuery> buildPullRequestIssueController(
            SonarQubeProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<Project, SonarQubeProjectDataSource.SonarQubeProjectQuery>builder()
                .queryClass(SonarQubeProjectDataSource.SonarQubeProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<Project, SonarQubeProjectDataSource.SonarQubeProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(SONARQUBE)
                        .dataType(PR_ISSUES)
                        .outputPageSize(1) // 1 project per page
                        .skipEmptyResults(true)
                        .customEmptyPagePredicate(page -> page.stream()
                                .map(Project::getPullRequests)
                                .allMatch(CollectionUtils::isEmpty))
                        .build())
                .build();
    }
}
