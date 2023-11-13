package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.EmptyIngestionResult;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.models.ScmRepoMappingQuery;
import io.levelops.ingestion.models.ScmRepoMappingResult;
import io.levelops.integrations.github.services.GithubOrganizationService;
import io.levelops.integrations.scm_repo_mapping.RepoMappingClient;
import io.levelops.integrations.scm_repo_mapping.RepoMappingClientRegistry;
import lombok.extern.log4j.Log4j2;
import org.jsoup.helper.Validate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class ScmRepoMappingController implements DataController<ScmRepoMappingQuery> {
    private final RepoMappingClientRegistry repoMappingClientRegistry;
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final GithubOrganizationService organizationService;

    public ScmRepoMappingController(
            RepoMappingClientRegistry repoMappingClientRegistry,
            InventoryService inventoryService,
            ObjectMapper objectMapper,
            GithubOrganizationService organizationService
    ) {
        this.repoMappingClientRegistry = repoMappingClientRegistry;
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.organizationService = organizationService;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, ScmRepoMappingQuery query) throws IngestException {
        Validate.notNull(query, "Query cannot be null");
        Validate.notNull(query.getIntegrationKey(), "Integration key cannot be null");
        log.info("Running repo mapping job for tenant {}, integration {}, user count {}",
                jobContext.getTenantId(), query.getIntegrationKey().getIntegrationId(), query.getUserIds().size());

        try {
            Integration integration = inventoryService.getIntegration(query.getIntegrationKey());
            RepoMappingClient repoMappingClient = repoMappingClientRegistry.getClient(integration.getApplication());
            
            // TODO: This breaks the app agnostic nature of the repo mapping controller. Abstract the organizations part out
            // into the repoMappingClient. This will require the GithubOrganizationService to be moved to commons.
            List<String> organizations = organizationService.getOrganizations(query.getIntegrationKey());
            log.info("Found {} organizations for tenant {} and integration {}",
                    organizations.size(), jobContext.getTenantId(), query.getIntegrationKey().getIntegrationId());
            Set<String> allRepos = query.getUserIds().stream()
                    .flatMap(userId -> {
                                List<String> repos = List.of();
                                try {
                                    repos = repoMappingClient.getReposForUser(jobContext.getTenantId(), integration.getId(), userId, organizations);
                                } catch (Exception e) {
                                    log.error("Error getting repos for user {} in integration {}", userId, integration.getId(), e);
                                }
                                return repos.stream();
                            }
                    )
                    .collect(Collectors.toSet());
            log.info("Found {} repos for tenant {} and integration {}",
                    allRepos.size(), jobContext.getTenantId(), query.getIntegrationKey().getIntegrationId());
            return ScmRepoMappingResult.builder()
                    .mappedRepos(new ArrayList<>(allRepos))
                    .fetchedAt(Instant.now())
                    .build();
        } catch (InventoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ScmRepoMappingQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, ScmRepoMappingQuery.class);
    }
}
