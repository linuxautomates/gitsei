package io.levelops.integrations.gitlab.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.data.IngestionData;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.client.GitlabClientException;
import io.levelops.integrations.gitlab.client.GitlabClientFactory;
import io.levelops.integrations.gitlab.models.GitlabGroup;
import io.levelops.integrations.gitlab.models.GitlabQuery;
import io.levelops.integrations.gitlab.services.GitlabFetchGroupsService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Gitlab's implementation of the {@link DataSource}. This class can be used to fetch {@link GitlabGroup}
 * data from Gitlab.
 */
@Log4j2
public class GitlabGroupDataSource implements DataSource<GitlabGroup, GitlabQuery> {
    private final GitlabFetchGroupsService fetchGroupsService;
    private final GitlabClientFactory clientFactory;


    public GitlabGroupDataSource(GitlabClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        fetchGroupsService = new GitlabFetchGroupsService();
    }

    private GitlabClient getClient(IntegrationKey integrationKey) throws FetchException {
        GitlabClient client;
        try {
            client = clientFactory.get(integrationKey, false);
        } catch (GitlabClientException e) {
            throw new FetchException("Could not fetch Gitlab client", e);
        }
        return client;
    }

    @Override
    public Data<GitlabGroup> fetchOne(GitlabQuery query) throws FetchException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Data<GitlabGroup>> fetchMany(GitlabQuery query) throws FetchException {
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        IntegrationKey integrationKey = query.getIntegrationKey();
        GitlabClient client = getClient(integrationKey);
        int DEFAULT_PER_PAGE = 100;
        Stream<Data<GitlabGroup>> stream = fetchGroupsService.fetchGroups(client, DEFAULT_PER_PAGE)
                .filter(Objects::nonNull)
                .map(result -> IngestionData.of(GitlabGroup.class, result.getData(), result.getIngestionFailures()));
        return stream.filter(Objects::nonNull);
    }
}
