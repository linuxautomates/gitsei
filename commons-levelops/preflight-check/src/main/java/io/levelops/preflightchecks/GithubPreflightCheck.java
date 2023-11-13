package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.utils.MapUtils;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.models.GithubOrganization;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import io.levelops.models.PreflightCheckResults.PreflightCheckResultsBuilder;
import io.levelops.models.ScmRepository;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Component
public class GithubPreflightCheck implements PreflightCheck, ScmRepositoryService {

    @Getter
    private final String integrationType = "github";
    private final GithubClientFactory clientFactory;

    @Autowired
    public GithubPreflightCheck(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.clientFactory = new GithubClientFactory(inventoryService, objectMapper, okHttpClient, 0);
    }

    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .tenantId(tenantId)
                .integration(integration)
                .allChecksMustPass();

        GithubClient client;
        try {
            client = clientFactory.buildFromToken(tenantId, integration, token, true);
        } catch (InventoryException | NullPointerException e) {
            return builder
                    .success(false)
                    .exception(e.toString())
                    .build();
        }

        builder.check(checkRepositories(integration, client));

        return builder.build();
    }

    private boolean isGithubApp(Integration integration) {
        Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());
        return StringUtils.isNotBlank((String) metadata.get("app_id"));
    }

    private PreflightCheckResult checkRepositories(Integration integration, GithubClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get repositories")
                .success(true);
        try {
            Optional<GithubRepository> repoOpt;
            if (isGithubApp(integration)) {
                repoOpt = IterableUtils.getFirst(client.getInstallationRepositories(0, 1).getRepositories());
            } else {
                Optional<GithubOrganization> orgOpt = client.streamOrganizations().findFirst();
                if (orgOpt.isEmpty()) {
                    return check.success(false).error("No organization found. Access to a least 1 organization is required.").build();
                }
                Stream<GithubRepository> repos = client.streamRepositories(orgOpt.get().getLogin());
                repoOpt = repos.findFirst();
            }
            if (repoOpt.isEmpty()) {
                check.success(false).error("No repositories found. Access to at least 1 repository is required.");
            }
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    @Override
    public int getTotalRepositoriesCount(String company, Integration integration, String repoName, String projectKey) throws GithubClientException, FetchException {

        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(company)
                .integrationId(integration.getId())
                .build();

        GithubClient client = clientFactory.get(integrationKey, true);

        if (isGithubApp(integration)) {
            if (StringUtils.isEmpty(repoName)) {
                return client.getInstallationRepositories(0, 1).getTotalCount();
            } else {
                return (int) client.streamInstallationRepositories()
                        .filter(repo -> repo.getName().toLowerCase().contains(repoName.toLowerCase()))
                        .count();
            }
        } else {
            List<String> organizations = getOrganizations(integrationKey);
            if (StringUtils.isNotEmpty(repoName)) {
                return organizations.stream()
                        .map(org -> client.searchRepositories(org, repoName).count())
                        .mapToInt(Long::intValue)
                        .sum();
            }

            return organizations.stream()
                    .map(org -> client.streamRepositories(org).count())
                    .mapToInt(Long::intValue)
                    .sum();
        }
    }

    private List<String> getOrganizations(IntegrationKey integrationKey) throws GithubClientException {
        return clientFactory.get(integrationKey, true).streamOrganizations()
                .map(GithubOrganization::getLogin)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScmRepository> getScmRepositories(String company, Integration integration, List<String> filterRepos, int pageNumber, int pageSize) throws Exception {

        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(company)
                .integrationId(integration.getId())
                .build();

        GithubClient client = clientFactory.get(integrationKey, true);

        Stream<GithubRepository> repoStream;
        if (isGithubApp(integration)) {
            repoStream = client.streamInstallationRepositories();
        } else {
            repoStream = getOrganizations(integrationKey).stream()
                    .flatMap(client::streamRepositories);
        }

        return repoStream
                .filter(repo -> CollectionUtils.isEmpty(filterRepos) || filterRepos.contains(repo.getFullName()))
                .skip((long) pageNumber * pageSize)
                .limit(pageSize)
                .map(GithubPreflightCheck::convertToScmRepo)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScmRepository> searchScmRepository(String company, Integration integration, String repoName, String projectKey, int pageNumber, int pageSize) throws Exception {

        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(company)
                .integrationId(integration.getId())
                .build();

        GithubClient client = clientFactory.get(integrationKey, true);

        Stream<GithubRepository> repoStream;
        if (isGithubApp(integration)) {
            repoStream = client.streamInstallationRepositories()
                    .filter(repo -> repo.getName().toLowerCase().contains(repoName.toLowerCase()));
        } else {
            List<String> organizations = getOrganizations(integrationKey);
            repoStream = organizations.stream()
                    .flatMap(org -> client.searchRepositories(org, repoName));
        }

        return repoStream
                .skip((long) pageNumber * pageSize)
                .limit(pageSize)
                .map(GithubPreflightCheck::convertToScmRepo)
                .collect(Collectors.toList());
    }

    private static ScmRepository convertToScmRepo(GithubRepository githubRepo) {
        return ScmRepository.builder()
                .name(githubRepo.getFullName())
                .description(githubRepo.getDescription())
                .url(githubRepo.getHtmlUrl())
                .updatedAt(githubRepo.getPushedAt().toInstant().getEpochSecond())
                .build();
    }


}
