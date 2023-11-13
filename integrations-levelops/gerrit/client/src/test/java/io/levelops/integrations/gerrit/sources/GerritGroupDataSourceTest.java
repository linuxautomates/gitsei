package io.levelops.integrations.gerrit.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.gerrit.client.GerritClient;
import io.levelops.integrations.gerrit.client.GerritClientException;
import io.levelops.integrations.gerrit.client.GerritClientFactory;
import io.levelops.integrations.gerrit.models.GerritQuery;
import io.levelops.integrations.gerrit.models.GroupInfo;
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

public class GerritGroupDataSourceTest {
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    GerritGroupDataSource dataSource;

    @Before
    public void setup() throws GerritClientException {
        GerritClient gerritClient = Mockito.mock(GerritClient.class);
        GerritClientFactory gerritClientFactory = Mockito.mock(GerritClientFactory.class);
        dataSource = new GerritGroupDataSource(gerritClientFactory);
        when(gerritClientFactory.get(TEST_KEY)).thenReturn(gerritClient);
        List<GroupInfo> groups = List.of(
                GroupInfo.builder().id("group1").build(),
                GroupInfo.builder().id("group2").build(),
                GroupInfo.builder().id("group3").build(),
                GroupInfo.builder().id("group4").build(),
                GroupInfo.builder().id("group5").build());
        when(gerritClient.getGroups(0, 50)).thenReturn(groups);
        when(gerritClient.getGroups(50, 50)).thenReturn(new ArrayList<>());
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(GerritQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<GroupInfo>> groups = dataSource.fetchMany(
                GerritQuery.builder().integrationKey(TEST_KEY).build()).collect(Collectors.toList());
        assertThat(groups).hasSize(5);
    }
}
