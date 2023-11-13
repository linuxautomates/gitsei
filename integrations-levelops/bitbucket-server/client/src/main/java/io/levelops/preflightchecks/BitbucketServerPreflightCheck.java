package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClient;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientException;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientFactory;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerEnrichedProjectData;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerProject;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerRepository;
import io.levelops.integrations.bitbucket_server.services.BitbucketServerFetchReposService;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import io.levelops.models.ScmRepository;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
public class BitbucketServerPreflightCheck implements PreflightCheck, ScmRepositoryService {

    private final BitbucketServerFetchReposService fetchReposService;
    @Getter
    private final String integrationType = IntegrationType.BITBUCKET_SERVER.toString();
    private BitbucketServerClientFactory clientFactory;

    @Autowired
    public BitbucketServerPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient, InventoryService inventoryService) {
        this.clientFactory = BitbucketServerClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .inventoryService(inventoryService)
                .build();
        this.fetchReposService = new BitbucketServerFetchReposService();
    }

    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        BitbucketServerClient client;
        try {
            client = clientFactory.buildFromToken(tenantId, integration, token, true);
        } catch (InventoryException e) {
            log.error("check: error creating client for tenant: " + tenantId + " , integration: " + integration
                    + e.getMessage(), e);
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
        builder.check(checkProjects(client, builder));
        return builder.build();
    }

    private PreflightCheckResult checkProjects(BitbucketServerClient client, PreflightCheckResults.PreflightCheckResultsBuilder builder) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get projects")
                .success(true);
        try {
            var response = client.getProjects(0);
            if (response != null && CollectionUtils.isNotEmpty(response.getValues())) {
                BitbucketServerProject project = response.getValues().get(0);
                if (project != null) {
                    builder.check(checkRepositories(client, project.getKey(), builder));
                }
            }
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkRepositories(BitbucketServerClient client, String projectKey, PreflightCheckResults.PreflightCheckResultsBuilder builder) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get repositories")
                .success(true);
        try {
            var response = client.getRepositories(projectKey, 0);
            if (response != null && CollectionUtils.isNotEmpty(response.getValues())) {
                BitbucketServerRepository repository = response.getValues().get(0);
                if (repository != null) {
                    builder.check(checkCommits(client, projectKey, repository.getSlug()));
                    builder.check(checkPullRequests(client, projectKey, repository.getSlug()));
                }
            }
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkCommits(BitbucketServerClient client, String projectKey, String repoSlug) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get commits")
                .success(true);
        try {
            client.getCommits(projectKey, repoSlug, 0);
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkPullRequests(BitbucketServerClient client, String projectKey, String repoSlug) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get pull requests")
                .success(true);
        try {
            client.getPullRequests(projectKey, repoSlug, 0);
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    @Override
    public int getTotalRepositoriesCount(String company, Integration integration, String repoName, String projectKey) throws BitbucketServerClientException, IOException {

        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(company)
                .integrationId(integration.getId())
                .build();

        BitbucketServerClient client = clientFactory.get(integrationKey, true);
        if (StringUtils.isNotEmpty(repoName)) {
            return (int) client.searchRepositories(repoName.toLowerCase()).count();
        }

        return (int) fetchReposService.fetchRepos(client).count();
    }

    @Override
    public List<ScmRepository> getScmRepositories(String company, Integration integration, List<String> filterRepos, int pageNumber, int pageSize) throws BitbucketServerClientException {

        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(company)
                .integrationId(integration.getId())
                .build();

        BitbucketServerClient client = clientFactory.get(integrationKey, true);
        List<BitbucketServerEnrichedProjectData> bitBucketRepoList = fetchReposService.fetchRepos(client)
                .filter(i -> CollectionUtils.isEmpty(filterRepos) || filterRepos.contains(i.getRepository().getName()))
                .skip(pageNumber * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(bitBucketRepoList)) {
            return List.of();
        }

        List<ScmRepository> repoList = Lists.newArrayList();
        bitBucketRepoList.forEach(bitbucketRepo -> {
            ScmRepository repo = ScmRepository.builder()
                    .name(bitbucketRepo.getRepository().getName())
                    .description(bitbucketRepo.getRepository().getDescription())
                    .url(client.resourceUrl + "/projects/" + bitbucketRepo.getProject().getKey() + "/repos/" + bitbucketRepo.getRepository().getName() + "/browse")
                    .build();
            repoList.add(repo);
        });

        return repoList;
    }

    @Override
    public List<ScmRepository> searchScmRepository(String company, Integration integration, String repoName, String projectKey, int pageNumber, int pageSize) throws Exception {
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(company)
                .integrationId(integration.getId())
                .build();

        BitbucketServerClient client = clientFactory.get(integrationKey, true);
        List<BitbucketServerRepository> bitBucketRepoList = client.searchRepositories(repoName.toLowerCase())
                .skip(pageNumber * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(bitBucketRepoList)) {
            return List.of();
        }

        List<ScmRepository> repoList = Lists.newArrayList();
        bitBucketRepoList.forEach(bitbucketRepo -> {
            ScmRepository repo = ScmRepository.builder()
                    .name(bitbucketRepo.getName())
                    .description(bitbucketRepo.getDescription())
                    .url(client.resourceUrl + "/projects/" + bitbucketRepo.getProject().getKey() + "/repos/" + bitbucketRepo.getName() + "/browse")
                    .build();
            repoList.add(repo);
        });

        return repoList;
    }
}
