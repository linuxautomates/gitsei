package io.levelops.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.model.ScmRepoRequest;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.DbListResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.models.ScmRepository;
import io.levelops.preflightchecks.BitbucketPreflightCheck;
import io.levelops.preflightchecks.BitbucketServerPreflightCheck;
import io.levelops.preflightchecks.GithubActionsPreflightCheck;
import io.levelops.preflightchecks.GithubPreflightCheck;
import io.levelops.preflightchecks.GitlabPreflightCheck;
import io.levelops.preflightchecks.ScmRepositoryService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class OnboardingService {

    private final InventoryService inventoryService;
    private final IntegrationService integrationService;
    private final GithubPreflightCheck githubPreflightCheck;
    private final GitlabPreflightCheck gitlabPreflightCheck;
    private final BitbucketPreflightCheck bitbucketPreflightCheck;
    private final BitbucketServerPreflightCheck bitbucketServerPreflightCheck;
    private final GithubActionsPreflightCheck githubActionsPreflightCheck;
    private final ObjectMapper mapper;
    private final AggCacheService cacheService;

    @Autowired
    public OnboardingService(InventoryService inventoryService, IntegrationService integrationService, GithubPreflightCheck githubPreflightCheck, GitlabPreflightCheck gitlabPreflightCheck,
                             BitbucketPreflightCheck bitbucketPreflightCheck, BitbucketServerPreflightCheck bitbucketServerPreflightCheck, GithubActionsPreflightCheck githubActionsPreflighCheck, ObjectMapper mapper, AggCacheService cacheService) {
        this.inventoryService = inventoryService;
        this.integrationService = integrationService;
        this.githubPreflightCheck = githubPreflightCheck;
        this.gitlabPreflightCheck = gitlabPreflightCheck;
        this.bitbucketPreflightCheck = bitbucketPreflightCheck;
        this.bitbucketServerPreflightCheck = bitbucketServerPreflightCheck;
        this.githubActionsPreflightCheck = githubActionsPreflighCheck;
        this.mapper = mapper;
        this.cacheService = cacheService;
    }

    public DbListResponse<ScmRepository> getRepositories(String company, String integrationId, List<String> filterRepos, int page, int pageSize) throws Exception {
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");

        Integration integration = getIntegration(company, integrationId);
        IntegrationType integrationType = getIntegrationTypeAndValidate(integration);

        log.info("Discovering repos from Cloud for tenant={}, integration_id={}, app={} (page={}, pageSize={})", company, integrationId, integrationType, page, pageSize);

        if (IntegrationType.GITLAB == integrationType) {
            page++;
        }

        ScmRepositoryService repositoryService = getScmRepoService(integrationType);

        log.info("Counting repos for tenant={}, integration_id={}, app={}...", company, integrationId, integrationType);
        int repoCount = getRepositoryCount(company, integration, repositoryService, StringUtils.EMPTY, StringUtils.EMPTY);
        log.info("Done counting repos for tenant={}, integration_id={}, app={}: found {}", company, integrationId, integrationType, repoCount);

        log.info("Getting page={} of repos from Cloud for tenant={}, integration_id={}, app={}, page_size={}", page, company, integrationId, integrationType, pageSize);
        List<ScmRepository> repos = repositoryService.getScmRepositories(company, integration, filterRepos, page, pageSize);
        log.info("Got page={} of repos from Cloud for tenant={}, integration_id={}, app={}, page_size={} - results_size={}", page, company, integrationId, integrationType, pageSize, repos.size());

        return DbListResponse.of(repos, repoCount);
    }

    public DbListResponse<ScmRepository> searchRepository(String company, String integrationId, String repoName, String projectKey, int pageNumber, int pageSize) throws Exception {
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");

        Integration integration = getIntegration(company, integrationId);
        IntegrationType integrationType = getIntegrationTypeAndValidate(integration);

        if (StringUtils.isEmpty(repoName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repository name cannot be null or empty for " + integrationType + " repository search.");
        }

        if (IntegrationType.GITLAB == integrationType) {
            pageNumber++;
        }

        ScmRepositoryService repositoryService = getScmRepoService(integrationType);
        int repoSize = getRepositoryCount(company, integration, repositoryService, repoName, projectKey);
        List<ScmRepository> repos = repositoryService.searchScmRepository(company, integration, repoName, projectKey, pageNumber, pageSize);

        return DbListResponse.of(repos, repoSize);
    }

    public Boolean updateRepos(String company, ScmRepoRequest scmRepoRequest) throws InventoryException {
        Validate.notBlank(scmRepoRequest.getIntegrationId(), "integrationId cannot be null or empty.");

        Integration integration = getIntegration(company, scmRepoRequest.getIntegrationId());
        getIntegrationTypeAndValidate(integration);

        if (scmRepoRequest.getScmParams() == null || scmRepoRequest.getScmParams().size() == 0) {
            log.info("Empty integration id or repo list provide, will not execute update");
            return false;
        }

        Integration integrationUpdate = Integration.builder()
                .id(scmRepoRequest.getIntegrationId())
                .appendMetadata(true)
                .metadata(scmRepoRequest.getScmParams())
                .build();
        try {
            return integrationService.update(company, integrationUpdate);
        } catch (SQLException e) {
            log.error("Exception occurred while trying to update integration metadata ", e);
            return false;
        }
    }

    private Integration getIntegration(String company, String integrationId) throws InventoryException {
        return inventoryService.getIntegration(IntegrationKey.builder()
                .tenantId(company)
                .integrationId(integrationId)
                .build());
    }

    private IntegrationType getIntegrationTypeAndValidate(Integration integration) throws InventoryException {
        IntegrationType integrationType = IntegrationType.fromString(integration.getApplication());
        Validate.notNull(integrationType, "integrationType cannot be null.");

        if (integrationType.equals(IntegrationType.GITHUB_ACTIONS))
            return integrationType;

        if (!integrationType.isScmFamily()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-SCM integration provided to get repo details");
        }

        return integrationType;
    }

    private Integer getRepositoryCount(String company, Integration integration, ScmRepositoryService repositoryService, String repoName, String projectKey) throws Exception {

        return (Integer) AggCacheUtils.cacheOrCallGeneric(false, company, company + "_" + integration.getId() + "_" + repoName + "_" + projectKey,
                "" + integration.getId().hashCode(), List.of(integration.getId()), mapper, cacheService, Integer.class, 1L, TimeUnit.DAYS,
                () -> repositoryService.getTotalRepositoriesCount(company, integration, repoName, projectKey));
    }

    private ScmRepositoryService getScmRepoService(IntegrationType integrationType) {

        switch (integrationType) {
            case GITHUB:
                return githubPreflightCheck;
            case GITLAB:
                return gitlabPreflightCheck;
            case BITBUCKET:
                return bitbucketPreflightCheck;
            case BITBUCKET_SERVER:
                return bitbucketServerPreflightCheck;
            case GITHUB_ACTIONS:
                return githubActionsPreflightCheck;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported integration type " + integrationType);
        }
    }
}
