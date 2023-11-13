package io.levelops.integrations.helixcore.sources;

import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.helixcore.client.HelixCoreClient;
import io.levelops.integrations.helixcore.client.HelixCoreClientFactory;
import io.levelops.integrations.helixcore.models.HelixCoreDepot;
import io.levelops.integrations.helixcore.models.HelixCoreIterativeQuery;
import lombok.extern.log4j.Log4j2;

import java.util.stream.Stream;

/**
 * Helix core's implementation of the {@link DataSource}. This class can be used to fetch data from Helix core server.
 */
@Log4j2
public class HelixCoreDepotDataSource implements DataSource<HelixCoreDepot, HelixCoreIterativeQuery> {

    private final HelixCoreClientFactory helixcoreClientFactory;

    /**
     * all arg constructor
     *
     * @param helixcoreClientFactory {@link HelixCoreClientFactory} for fetching the {@link HelixCoreClient}
     */
    public HelixCoreDepotDataSource(HelixCoreClientFactory helixcoreClientFactory) {
        this.helixcoreClientFactory = helixcoreClientFactory;
    }

    @Override
    public Data<HelixCoreDepot> fetchOne(HelixCoreIterativeQuery query) throws FetchException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<HelixCoreDepot>> fetchMany(HelixCoreIterativeQuery query) throws FetchException {
        HelixCoreClient helixcoreClient = helixcoreClientFactory.get(query.getIntegrationKey());
        return helixcoreClient.getDepots().map(BasicData.mapper(HelixCoreDepot.class));
    }

}
