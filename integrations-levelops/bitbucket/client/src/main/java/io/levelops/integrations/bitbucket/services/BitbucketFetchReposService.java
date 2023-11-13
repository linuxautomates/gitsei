package io.levelops.integrations.bitbucket.services;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.integrations.bitbucket.client.BitbucketClient;
import io.levelops.integrations.bitbucket.client.BitbucketClientException;
import io.levelops.integrations.bitbucket.models.BitbucketRepository;
import io.levelops.integrations.bitbucket.models.BitbucketWorkspace;
import lombok.extern.log4j.Log4j2;

import java.util.stream.Stream;

@Log4j2
public class BitbucketFetchReposService {
    public Stream<BitbucketRepository> fetchRepos(BitbucketClient client) throws BitbucketClientException {
        return client.streamWorkspaces()
                .map(BitbucketWorkspace::getSlug)
                .flatMap(workspaceSlug -> {
                    try {
                        return client.streamRepositories(workspaceSlug);
                    } catch (RuntimeStreamException e) {
                        log.warn("Failed to extract repositories from workspace={}", workspaceSlug, e);
                        return Stream.empty();
                    }
                });
    }
}
