package io.levelops.integrations.okta.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.okta.client.OktaClient;
import io.levelops.integrations.okta.client.OktaClientException;
import io.levelops.integrations.okta.client.OktaClientFactory;
import io.levelops.integrations.okta.models.OktaScanQuery;
import io.levelops.integrations.okta.models.OktaUser;
import io.levelops.integrations.okta.models.OktaUserType;
import io.levelops.integrations.okta.models.PaginatedOktaResponse;
import io.levelops.integrations.okta.services.OktaEnrichmentService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class OktaUserDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();
    public static final String ID = "id";

    OktaUsersDataSource dataSource;

    @Before
    public void setup() throws OktaClientException {
        OktaClient client = Mockito.mock(OktaClient.class);
        OktaClientFactory clientFactory = Mockito.mock(OktaClientFactory.class);

        OktaEnrichmentService enrichmentService = new OktaEnrichmentService(1, 10);
        dataSource = new OktaUsersDataSource(clientFactory, enrichmentService);

        when(client.isEnrichmentEnabled()).thenReturn(false);
        when(clientFactory.get(TEST_KEY)).thenReturn(client);
        String cursor = "cursor";
        when(client.getUsers(eq(OktaScanQuery.builder().integrationKey(TEST_KEY).build())))
                .thenReturn(PaginatedOktaResponse.<OktaUser>builder()
                        .values(List.of(OktaUser.builder().id("1").type(Map.of(ID, "1")).build()))
                        .nextCursor(cursor)
                        .build());
        when(client.getUsers(eq(OktaScanQuery.builder()
                .integrationKey(TEST_KEY)
                .cursor(cursor)
                .build())))
                .thenReturn(PaginatedOktaResponse.<OktaUser>builder()
                        .values(List.of(OktaUser.builder().id("2").type(Map.of(ID, "2")).build()))
                        .build());
        when(client.getUserTypes()).thenReturn(List.of(OktaUserType.builder().id("1").name("name1").build(),
                OktaUserType.builder().id("2").name("name2").build()));

        when(client.getLinkedObjectDefinitions()).thenReturn(Collections.emptyList());
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(OktaScanQuery.builder().integrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<OktaUser>> users = dataSource.fetchMany(OktaScanQuery.builder()
                .integrationKey(TEST_KEY)
                .cursor(null)
                .from(null)
                .build())
                .collect(Collectors.toList());
        assertThat(users).hasSize(2);
    }
}
