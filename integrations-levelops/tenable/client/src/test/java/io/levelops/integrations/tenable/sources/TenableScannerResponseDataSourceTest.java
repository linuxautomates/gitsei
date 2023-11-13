package io.levelops.integrations.tenable.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.tenable.client.TenableClient;
import io.levelops.integrations.tenable.client.TenableClientException;
import io.levelops.integrations.tenable.client.TenableClientFactory;
import io.levelops.integrations.tenable.models.ScannerResponse;
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
 * Unit test for {@link TenableScannerResponseDataSourceTest}
 */
public class TenableScannerResponseDataSourceTest {
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    TenableScannerDataSource dataSource;

    @Before
    public void setup() throws TenableClientException {
        TenableClient tenableClient = Mockito.mock(TenableClient.class);
        TenableClientFactory tenableClientFactory = Mockito.mock(TenableClientFactory.class);
        dataSource = new TenableScannerDataSource(tenableClientFactory);
        when(tenableClientFactory.get(TEST_KEY)).thenReturn(tenableClient);

        List<ScannerResponse.Scanner> scanners = List.of(
                ScannerResponse.Scanner.builder().id(1).build(),
                ScannerResponse.Scanner.builder().id(2).build(),
                ScannerResponse.Scanner.builder().id(3).build(),
                ScannerResponse.Scanner.builder().id(4).build(),
                ScannerResponse.Scanner.builder().id(5).build());
        ScannerResponse scannerResponse = ScannerResponse.builder().scanners(scanners).build();
        when(tenableClient.getScanners()).thenReturn(scannerResponse);
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(TenableMetadataQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<ScannerResponse.Scanner>> scanners = dataSource.fetchMany(
                TenableMetadataQuery.builder().integrationKey(TEST_KEY).build()).collect(Collectors.toList());
        assertThat(scanners).hasSize(5);
    }
}
