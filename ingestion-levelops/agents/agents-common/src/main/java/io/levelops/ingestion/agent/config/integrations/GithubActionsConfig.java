package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.GithubActionsController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.github.actions.services.GithubActionsWorkflowRunJobService;
import io.levelops.integrations.github.actions.services.GithubActionsWorkflowRunService;
import io.levelops.integrations.github.actions.sources.GithubActionsWorkflowDataSource;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.services.GithubOrganizationService;
import io.levelops.integrations.github.services.GithubRepositoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GithubActionsConfig {

    private static final String GITHUB_ACTIONS_CONTROLLER = "GithubActionsController";
    private static final String GITHUB_ACTIONS_PIPELINE_DATA_SOURCE = "GithubActionsWorkflowDataSource";

    @Value("${github_actions.onboarding_in_days:7}")
    private Integer githubActionsOnboardingInDays;

    @Bean
    public GithubActionsController githubActionsController(IngestionEngine ingestionEngine,
                                                           ObjectMapper objectMapper,
                                                           GithubActionsWorkflowDataSource workflowDataSource,
                                                           InventoryService inventoryService,
                                                           StorageDataSink storageDataSink) throws JsonProcessingException {

        return ingestionEngine.add(GITHUB_ACTIONS_CONTROLLER, GithubActionsController.builder()
                .objectMapper(objectMapper)
                .inventoryService(inventoryService)
                .storageDataSink(storageDataSink)
                .workflowDataSource(workflowDataSource)
                .onboardingInDays(githubActionsOnboardingInDays)
                .build());
    }

    @Bean
    public GithubActionsWorkflowRunService githubActionsWorkflowRunService() {
        return new GithubActionsWorkflowRunService();
    }

    @Bean
    public GithubActionsWorkflowRunJobService githubActionsWorkflowRunJobService() {
        return new GithubActionsWorkflowRunJobService();
    }

    @Bean
    public GithubActionsWorkflowDataSource workflowDataSource(IngestionEngine ingestionEngine,
                                                              GithubClientFactory clientFactory,
                                                              GithubRepositoryService repositoryService,
                                                              GithubOrganizationService organizationService,
                                                              GithubActionsWorkflowRunService workflowRunService,
                                                              GithubActionsWorkflowRunJobService workflowRunJobService) {
        return ingestionEngine.add(GITHUB_ACTIONS_PIPELINE_DATA_SOURCE, new GithubActionsWorkflowDataSource(clientFactory, repositoryService, organizationService, workflowRunService, workflowRunJobService));
    }
}
