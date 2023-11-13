package io.levelops.integrations.splunk.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.splunk.client.SplunkClientException;
import io.levelops.integrations.splunk.client.SplunkClientFactory;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

import java.util.stream.Stream;

@Log4j2
public class SplunkSearchDataSource implements DataSource<String, SplunkSearchDataSource.SplunkSearchQuery> {
    private final SplunkClientFactory clientFactory;

    public SplunkSearchDataSource(SplunkClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public String fetchOne(IntegrationKey integrationKey) throws FetchException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Data<String> fetchOne(SplunkSearchQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        String data = fetchOne(query.getIntegrationKey());
        return BasicData.of(String.class, data);
    }

    @Override
    public Stream<Data<String>> fetchMany(SplunkSearchQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        log.info("query = {}", query);

        try {
            Stream<String> results = clientFactory.get(query.getIntegrationKey()).search(query.getQuery());
            return query.getLimit() == null ? results.map(BasicData.mapper(String.class)) :
                    results.limit(query.getLimit()).map(BasicData.mapper(String.class));
        } catch (SplunkClientException e) {
            throw new FetchException("Could not fetch Splunk Search", e);
        }
    }

    @Value
    @Builder
    @JsonDeserialize(builder = SplunkSearchQuery.SplunkSearchQueryBuilder.class)
    public static class SplunkSearchQuery implements IntegrationQuery {
        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("query")
        String query;

        @JsonProperty("limit")
        Integer limit;
    }
}
