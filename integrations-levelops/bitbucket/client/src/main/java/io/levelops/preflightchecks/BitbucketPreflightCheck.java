package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.bitbucket.client.BitbucketClient;
import io.levelops.integrations.bitbucket.client.BitbucketClientException;
import io.levelops.integrations.bitbucket.client.BitbucketClientFactory;
import io.levelops.integrations.bitbucket.models.BitbucketRepository;
import io.levelops.integrations.bitbucket.services.BitbucketFetchReposService;
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

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
public class BitbucketPreflightCheck implements PreflightCheck, ScmRepositoryService {

    private final BitbucketFetchReposService fetchReposService;
    @Getter
    private final String integrationType = "bitbucket";
    private BitbucketClientFactory clientFactory;

    @Autowired
    public BitbucketPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient, InventoryService inventoryService) {
        this.clientFactory = BitbucketClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .inventoryService(inventoryService)
                .build();
        fetchReposService = new BitbucketFetchReposService();
    }

    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        BitbucketClient client;
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
        builder.check(checkRepositories(client));
        return builder.build();
    }

    private PreflightCheckResult checkRepositories(BitbucketClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get repositories")
                .success(true);
        try {
            BitbucketFetchReposService reposService = new BitbucketFetchReposService();
            var repos = client.getWorkspaces(0);
            if (repos.getValues() == null) {
                check.success(false).error("Get repositories did not return any data");
            }

        } catch (Exception e) {
            log.error("Error in checkRepositories!", e);
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    @Override
    public int getTotalRepositoriesCount(String company, Integration integration, String repoName, String projectKey) throws BitbucketClientException {

        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(company)
                .integrationId(integration.getId())
                .build();

        BitbucketClient client = clientFactory.get(integrationKey, true);

        if(StringUtils.isNotEmpty(repoName)){
           return (int) client.searchRepositories(repoName.toLowerCase()).count();
        }

        return (int) fetchReposService.fetchRepos(client).count();
    }

    @Override
    public List<ScmRepository> getScmRepositories(String company, Integration integration, List<String> filterRepos,int pageNumber, int pageSize) throws BitbucketClientException {

        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(company)
                .integrationId(integration.getId())
                .build();

        BitbucketClient client = clientFactory.get(integrationKey, true);
        List<BitbucketRepository> bitBucketRepoList = fetchReposService.fetchRepos(client)
                .filter(i -> CollectionUtils.isEmpty(filterRepos) || filterRepos.contains(i.getFullName()))
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
                    .url(bitbucketRepo.getLinks().getHtml().getHref())
                    .updatedAt(bitbucketRepo.getUpdatedOn().toInstant().getEpochSecond())
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

        BitbucketClient client = clientFactory.get(integrationKey, true);
        List<BitbucketRepository> bitBucketRepoList = client.searchRepositories(repoName.toLowerCase())
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
                    .url(bitbucketRepo.getLinks().getHtml().getHref())
                    .updatedAt(bitbucketRepo.getUpdatedOn().toInstant().getEpochSecond())
                    .build();
            repoList.add(repo);
        });

        return repoList;
    }
}
