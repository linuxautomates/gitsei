package io.levelops.integrations.bitbucket_server.services;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClient;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientException;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerBranchInfo;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerEnrichedProjectData;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerProject;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Log4j2
public class BitbucketServerFetchReposService {

    public Stream<BitbucketServerEnrichedProjectData> fetchRepos(BitbucketServerClient client, List<String> repos) throws BitbucketServerClientException {
        return Objects.requireNonNull(client.streamProjects())
                .map(BitbucketServerProject::getKey)
                .flatMap(projectKey -> {
                    try {
                        return client.streamRepositories(projectKey)
                                .filter(repository -> {
                                    if (CollectionUtils.isNotEmpty(repos)) {
                                        return repos.contains(repository.getName());
                                    } else {
                                        return true;
                                    }
                                })
                                .map(repository -> enrichRepository(client, repository))
                                .map(repository -> BitbucketServerEnrichedProjectData.builder()
                                        .repository(repository).build());
                    } catch (RuntimeStreamException | BitbucketServerClientException e) {
                        log.warn("Failed to extract repositories from project={}", projectKey, e);
                        return Stream.empty();
                    }
                });

    }

    public Stream<BitbucketServerEnrichedProjectData> fetchRepos(BitbucketServerClient client) throws BitbucketServerClientException {
        return Objects.requireNonNull(client.streamProjects())
                .map(BitbucketServerProject::getKey)
                .flatMap(projectKey -> {
                    try {
                        return client.streamRepositories(projectKey)
                                .map(repository -> enrichRepository(client, repository))
                                .map(repository -> BitbucketServerEnrichedProjectData.builder()
                                        .repository(repository).build());
                    } catch (RuntimeStreamException | BitbucketServerClientException e) {
                        log.warn("Failed to extract repositories from project={}", projectKey, e);
                        return Stream.empty();
                    }
                });
    }

    private BitbucketServerRepository enrichRepository(BitbucketServerClient client,
                                                       BitbucketServerRepository repository) {
        String defaultBranch = null;
        try {
            BitbucketServerBranchInfo defaultBranchInfo = client.getDefaultBranch(repository.getProject().getKey(), repository.getSlug());
            if (defaultBranchInfo != null) {
                defaultBranch = defaultBranchInfo.getDisplayId();
            }
        } catch (BitbucketServerClientException e) {
            log.warn("Failed to fetch default branch info for repo {}/{}", repository.getProject().getKey(), repository.getSlug(), e);
            return repository;
        }
        return repository.toBuilder()
                .defaultBranch(defaultBranch)
                .build();
    }
}
