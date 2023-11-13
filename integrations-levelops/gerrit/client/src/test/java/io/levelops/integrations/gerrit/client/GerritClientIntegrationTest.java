package io.levelops.integrations.gerrit.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.gerrit.models.GerritQuery;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class GerritClientIntegrationTest {
    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "gerrit1";
    private static final String APPLICATION = "gerrit";
    private static final String GERRIT_URL = System.getenv("GERRIT_URL");
    private static final String GERRIT_USERNAME = System.getenv("GERRIT_USERNAME");
    private static final String GERRIT_PASSWORD = System.getenv("GERRIT_PASSWORD");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private GerritClientFactory gerritClientFactory;

    @Before
    public void setup() {
        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, GERRIT_URL, null, GERRIT_USERNAME, GERRIT_PASSWORD)
                .build());
        gerritClientFactory = GerritClientFactory.builder()
                .okHttpClient(client)
                .objectMapper(DefaultObjectMapper.get())
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void getProjects() throws GerritClientException {
        var listProjectsResponse = gerritClientFactory.get(TEST_INTEGRATION_KEY).getProjects(0, 1);
        DefaultObjectMapper.prettyPrint(listProjectsResponse);
        assertThat(listProjectsResponse).isNotNull();
    }

    @Test
    public void getGroups() throws GerritClientException {
        var listGroupsResponse = gerritClientFactory.get(TEST_INTEGRATION_KEY).getGroups(0, 10);
        DefaultObjectMapper.prettyPrint(listGroupsResponse);
        assertThat(listGroupsResponse).isNotNull();
    }

    @Test
    public void getAccounts() throws GerritClientException {
        var listAccountsResponse = gerritClientFactory.get(TEST_INTEGRATION_KEY).getAccounts(0, 10);
        DefaultObjectMapper.prettyPrint(listAccountsResponse);
        assertThat(listAccountsResponse).isNotNull();
    }

    @Test
    public void getChanges() throws GerritClientException {
        var listChangesResponse = gerritClientFactory.get(TEST_INTEGRATION_KEY).getChanges(0, 10, GerritQuery.builder()
                .integrationKey(TEST_INTEGRATION_KEY)
                .after(Date.from(new Date().toInstant().minus(5, ChronoUnit.MINUTES))).build());
        DefaultObjectMapper.prettyPrint(listChangesResponse);
        assertThat(listChangesResponse).isNotNull();
    }
}
