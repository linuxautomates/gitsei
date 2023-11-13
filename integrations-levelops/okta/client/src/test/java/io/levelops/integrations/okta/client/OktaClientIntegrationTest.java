package io.levelops.integrations.okta.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.okta.models.OktaGroup;
import io.levelops.integrations.okta.models.OktaScanQuery;
import io.levelops.integrations.okta.models.OktaUser;
import io.levelops.integrations.okta.models.PaginatedOktaResponse;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class OktaClientIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "okta1";
    private static final String APPLICATION = "okta";
    private static final String OKTA_URL =  System.getenv("OKTA_URL");
    private static final String OKTA_USERNAME = "Authorization";
    private static final String OKTA_API_KEY = System.getenv("OKTA_API_KEY");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private OktaClientFactory oktaClientFactory;

    @Before
    public void setup() {
        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, OKTA_URL, Collections.emptyMap(), OKTA_USERNAME, OKTA_API_KEY)
                .build());
        oktaClientFactory = OktaClientFactory.builder()
                .okHttpClient(client)
                .objectMapper(DefaultObjectMapper.get())
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void getGroups() throws OktaClientException {
        PaginatedOktaResponse<OktaGroup> groupsResponse = oktaClientFactory.get(TEST_INTEGRATION_KEY)
                .getGroups(OktaScanQuery.builder().integrationKey(TEST_INTEGRATION_KEY).build());
        DefaultObjectMapper.prettyPrint(groupsResponse);
        assertThat(groupsResponse).isNotNull();
    }

    @Test
    public void getUsers() throws OktaClientException {
        PaginatedOktaResponse<OktaUser> usersResponse = oktaClientFactory.get(TEST_INTEGRATION_KEY)
                .getUsers(OktaScanQuery.builder().integrationKey(TEST_INTEGRATION_KEY).build());
        DefaultObjectMapper.prettyPrint(usersResponse);
        assertThat(usersResponse).isNotNull();
    }

}
