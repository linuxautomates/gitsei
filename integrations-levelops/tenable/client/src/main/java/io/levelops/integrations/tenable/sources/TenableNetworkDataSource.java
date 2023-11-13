package io.levelops.integrations.tenable.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.tenable.client.TenableClient;
import io.levelops.integrations.tenable.client.TenableClientException;
import io.levelops.integrations.tenable.client.TenableClientFactory;
import io.levelops.integrations.tenable.models.NetworkResponse;
import io.levelops.integrations.tenable.models.TenableMetadataQuery;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tenable's implementation of the {@link DataSource}. This class is used for fetching network details from tenable.
 */
@Log4j2
public class TenableNetworkDataSource implements DataSource<NetworkResponse.Network, TenableMetadataQuery> {

    private final TenableClientFactory tenableClientFactory;
    private final Integer PAGE_LIMIT = 50;

    public TenableNetworkDataSource(TenableClientFactory tenableClientFactory) {
        this.tenableClientFactory = tenableClientFactory;
    }

    @Override
    public Data<NetworkResponse.Network> fetchOne(TenableMetadataQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<NetworkResponse.Network>> fetchMany(TenableMetadataQuery query) throws FetchException {
        TenableClient tenableClient = tenableClientFactory.get(query.getIntegrationKey());
        return PaginationUtils.stream(0, PAGE_LIMIT, offset -> {
            try {
                return getNetworks(tenableClient, offset, PAGE_LIMIT);
            } catch (TenableClientException e) {
                log.error("Encountered tenable client error for integration key: "
                        + query.getIntegrationKey() + " as : " + e.getMessage(), e);
                throw new RuntimeStreamException("Encountered tenable client error for integration key: " + query.getIntegrationKey(), e);
            }
        });
    }

    private List<Data<NetworkResponse.Network>> getNetworks(TenableClient tenableClient, Integer offset,
                                                            Integer limit) throws TenableClientException {
        NetworkResponse networkResponse = tenableClient.getNetworks(offset, limit);
        return networkResponse.getNetworks().stream()
                .map(BasicData.mapper(NetworkResponse.Network.class))
                .collect(Collectors.toList());
    }
}
