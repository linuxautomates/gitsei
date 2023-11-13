package io.levelops.integrations.tenable.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.tenable.client.TenableClient;
import io.levelops.integrations.tenable.client.TenableClientException;
import io.levelops.integrations.tenable.client.TenableClientFactory;
import io.levelops.integrations.tenable.models.ScannerPoolResponse;
import io.levelops.integrations.tenable.models.TenableMetadataQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link TenableScannerPoolDataSource}
 */
public class TenableScannerPoolResponseDataSourceTest {
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    TenableScannerPoolDataSource dataSource;

    @Before
    public void setup() throws TenableClientException {
        TenableClient tenableClient = Mockito.mock(TenableClient.class);
        TenableClientFactory tenableClientFactory = Mockito.mock(TenableClientFactory.class);
        dataSource = new TenableScannerPoolDataSource(tenableClientFactory);
        when(tenableClientFactory.get(TEST_KEY)).thenReturn(tenableClient);
        List<ScannerPoolResponse.ScannerPool> scannerPools = List.of(
                ScannerPoolResponse.ScannerPool.builder().id(1).build(),
                ScannerPoolResponse.ScannerPool.builder().id(2).build(),
                ScannerPoolResponse.ScannerPool.builder().id(3).build(),
                ScannerPoolResponse.ScannerPool.builder().id(4).build(),
                ScannerPoolResponse.ScannerPool.builder().id(5).build());
        ScannerPoolResponse scannerPoolResponse = ScannerPoolResponse.builder().scannerPools(scannerPools).build();
        when(tenableClient.getScannerPools()).thenReturn(scannerPoolResponse);
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(TenableMetadataQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<ScannerPoolResponse.ScannerPool>> scannerPools = dataSource.fetchMany(
                TenableMetadataQuery.builder().integrationKey(TEST_KEY).build()).collect(Collectors.toList());
        assertThat(scannerPools).hasSize(5);
    }
}
