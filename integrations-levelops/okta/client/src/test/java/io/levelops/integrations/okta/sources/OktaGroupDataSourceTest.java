package io.levelops.integrations.okta.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.okta.client.OktaClient;
import io.levelops.integrations.okta.client.OktaClientException;
import io.levelops.integrations.okta.client.OktaClientFactory;
import io.levelops.integrations.okta.models.OktaGroup;
import io.levelops.integrations.okta.models.OktaScanQuery;
import io.levelops.integrations.okta.models.PaginatedOktaResponse;
import io.levelops.integrations.okta.services.OktaEnrichmentService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class OktaGroupDataSourceTest {
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    OktaGroupsDataSource dataSource;

    @Before
    public void setup() throws OktaClientException {
        OktaClient client = Mockito.mock(OktaClient.class);
        OktaClientFactory clientFactory = Mockito.mock(OktaClientFactory.class);

        OktaEnrichmentService enrichmentService = new OktaEnrichmentService(1, 10);
        dataSource = new OktaGroupsDataSource(clientFactory, enrichmentService);

        when(client.isEnrichmentEnabled()).thenReturn(false);
        when(clientFactory.get(TEST_KEY)).thenReturn(client);
        String cursor = "cursor";
        when(client.getGroups(eq(OktaScanQuery.builder().integrationKey(TEST_KEY).build())))
                .thenReturn(PaginatedOktaResponse.<OktaGroup>builder()
                        .values(List.of(OktaGroup.builder().id("1").build()))
                        .nextCursor(cursor)
                        .build());
        when(client.getGroups(eq(OktaScanQuery.builder()
                .integrationKey(TEST_KEY)
                .cursor(cursor)
                .build())))
                .thenReturn(PaginatedOktaResponse.<OktaGroup>builder()
                        .values(List.of(OktaGroup.builder().id("2").build()))
                        .build());

        when(client.getMembersOfGroup("1")).thenReturn(Collections.emptyList());
        when(client.getMembersOfGroup("2")).thenReturn(Collections.emptyList());
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(OktaScanQuery.builder().integrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<OktaGroup>> groups = dataSource.fetchMany(OktaScanQuery.builder()
                .integrationKey(TEST_KEY)
                .cursor(null)
                .from(null)
                .build())
                .collect(Collectors.toList());
        assertThat(groups).hasSize(2);
    }
}
