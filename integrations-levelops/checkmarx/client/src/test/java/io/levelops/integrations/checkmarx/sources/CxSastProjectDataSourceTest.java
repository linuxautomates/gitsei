package io.levelops.integrations.checkmarx.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.checkmarx.client.cxsast.CxSastClient;
import io.levelops.integrations.checkmarx.client.cxsast.CxSastClientException;
import io.levelops.integrations.checkmarx.client.cxsast.CxSastClientFactory;
import io.levelops.integrations.checkmarx.models.CxSastProject;
import io.levelops.integrations.checkmarx.sources.cxsast.CxSastProjectDataSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class CxSastProjectDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder()
            .integrationId(EMPTY).tenantId(EMPTY).build();
    CxSastProjectDataSource dataSource;

    @Before
    public void setup() throws CxSastClientException {
        CxSastClient client = Mockito.mock(CxSastClient.class);
        CxSastClientFactory clientFactory = Mockito.mock(CxSastClientFactory.class);
        dataSource = new CxSastProjectDataSource(clientFactory);
        when(clientFactory.get(TEST_KEY)).thenReturn(client);
        List<CxSastProject> projects = client.getProjects();
        when(client.getProjects())
                .thenReturn(projects);
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(CxSastProjectDataSource.CxSastProjectQuery
                .builder().integrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<CxSastProject>> projects = dataSource.fetchMany(CxSastProjectDataSource.CxSastProjectQuery.builder()
                .integrationKey(TEST_KEY)
                .build())
                .collect(Collectors.toList());
        assertThat(projects).hasSize(0);
    }
}
