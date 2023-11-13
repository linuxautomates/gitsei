package io.levelops.integrations.tenable.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.tenable.client.TenableClient;
import io.levelops.integrations.tenable.client.TenableClientException;
import io.levelops.integrations.tenable.client.TenableClientFactory;
import io.levelops.integrations.tenable.models.TenableScanQuery;
import io.levelops.integrations.tenable.models.WASResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link TenableWASDataSource}
 */
public class TenableWasResponseDataSourceTest {
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    TenableWASDataSource dataSource;

    @Before
    public void setup() throws TenableClientException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        TenableClient tenableClient = Mockito.mock(TenableClient.class);
        TenableClientFactory tenableClientFactory = Mockito.mock(TenableClientFactory.class);
        dataSource = new TenableWASDataSource(tenableClientFactory);
        when(tenableClientFactory.get(TEST_KEY)).thenReturn(tenableClient);
        List<WASResponse.Data> wasDataList = List.of(
                WASResponse.Data.builder().vulnId("1").createdAt(dateFormat.format(new Date())).build(),
                WASResponse.Data.builder().vulnId("2").createdAt(dateFormat.format(new Date())).build(),
                WASResponse.Data.builder().vulnId("3").createdAt(dateFormat.format(new Date())).build(),
                WASResponse.Data.builder().vulnId("4").createdAt(dateFormat.format(new Date())).build(),
                WASResponse.Data.builder().vulnId("5").createdAt(dateFormat.format(new Date())).build());
        WASResponse wasResponse = WASResponse.builder().data(wasDataList).build();
        when(tenableClient.getWasResponse(0, 50, "desc"))
                .thenReturn(wasResponse);
        when(tenableClient.getWasResponse(1, 50, "desc"))
                .thenReturn(WASResponse.builder().data(new ArrayList<>()).build());
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(TenableScanQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<WASResponse.Data>> wasVulns = dataSource.fetchMany(TenableScanQuery.builder()
                .since(Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond())
                .integrationKey(TEST_KEY).build()).collect(Collectors.toList());
        assertThat(wasVulns).hasSize(5);
    }


}
