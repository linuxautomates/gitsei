package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.JiraControllers;
import io.levelops.ingestion.agent.ingestion.JiraCreateIssueController;
import io.levelops.ingestion.agent.ingestion.JiraEditIssueController;
import io.levelops.ingestion.agent.ingestion.JiraGetIssueController;
import io.levelops.ingestion.agent.ingestion.JiraIterativeScanController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.jira.client.JiraClientFactory;
import io.levelops.integrations.jira.sources.JiraFieldDataSource;
import io.levelops.integrations.jira.sources.JiraIssueDataSource;
import io.levelops.integrations.jira.sources.JiraProjectDataSource;
import io.levelops.integrations.jira.sources.JiraSprintDataSource;
import io.levelops.integrations.jira.sources.JiraStatusDataSource;
import io.levelops.integrations.jira.sources.JiraUserDataSource;
import io.levelops.integrations.jira.sources.JiraUserEmailsDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JiraConfig {

    @Value("${JIRA_OUTPUT_PAGE_SIZE:}")
    private Integer jiraOutputPageSize;


    @Value("${jira.onboarding_in_days:365}")
    private Integer jiraOnboardingInDays;

    @Bean
    public JiraClientFactory jiraClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient,
                                               @Qualifier("allowUnsafeSSLJira") Boolean allowUnsafeSSL,
                                               @Qualifier("jiraRateLimitPerSecond") Double jiraRateLimitPerSecond) {
        return new JiraClientFactory(inventoryService, objectMapper, okHttpClient, allowUnsafeSSL, jiraRateLimitPerSecond);
    }

    @Bean
    public JiraProjectDataSource jiraProjectDataSource(IngestionEngine ingestionEngine,
                                                       JiraClientFactory jiraClientFactory) {
        return ingestionEngine.add("JiraProjectDataSource",
                new JiraProjectDataSource(jiraClientFactory));
    }

    @Bean
    public JiraUserDataSource jiraUserDataSource(IngestionEngine ingestionEngine,
                                                 JiraClientFactory jiraClientFactory) {
        return ingestionEngine.add("JiraUserDataSource",
                new JiraUserDataSource(jiraClientFactory));
    }

    @Bean
    public JiraStatusDataSource jiraStatusDataSource(IngestionEngine ingestionEngine,
                                                     JiraClientFactory jiraClientFactory) {
        return ingestionEngine.add("JiraStatusDataSource",
                new JiraStatusDataSource(jiraClientFactory));
    }

    @Bean
    public JiraSprintDataSource jiraSprintDataSource(IngestionEngine ingestionEngine,
                                                     JiraClientFactory jiraClientFactory) {
        return ingestionEngine.add("JiraSprintDataSource",
                new JiraSprintDataSource(jiraClientFactory));
    }


    @Bean("jiraProjectController")
    public IntegrationController<JiraProjectDataSource.JiraProjectQuery> jiraProjectController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            JiraProjectDataSource jiraProjectDataSource) {

        return ingestionEngine.add("JiraProjectController", JiraControllers.projectController()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .projectDataSource(jiraProjectDataSource)
                .build());
    }

    @Bean
    public JiraIssueDataSource jiraIssueDataSource(IngestionEngine ingestionEngine,
                                                   JiraClientFactory jiraClientFactory) {
        return ingestionEngine.add("JiraIssueDataSource",
                new JiraIssueDataSource(jiraClientFactory));
    }

    @Bean("jiraIssueController")
    public IntegrationController<JiraIssueDataSource.JiraIssueQuery> jiraIssueController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            JiraIssueDataSource jiraIssueDataSource) {

        return ingestionEngine.add("JiraIssueController", JiraControllers.issueController()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .issueDataSource(jiraIssueDataSource)
                .outputPageSize(jiraOutputPageSize)
                .build());
    }

    @Bean("jiraSprintController")
    public IntegrationController<JiraSprintDataSource.JiraSprintQuery> jiraSprintController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            JiraSprintDataSource jiraSprintDataSource) {

        return ingestionEngine.add("JiraSprintController", JiraControllers.sprintController()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .sprintDataSource(jiraSprintDataSource)
                .build());
    }

    @Bean
    public JiraIterativeScanController jiraIterativeScanController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            JiraIssueDataSource jiraIssueDataSource,
            JiraProjectDataSource jiraProjectDataSource,
            JiraUserDataSource jiraUserDataSource,
            JiraStatusDataSource jiraStatusDataSource,
            JiraSprintDataSource jiraSprintDataSource,
            JiraClientFactory jiraClientFactory,
            InventoryService inventoryService,
            @Qualifier("jiraProject") String jiraProject) {

        JiraFieldDataSource jiraFieldDataSource = ingestionEngine.add("JiraFieldDataSource",
                new JiraFieldDataSource(jiraClientFactory));

        return ingestionEngine.add("JiraIterativeScanController", JiraIterativeScanController.builder()
                .objectMapper(objectMapper)
                .issueDataSource(jiraIssueDataSource)
                .projectDataSource(jiraProjectDataSource)
                .fieldDataSource(jiraFieldDataSource)
                .jiraUserDataSource(jiraUserDataSource)
                .jiraStatusDataSource(jiraStatusDataSource)
                .jiraSprintDataSource(jiraSprintDataSource)
                .storageDataSink(storageDataSink)
                .onboardingScanInDays(jiraOnboardingInDays)
                .outputPageSize(jiraOutputPageSize)
                .jiraProject(jiraProject)
                .inventoryService(inventoryService)
                .build());
    }

    @Bean
    public JiraCreateIssueController jiraCreateIssueController(IngestionEngine ingestionEngine,
                                                               ObjectMapper objectMapper,
                                                               JiraClientFactory jiraClientFactory) {
        return ingestionEngine.add("JiraCreateIssueController", JiraCreateIssueController.builder()
                .objectMapper(objectMapper)
                .jiraClientFactory(jiraClientFactory)
                .build());
    }

    @Bean
    public JiraGetIssueController jiraGetIssueController(IngestionEngine ingestionEngine,
                                                         ObjectMapper objectMapper,
                                                         JiraIssueDataSource issueDataSource) {
        return ingestionEngine.add("JiraGetIssueController", new JiraGetIssueController(objectMapper, issueDataSource));
    }

    @Bean
    public JiraEditIssueController jiraEditIssueController(IngestionEngine ingestionEngine,
                                                           ObjectMapper objectMapper,
                                                           JiraClientFactory jiraClientFactory,
                                                           JiraIssueDataSource jiraIssueDataSource) {
        return ingestionEngine.add("JiraEditIssueController", new JiraEditIssueController(objectMapper, jiraClientFactory, jiraIssueDataSource));
    }

    @Bean
    public JiraUserEmailsDataSource jiraUserEmailsDataSource(IngestionEngine ingestionEngine,
                                                             JiraClientFactory jiraClientFactory) {
       return ingestionEngine.add("JiraUserEmailsDataSource", new JiraUserEmailsDataSource(jiraClientFactory));
    }

    @Bean
    public IntegrationController<JiraUserEmailsDataSource.JiraUserEmailQuery> jiraUserEmailIntegrationController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            JiraUserEmailsDataSource jiraUserEmailsDataSource) {
        return ingestionEngine.add("JiraUserEmailIntegrationController", JiraControllers.userEmailsController()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .userEmailsDataSource(jiraUserEmailsDataSource)
                .build());
    }
}
