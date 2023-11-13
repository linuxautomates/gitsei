package io.levelops.integrations.confluence.sources;

import com.google.common.base.MoreObjects;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.confluence.client.ConfluenceClientException;
import io.levelops.integrations.confluence.client.ConfluenceClientFactory;
import io.levelops.integrations.confluence.client.ConfluenceCqlBuilder;
import io.levelops.integrations.confluence.models.ConfluenceSearchQuery;
import io.levelops.integrations.confluence.models.ConfluenceSearchResponse;
import io.levelops.integrations.confluence.models.ConfluenceSearchResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import java.util.stream.Stream;

import static io.levelops.integrations.confluence.client.ConfluenceCqlBuilder.FIELD_LAST_MODIFIED;
import static io.levelops.integrations.confluence.client.ConfluenceCqlBuilder.FIELD_TEXT;

@Log4j2
public class ConfluenceSearchDataSource implements DataSource<ConfluenceSearchResult, ConfluenceSearchQuery> {

    private static final int DEFAULT_DAYS_TO_QUERY = 30;
    private final ConfluenceClientFactory clientFactory;

    @Builder
    public ConfluenceSearchDataSource(ConfluenceClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    private ConfluenceSearchResponse search(ConfluenceSearchQuery query) throws FetchException {
        IntegrationKey integrationKey = query.getIntegrationKey();
        String cql = ConfluenceCqlBuilder.builder()
                .containsAny(FIELD_TEXT, query.getKeywords())
                .and()
                .sinceDays(FIELD_LAST_MODIFIED, MoreObjects.firstNonNull(query.getSinceDays(), DEFAULT_DAYS_TO_QUERY))
                .orderByDesc(FIELD_LAST_MODIFIED)
                .build();
        try {
            return clientFactory.get(integrationKey).search(cql, query.getSkip(), query.getLimit());
        } catch (ConfluenceClientException e) {
            throw new FetchException(e);
        }
    }

    @Override
    public Data<ConfluenceSearchResult> fetchOne(ConfluenceSearchQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        ConfluenceSearchResponse searchResponse = search(query);
        if (CollectionUtils.isEmpty(searchResponse.getResults())) {
            return BasicData.empty(ConfluenceSearchResult.class);
        }
        return BasicData.of(ConfluenceSearchResult.class, searchResponse.getResults().get(0));
    }

    @Override
    public Stream<Data<ConfluenceSearchResult>> fetchMany(ConfluenceSearchQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        ConfluenceSearchResponse searchResponse = search(query);
        return searchResponse.getResults().stream()
                .map(BasicData.mapper(ConfluenceSearchResult.class));
    }


}
