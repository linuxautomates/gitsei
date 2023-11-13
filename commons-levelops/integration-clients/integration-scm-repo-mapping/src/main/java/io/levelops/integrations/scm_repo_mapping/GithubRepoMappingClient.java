package io.levelops.integrations.scm_repo_mapping;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.model.GithubContributor;
import io.levelops.integrations.github.models.GithubOrganization;
import io.levelops.integrations.github.models.GithubRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Log4j2
public class GithubRepoMappingClient implements RepoMappingClient {
    private final GithubClientFactory githubClientFactory;

    public GithubRepoMappingClient(GithubClientFactory githubClientFactory) {
        this.githubClientFactory = githubClientFactory;
    }

    private GithubClient getGithubClient(String tenantId, String integrationId) {
        try {
            return githubClientFactory.get(IntegrationKey.builder()
                    .tenantId(tenantId)
                    .integrationId(integrationId)
                    .build(), false);
        } catch (GithubClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getIntegrationType() {
        return "github";
    }

    @Override
    public List<String> getReposForUser(
            String tenantId,
            String integrationId,
            String username,
            List<String> orgs) {
        GithubClient client = getGithubClient(tenantId, integrationId);
        Set<String> orgLogins = new HashSet<>(orgs);
        return client.streamUserRecentRepos(username, true)
                .filter(Objects::nonNull)
                .filter(repo -> orgLogins.contains(repo.getOwner().getLogin()))
                .map(GithubRepository::getFullName)
                .collect(Collectors.toList());
    }

    // TODO: This won't work for GHS (github app) like Equifax, see the else if:
    //io.levelops.integrations.github.sources.GithubRepositoryDataSource#getRepositoryStream
    //(ingestion). We need to get the orgs from the app if this integration is a github app.
//    public List<String> getAllRepos(String tenantId, String integrationId) {
//        GithubClient client = getGithubClient(tenantId, integrationId);
//
//        AtomicInteger orgCount = new AtomicInteger();
//        List<String> allRepos = client.streamOrganizations().map(org -> {
//                    orgCount.getAndIncrement();
//                    List<GithubRepository> repos = List.of();
//                    try {
//                        repos = client.streamRepositories(org.getLogin()).collect(Collectors.toList());
//                        log.info("Found {} repos for org: {}", repos.size(), org);
//                        return repos;
//                    } catch (Exception e) {
//                        log.error("Error getting repositories for org: {}", org, e);
//                    }
//                    return repos;
//                }).flatMap(List::stream)
//                .map(GithubRepository::getFullName).collect(Collectors.toList());
//        log.info("Tenant {}, total repo count: {}, total org count: {}", tenantId, allRepos.size(), orgCount.get());
//        return allRepos;
//    }

    public List<String> getRepoContributors(String tenantId, String integrationId, String repoName) {
        return getGithubClient(tenantId, integrationId).streamRepoContributors(repoName)
                .map(GithubContributor::getLogin)
                .collect(Collectors.toList());
    }

}
