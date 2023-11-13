package io.levelops.integrations.droneci.sources;


import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.droneci.client.DroneCIClient;
import io.levelops.integrations.droneci.client.DroneCIClientException;
import io.levelops.integrations.droneci.client.DroneCIClientFactory;
import io.levelops.integrations.droneci.models.DroneCIEnrichRepoData;
import io.levelops.integrations.droneci.models.DroneCIIngestionQuery;
import io.levelops.integrations.droneci.source.DroneCIEnrichRepoDataSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class DroneCIEnrichRepoDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder()
            .integrationId(EMPTY).tenantId(EMPTY).build();

    DroneCIEnrichRepoDataSource dataSource;

    @Before
    public void setup() throws DroneCIClientException {
        DroneCIClient client = Mockito.mock(DroneCIClient.class);
        DroneCIClientFactory clientFactory = Mockito.mock(DroneCIClientFactory.class);
        dataSource = new DroneCIEnrichRepoDataSource(clientFactory);
        when(clientFactory.get(TEST_KEY)).thenReturn(client);
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(DroneCIIngestionQuery.builder().integrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<DroneCIEnrichRepoData>> repositoriesData = dataSource.fetchMany(DroneCIIngestionQuery.builder()
                        .integrationKey(TEST_KEY)
                        .from(null)
                        .build())
                .collect(Collectors.toList());
        assertThat(repositoriesData).hasSize(0);
    }
}