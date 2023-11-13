package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.generic.GenericIntegrationController;
import io.levelops.ingestion.controllers.generic.GenericMultiIntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.integrations.github.sources.GithubRepositoryDataSource.GithubRepositoryQuery;
import io.levelops.integrations.jira.sources.JiraIssueDataSource.JiraIssueQuery;
import io.levelops.integrations.jira.sources.JiraProjectDataSource.JiraProjectQuery;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GenericIntegrationConfig {

    @Bean
    public GenericMultiIntegrationController genericIntegrationController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            IntegrationController<JiraProjectQuery> jiraProjectController,
            IntegrationController<JiraIssueQuery> jiraIssueController,
            @Qualifier("githubRepositoryController") IntegrationController<GithubRepositoryQuery> githubRepositoryController,
            @Qualifier("githubCommitController") IntegrationController<GithubRepositoryQuery> githubCommitController) {

        GenericIntegrationController genericIntegrationController = ingestionEngine.add("GenericIntegrationController", GenericIntegrationController.builder()
                .objectMapper(objectMapper)
                .controller(jiraIssueController)
                .controller(jiraProjectController)
                .controller(githubCommitController)
                .controller(githubRepositoryController)
                .build());
        return ingestionEngine.add("GenericMultiIntegrationController", GenericMultiIntegrationController.builder()
                .objectMapper(objectMapper)
                .genericIntegrationController(genericIntegrationController)
                .build());
    }
}
