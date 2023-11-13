package io.levelops.integrations.tenable.sources;

import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.tenable.client.TenableClient;
import io.levelops.integrations.tenable.client.TenableClientException;
import io.levelops.integrations.tenable.client.TenableClientFactory;
import io.levelops.integrations.tenable.models.ScannerResponse;
import io.levelops.integrations.tenable.models.TenableMetadataQuery;
import lombok.extern.log4j.Log4j2;

import java.util.stream.Stream;

/**
 * Tenable's implementation of the {@link DataSource}. This class is used for fetching scanners detail from tenable.
 */
@Log4j2
public class TenableScannerDataSource implements DataSource<ScannerResponse.Scanner, TenableMetadataQuery> {

    private final TenableClientFactory tenableClientFactory;

    public TenableScannerDataSource(TenableClientFactory tenableClientFactory) {
        this.tenableClientFactory = tenableClientFactory;
    }

    @Override
    public Data<ScannerResponse.Scanner> fetchOne(TenableMetadataQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<ScannerResponse.Scanner>> fetchMany(TenableMetadataQuery query) throws FetchException {
        TenableClient tenableClient = tenableClientFactory.get(query.getIntegrationKey());
        return getScanners(tenableClient);
    }

    private Stream<Data<ScannerResponse.Scanner>> getScanners(TenableClient tenableClient) throws TenableClientException {
        ScannerResponse scannerResp = tenableClient.getScanners();
        return scannerResp.getScanners().stream().map(BasicData.mapper(ScannerResponse.Scanner.class));
    }
}
