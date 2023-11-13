package io.levelops.integrations.tenable.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.tenable.client.TenableClient;
import io.levelops.integrations.tenable.client.TenableClientException;
import io.levelops.integrations.tenable.client.TenableClientFactory;
import io.levelops.integrations.tenable.models.NetworkResponse;
import io.levelops.integrations.tenable.models.TenableMetadataQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link TenableNetworkDataSource}
 */
public class TenableNetworkResponseDataSourceTest {
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    TenableNetworkDataSource dataSource;

    @Before
    public void setup() throws TenableClientException {
        TenableClient tenableClient = Mockito.mock(TenableClient.class);
        TenableClientFactory tenableClientFactory = Mockito.mock(TenableClientFactory.class);
        dataSource = new TenableNetworkDataSource(tenableClientFactory);
        when(tenableClientFactory.get(TEST_KEY)).thenReturn(tenableClient);
        List<NetworkResponse.Network> networks = List.of(
                NetworkResponse.Network.builder().uuid("1").build(),
                NetworkResponse.Network.builder().uuid("2").build(),
                NetworkResponse.Network.builder().uuid("3").build(),
                NetworkResponse.Network.builder().uuid("4").build(),
                NetworkResponse.Network.builder().uuid("5").build());
        NetworkResponse networkResponse = NetworkResponse.builder().networks(networks).build();
        when(tenableClient.getNetworks(0, 50)).thenReturn(networkResponse);
        when(tenableClient.getNetworks(50, 50)).thenReturn(NetworkResponse.builder()
                .networks(new ArrayList<>()).build());
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(TenableMetadataQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<NetworkResponse.Network>> networks = dataSource.fetchMany(
                TenableMetadataQuery.builder().integrationKey(TEST_KEY).build()).collect(Collectors.toList());
        assertThat(networks).hasSize(5);
    }
}
