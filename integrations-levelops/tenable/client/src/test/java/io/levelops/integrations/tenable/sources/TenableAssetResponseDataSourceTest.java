package io.levelops.integrations.tenable.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.tenable.client.TenableClient;
import io.levelops.integrations.tenable.client.TenableClientException;
import io.levelops.integrations.tenable.client.TenableClientFactory;
import io.levelops.integrations.tenable.models.Asset;
import io.levelops.integrations.tenable.models.ExportResponse;
import io.levelops.integrations.tenable.models.ExportStatusResponse;
import io.levelops.integrations.tenable.models.TenableScanQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link TenableAssetDataSource}
 */
public class TenableAssetResponseDataSourceTest {
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    TenableAssetDataSource dataSource;

    @Before
    public void setup() throws TenableClientException {
        TenableClient tenableClient = Mockito.mock(TenableClient.class);
        TenableClientFactory tenableClientFactory = Mockito.mock(TenableClientFactory.class);
        dataSource = new TenableAssetDataSource(tenableClientFactory);
        when(tenableClientFactory.get(TEST_KEY)).thenReturn(tenableClient);
        when(tenableClient.exportAssets(eq(TenableScanQuery.builder().integrationKey(TEST_KEY).build())))
                .thenReturn(ExportResponse.builder().exportUUID("1234").build());
        List<Integer> chunksAvailable = new ArrayList<>();
        chunksAvailable.add(1);
        when(tenableClient.getAssetsExportStatus("1234"))
                .thenReturn(ExportStatusResponse.builder()
                        .status("Finished")
                        .chunksAvailable(chunksAvailable)
                        .build());
        Asset asset11 = Asset.builder().id("11").build();
        Asset asset21 = Asset.builder().id("21").build();
        Asset asset31 = Asset.builder().id("31").build();
        List<Asset> assets = new ArrayList<>();
        assets.add(asset11);
        assets.add(asset21);
        assets.add(asset31);
        when(tenableClient.downloadAssetChunk("1234", 1))
                .thenReturn(assets);
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(TenableScanQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<Asset>> assets = dataSource.fetchMany(TenableScanQuery.builder()
                .integrationKey(TEST_KEY).build()).collect(Collectors.toList());
        assertThat(assets).hasSize(3);
    }
}
