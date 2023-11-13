package io.levelops.integrations.coverity.sources;

import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.coverity.client.CoverityClient;
import io.levelops.integrations.coverity.client.CoverityClientFactory;
import io.levelops.integrations.coverity.models.CoverityIterativeScanQuery;
import io.levelops.integrations.coverity.models.EnrichedProjectData;
import io.levelops.integrations.coverity.services.CoverityDefectsFetchService;
import org.apache.commons.lang3.Validate;

import java.util.stream.Stream;

public class CoverityMergedDefectDataSource implements DataSource<EnrichedProjectData, CoverityIterativeScanQuery> {

    private final CoverityClientFactory clientFactory;
    private final CoverityDefectsFetchService fetchDefectsService;

    public CoverityMergedDefectDataSource(CoverityClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        this.fetchDefectsService = new CoverityDefectsFetchService();
    }

    @Override
    public Data<EnrichedProjectData> fetchOne(CoverityIterativeScanQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<EnrichedProjectData>> fetchMany(CoverityIterativeScanQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        CoverityClient coverityClient = clientFactory.get(query.getIntegrationKey());
        Stream<EnrichedProjectData> dataStream = fetchDefectsService.fetch(coverityClient, query);
        return dataStream.map(BasicData.mapper(EnrichedProjectData.class));
    }
}
