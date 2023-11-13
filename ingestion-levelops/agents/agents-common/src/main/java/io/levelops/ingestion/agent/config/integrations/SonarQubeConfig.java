package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.SonarQubeControllers;
import io.levelops.ingestion.agent.ingestion.SonarQubeIterativeScanController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.sonarqube.client.SonarQubeClientFactory;
import io.levelops.integrations.sonarqube.models.SonarQubeIterativeScanQuery;
import io.levelops.integrations.sonarqube.services.SonarQubeProjectEnrichmentService;
import io.levelops.integrations.sonarqube.sources.*;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

import static io.levelops.integrations.sonarqube.sources.SonarQubeProjectDataSource.Enrichment.*;


@Configuration
public class SonarQubeConfig {

    private static final String SONARQUBE_QUALITY_GATES_DATA_SOURCE = "SonarQubeQualityGatesDataSource";
    private static final String SONARQUBE_USER_DATA_SOURCE = "SonarQubeUserDataSource";
    private static final String SONARQUBE_USER_GROUP_DATA_SOURCE = "SonarQubeUserGroupDataSource";
    private static final String SONARQUBE_ISSUE_DATA_SOURCE = "SonarQubeIssueDataSource";
    public static final String SONARQUBE_BRANCH_DATA_SOURCE = "SonarQubeBranchDataSource";
    public static final String SONARQUBE_PR_ISSUE_DATA_SOURCE = "SonarQubePRIssueDataSource";
    public static final String SONARQUBE_ANALYSES_DATA_SOURCE = "SonarQubeAnalysesDataSource";
    public static final String SONARQUBE_PROJECT_ENRICHMENT_DATA_SOURCE = "SonarQubeProjectEnrichmentDataSource";

    public static final String SONARQUBE_PROJECT_CONTROLLER = "SonarQubeProjectController";
    public static final String SONARQUBE_ANALYSES_CONTROLLER = "SonarQubeAnalysesController";
    public static final String SONARQUBE_PR_ISSUE_CONTROLLER = "SonarQubePRIssueController";
    public static final String SONARQUBE_BRANCH_CONTROLLER = "SonarQubeBranchController";
    public static final String SONARQUBE_QUALITY_GATES_CONTROLLER = "SonarQubeQualityGatesController";
    public static final String SONARQUBE_USER_CONTROLLER = "SonarQubeUserController";
    public static final String SONARQUBE_USER_GROUP_CONTROLLER = "SonarQubeUserGroupController";
    public static final String SONARQUBE_ISSUE_CONTROLLER = "SonarQubeIssueController";


    @Value("${sonarqube.onboarding_in_days:360}")
    private Integer sonarqubeOnboardingInDays;

    @Bean
    public SonarQubeClientFactory sonarqubeClientFactory(InventoryService inventoryService,
                                                         ObjectMapper objectMapper,
                                                         OkHttpClient okHttpClient) {
        return new SonarQubeClientFactory(inventoryService, objectMapper, okHttpClient, 0);
    }

    @Bean
    public SonarQubeProjectEnrichmentService sonarQubeProjectEnrichmentService() {
        return new SonarQubeProjectEnrichmentService();
    }

