package io.levelops.sources;

import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.blackduck.BlackDuckClient;
import io.levelops.integrations.blackduck.BlackDuckClientFactory;
import io.levelops.integrations.blackduck.models.BlackDuckIterativeScanQuery;
import io.levelops.integrations.blackduck.models.EnrichedProjectData;
import io.levelops.services.BlackDuckIssuesFetchService;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Stream;

public class BlackDuckDataSource implements DataSource<EnrichedProjectData,
        BlackDuckIterativeScanQuery> {

    private final BlackDuckClientFactory clientFactory;
    @Autowired
    private final BlackDuckIssuesFetchService fetchProjectsService;

    public BlackDuckDataSource(BlackDuckClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        this.fetchProjectsService = new BlackDuckIssuesFetchService();
    }

    @Override
    public Data<EnrichedProjectData> fetchOne(BlackDuckIterativeScanQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<EnrichedProjectData>> fetchMany(BlackDuckIterativeScanQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        Stream<EnrichedProjectData> dataStream;
        BlackDuckClient blackDuckClient = clientFactory.get(query.getIntegrationKey());
        dataStream = fetchProjectsService.fetch(blackDuckClient, query);
        return dataStream
                .map(BasicData.mapper(EnrichedProjectData.class));
    }
}
