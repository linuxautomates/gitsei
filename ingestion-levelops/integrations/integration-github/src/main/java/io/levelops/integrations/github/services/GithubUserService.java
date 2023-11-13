package io.levelops.integrations.github.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.models.GithubUser;

import java.util.stream.Stream;

public class GithubUserService {
    private final GithubClientFactory levelOpsClientFactory;

    public GithubUserService(GithubClientFactory levelOpsClientFactory) {
        this.levelOpsClientFactory = levelOpsClientFactory;
    }

    public Stream<GithubUser> streamUsers(IntegrationKey integrationKey, String organization) throws GithubClientException {
        return levelOpsClientFactory.get(integrationKey, false).streamOrgUsers(organization, true);
    }
}
