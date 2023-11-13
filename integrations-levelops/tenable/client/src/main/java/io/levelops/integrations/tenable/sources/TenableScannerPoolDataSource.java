package io.levelops.integrations.tenable.sources;

import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.tenable.client.TenableClient;
import io.levelops.integrations.tenable.client.TenableClientException;
import io.levelops.integrations.tenable.client.TenableClientFactory;
import io.levelops.integrations.tenable.models.ScannerPoolResponse;
import io.levelops.integrations.tenable.models.TenableMetadataQuery;
import lombok.extern.log4j.Log4j2;

import java.util.stream.Stream;

/**
 * Tenable's implementation of the {@link DataSource}. This class is used for fetching scanner pools detail from tenable.
 */
@Log4j2
public class TenableScannerPoolDataSource implements DataSource<ScannerPoolResponse.ScannerPool, TenableMetadataQuery> {

    private final TenableClientFactory tenableClientFactory;

    public TenableScannerPoolDataSource(TenableClientFactory tenableClientFactory) {
        this.tenableClientFactory = tenableClientFactory;
    }

    @Override
    public Data<ScannerPoolResponse.ScannerPool> fetchOne(TenableMetadataQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<ScannerPoolResponse.ScannerPool>> fetchMany(TenableMetadataQuery query) throws FetchException {
        TenableClient tenableClient = tenableClientFactory.get(query.getIntegrationKey());
        return getScannerPools(tenableClient);
    }

    private Stream<Data<ScannerPoolResponse.ScannerPool>> getScannerPools(TenableClient tenableClient) throws TenableClientException {
        ScannerPoolResponse scannerPoolResponse = tenableClient.getScannerPools();
        return scannerPoolResponse.getScannerPools().stream()
                .map(BasicData.mapper(ScannerPoolResponse.ScannerPool.class));
    }
}