    @Bean("sonarqubeProjectController")
    public IntegrationController<SonarQubeProjectDataSource.SonarQubeProjectQuery> sonarQubeProjectController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SonarQubeClientFactory sonarQubeClientFactory,
            SonarQubeProjectEnrichmentService sonarQubeProjectEnrichmentService) {
        SonarQubeProjectDataSource projectDataSource = ingestionEngine.add(SONARQUBE_PROJECT_ENRICHMENT_DATA_SOURCE,
                new SonarQubeProjectDataSource(sonarQubeClientFactory,
                        EnumSet.of(MEASURE),
                        sonarQubeProjectEnrichmentService));
        return ingestionEngine.add(SONARQUBE_PROJECT_CONTROLLER, SonarQubeControllers.projectController()
                .objectMapper(objectMapper)
                .dataSource(projectDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("sonarqubeAnalysesController")
    public IntegrationController<SonarQubeProjectDataSource.SonarQubeProjectQuery> sonarqubeAnalysesController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SonarQubeClientFactory sonarQubeClientFactory,
            SonarQubeProjectEnrichmentService sonarQubeProjectEnrichmentService) {
        SonarQubeProjectDataSource sonarqubeAnalysesDataSource = ingestionEngine.add(SONARQUBE_ANALYSES_DATA_SOURCE,
                new SonarQubeProjectDataSource(sonarQubeClientFactory,
                        EnumSet.of(PROJECT_ANALYSIS), sonarQubeProjectEnrichmentService));
        return ingestionEngine.add(SONARQUBE_ANALYSES_CONTROLLER, SonarQubeControllers.analysesController()
                .objectMapper(objectMapper)
                .dataSource(sonarqubeAnalysesDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("sonarqubePullRequestIssueController")
    public IntegrationController<SonarQubeProjectDataSource.SonarQubeProjectQuery> sonarqubePullRequestIssueController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SonarQubeClientFactory sonarQubeClientFactory,
            SonarQubeProjectEnrichmentService sonarQubeProjectEnrichmentService) {
        SonarQubeProjectDataSource sonarQubePullRequestDataSource = ingestionEngine.add(SONARQUBE_PR_ISSUE_DATA_SOURCE,
                new SonarQubeProjectDataSource(sonarQubeClientFactory,
                        EnumSet.of(PULL_REQUEST_ISSUE), sonarQubeProjectEnrichmentService));
        return ingestionEngine.add(SONARQUBE_PR_ISSUE_CONTROLLER, SonarQubeControllers.pullRequestIssueController()
                .objectMapper(objectMapper)
                .dataSource(sonarQubePullRequestDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("sonarqubeBranchController")
    public IntegrationController<SonarQubeProjectDataSource.SonarQubeProjectQuery> sonarqubeBranchController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SonarQubeClientFactory sonarQubeClientFactory,
            SonarQubeProjectEnrichmentService sonarQubeProjectEnrichmentService) {
        SonarQubeProjectDataSource sonarQubeBranchDataSource = ingestionEngine.add(SONARQUBE_BRANCH_DATA_SOURCE,
                new SonarQubeProjectDataSource(sonarQubeClientFactory,
                        EnumSet.of(PROJECT_BRANCH), sonarQubeProjectEnrichmentService));
        return ingestionEngine.add(SONARQUBE_BRANCH_CONTROLLER, SonarQubeControllers.branchController()
                .objectMapper(objectMapper)
                .dataSource(sonarQubeBranchDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("sonarqubeQualityGatesController")
    public IntegrationController<SonarQubeIterativeScanQuery> sonarqubeQualityGatesController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SonarQubeClientFactory sonarQubeClientFactory) {
        SonarQubeQualityGateDataSource sonarQubeQualityGateDataSource = ingestionEngine.add(SONARQUBE_QUALITY_GATES_DATA_SOURCE,
                new SonarQubeQualityGateDataSource(sonarQubeClientFactory));
        return ingestionEngine.add(SONARQUBE_QUALITY_GATES_CONTROLLER, SonarQubeControllers.qualityGatesController()
                .objectMapper(objectMapper)
                .dataSource(sonarQubeQualityGateDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("sonarqubeUserController")
    public IntegrationController<SonarQubeIterativeScanQuery> sonarqubeUserController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SonarQubeClientFactory sonarQubeClientFactory) {
        SonarQubeUserDataSource sonarQubeUserDataSource = ingestionEngine.add(SONARQUBE_USER_DATA_SOURCE,
                new SonarQubeUserDataSource(sonarQubeClientFactory));
        return ingestionEngine.add(SONARQUBE_USER_CONTROLLER, SonarQubeControllers.userController()
                .objectMapper(objectMapper)
                .dataSource(sonarQubeUserDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("sonarqubeUserGroupController")
    public IntegrationController<SonarQubeIterativeScanQuery> sonarqubeUserGroupController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SonarQubeClientFactory sonarQubeClientFactory) {
        SonarQubeUserGroupsDataSource sonarQubeUserGroupsDataSource = ingestionEngine.add(SONARQUBE_USER_GROUP_DATA_SOURCE,
                new SonarQubeUserGroupsDataSource(sonarQubeClientFactory));
        return ingestionEngine.add(SONARQUBE_USER_GROUP_CONTROLLER, SonarQubeControllers.userGroupController()
                .objectMapper(objectMapper)
                .dataSource(sonarQubeUserGroupsDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("sonarqubeIssueController")
    public IntegrationController<SonarQubeIterativeScanQuery> sonarqubeIssueController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SonarQubeClientFactory sonarQubeClientFactory) {
        SonarQubeIssueDataSource sonarQubeIssueDataSource = ingestionEngine.add(SONARQUBE_ISSUE_DATA_SOURCE,
                new SonarQubeIssueDataSource(sonarQubeClientFactory));
        return ingestionEngine.add(SONARQUBE_ISSUE_CONTROLLER, SonarQubeControllers.issueController()
                .objectMapper(objectMapper)
                .dataSource(sonarQubeIssueDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean
    public SonarQubeIterativeScanController sonarqubeIterativeScanController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            InventoryService inventoryService,
            @Qualifier("sonarqubeProjectController")
                    IntegrationController<SonarQubeProjectDataSource.SonarQubeProjectQuery> sonarqubeProjectController,
            @Qualifier("sonarqubeAnalysesController")
                    IntegrationController<SonarQubeProjectDataSource.SonarQubeProjectQuery> sonarqubeAnalysesController,
            @Qualifier("sonarqubePullRequestIssueController")
                    IntegrationController<SonarQubeProjectDataSource.SonarQubeProjectQuery> sonarqubePRIssueController,
            @Qualifier("sonarqubeBranchController")
                    IntegrationController<SonarQubeProjectDataSource.SonarQubeProjectQuery> sonarqubeBranchController,
            @Qualifier("sonarqubeQualityGatesController")
                    IntegrationController<SonarQubeIterativeScanQuery> sonarqubeQualityGatesController,
            @Qualifier("sonarqubeUserController")
                    IntegrationController<SonarQubeIterativeScanQuery> sonarqubeUserController,
            @Qualifier("sonarqubeUserGroupController")
                    IntegrationController<SonarQubeIterativeScanQuery> sonarqubeUserGroupController,
            @Qualifier("sonarqubeIssueController")
                    IntegrationController<SonarQubeIterativeScanQuery> sonarqubeIssueController) {
        return ingestionEngine.add("SonarQubeIterativeScanController", SonarQubeIterativeScanController.builder()
                .objectMapper(objectMapper)
                .inventoryService(inventoryService)
                .qualityGatesController(sonarqubeQualityGatesController)
                .userController(sonarqubeUserController)
                .userGroupController(sonarqubeUserGroupController)
                .issueController(sonarqubeIssueController)
                .projectController(sonarqubeProjectController)
                .analysesController(sonarqubeAnalysesController)
                .branchController(sonarqubeBranchController)
                .prIssueController(sonarqubePRIssueController)
                .objectMapper(objectMapper)
                .onboardingInDays(sonarqubeOnboardingInDays)
                .build());
    }
}
