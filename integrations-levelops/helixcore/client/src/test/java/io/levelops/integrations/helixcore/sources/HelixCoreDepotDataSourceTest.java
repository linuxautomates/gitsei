package io.levelops.integrations.helixcore.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.helixcore.client.HelixCoreClient;
import io.levelops.integrations.helixcore.client.HelixCoreClientException;
import io.levelops.integrations.helixcore.client.HelixCoreClientFactory;
import io.levelops.integrations.helixcore.models.HelixCoreDepot;
import io.levelops.integrations.helixcore.models.HelixCoreIterativeQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class HelixCoreDepotDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    HelixCoreDepotDataSource dataSource;

    @Before
    public void setup() throws HelixCoreClientException {
        HelixCoreClient helixcoreClient = Mockito.mock(HelixCoreClient.class);
        HelixCoreClientFactory helixcoreClientFactory = Mockito.mock(HelixCoreClientFactory.class);
        dataSource = new HelixCoreDepotDataSource(helixcoreClientFactory);
        when(helixcoreClientFactory.get(TEST_KEY)).thenReturn(helixcoreClient);
        List<HelixCoreDepot> depots = List.of(
                HelixCoreDepot.builder().name("name").build(),
                HelixCoreDepot.builder().name("name").build(),
                HelixCoreDepot.builder().name("name").build(),
                HelixCoreDepot.builder().name("name").build(),
                HelixCoreDepot.builder().name("name").build()
        );
        when(helixcoreClient.getDepots()).thenReturn(depots.stream());
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(HelixCoreIterativeQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<HelixCoreDepot>> accounts = dataSource.fetchMany(
                HelixCoreIterativeQuery.builder().integrationKey(TEST_KEY).build()).collect(Collectors.toList());
        assertThat(accounts).hasSize(5);
    }
}
