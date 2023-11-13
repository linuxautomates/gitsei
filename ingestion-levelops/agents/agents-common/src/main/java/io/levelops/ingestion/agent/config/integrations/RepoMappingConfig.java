package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.ScmRepoMappingController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.services.GithubOrganizationService;
import io.levelops.integrations.scm_repo_mapping.GithubRepoMappingClient;
import io.levelops.integrations.scm_repo_mapping.RepoMappingClientRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepoMappingConfig {
    @Bean
    GithubRepoMappingClient githubRepoMappingClient(GithubClientFactory githubClientFactory) {
        return new GithubRepoMappingClient(githubClientFactory);
    }

    @Bean
    RepoMappingClientRegistry repoMappingClientRegistry(GithubRepoMappingClient githubRepoMappingClient) {
        var registry = new RepoMappingClientRegistry();
        registry.registerClient("github", githubRepoMappingClient);
        return registry;
    }

    @Bean
    ScmRepoMappingController scmRepoMappingController(
            IngestionEngine ingestionEngine,
            RepoMappingClientRegistry repoMappingClientRegistry,
            InventoryService inventoryService,
            ObjectMapper objectMapper,
            GithubOrganizationService githubOrganizationService
    ) {
        var repoMappingController = new ScmRepoMappingController(repoMappingClientRegistry, inventoryService, objectMapper, githubOrganizationService);
        return ingestionEngine.add("ScmRepoMappingController", repoMappingController);
    }
}
