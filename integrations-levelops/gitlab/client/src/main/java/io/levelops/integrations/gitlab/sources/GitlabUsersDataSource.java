package io.levelops.integrations.gitlab.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.client.GitlabClientException;
import io.levelops.integrations.gitlab.client.GitlabClientFactory;
import io.levelops.integrations.gitlab.models.GitlabUser;
import lombok.Builder;
import lombok.Value;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.stream.Stream;

public class GitlabUsersDataSource implements DataSource<GitlabUser, GitlabUsersDataSource.GitlabUserQuery> {
    private final GitlabClientFactory clientFactory;

    public GitlabUsersDataSource(GitlabClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Data<GitlabUser> fetchOne(GitlabUserQuery query) throws FetchException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<GitlabUser>> fetchMany(GitlabUserQuery query) throws FetchException {
        IntegrationKey integrationKey = query.getIntegrationKey();
        Date createdAfter = query.getFrom();
        Date createdBefore = query.getTo();
        GitlabClient client = getClient(integrationKey);

        return client.streamUsers(createdAfter, createdBefore, 100)
                .map(BasicData.mapper(GitlabUser.class));
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

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GitlabUsersDataSource.GitlabUserQuery.GitlabUserQueryBuilder.class)
    public static class GitlabUserQuery implements IntegrationQuery {
        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @Nullable
        @JsonProperty("from")
        Date from;

        @Nullable
        @JsonProperty("to")
        Date to;
    }
}
