package io.levelops.integrations.postgres.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.postgres.client.PostgresClient;
import io.levelops.integrations.postgres.client.PostgresClientException;
import io.levelops.integrations.postgres.client.PostgresClientFactory;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.Validate;

import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Stream;

public class PostgresQueryDataSource implements DataSource<PostgresClient.Row, PostgresQueryDataSource.PostgresQuery> {
    private final PostgresClientFactory clientFactory;

    public PostgresQueryDataSource(PostgresClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    private PostgresClient.Row fetchOneProject(IntegrationKey integrationKey) throws PostgresClientException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Data<PostgresClient.Row> fetchOne(PostgresQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        try {
            PostgresClient.Row row = fetchOneProject(query.getIntegrationKey());
            return BasicData.of(PostgresClient.Row.class, row);
        } catch (PostgresClientException e) {
            throw new FetchException("Could not fetch Postgres Query Data!", e);
        }
    }

    @Override
    public Stream<Data<PostgresClient.Row>> fetchMany(PostgresQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        try {
            Stream<PostgresClient.Row> data = clientFactory.get(query.getIntegrationKey()).executeQueryStreamResults(query.getQuery());
            return data
                    .filter(Objects::nonNull)
                    .map(BasicData.mapper(PostgresClient.Row.class));
        } catch (PostgresClientException e) {
            throw new FetchException("Could not fetch Postgres Query Data!", e);
        } catch (SQLException e) {
            throw new FetchException("Could not fetch Postgres Query Data!", e);
        }
    }

    @Value
    @Builder
    @JsonDeserialize(builder = PostgresQuery.PostgresQueryBuilder.class)
    public static class PostgresQuery implements IntegrationQuery {
        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("query")
        String query;
    }
}
