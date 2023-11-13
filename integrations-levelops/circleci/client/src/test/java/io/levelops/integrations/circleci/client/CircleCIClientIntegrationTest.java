package io.levelops.integrations.circleci.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.circleci.models.CircleCIBuild;
import io.levelops.integrations.circleci.models.CircleCIProject;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CircleCIClientIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "circleci1";
    private static final String APPLICATION = "circleci";
    private static final String CIRCLECI_URL = System.getenv("CIRCLECI_URL");
    private static final String CIRCLECI_USERNAME = System.getenv("CIRCLECI_USERNAME");
    private static final String CIRCLECI_API_KEY = System.getenv("CIRCLECI_API_KEY");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private CircleCIClientFactory clientFactory;

    @Before
    public void setup() {
        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, CIRCLECI_URL, Collections.emptyMap(),
                        CIRCLECI_USERNAME, CIRCLECI_API_KEY)
                .build());
        clientFactory = CircleCIClientFactory.builder()
                .okHttpClient(client)
                .objectMapper(DefaultObjectMapper.get())
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void recentBuilds() throws CircleCIClientException {
        CircleCIClient client = clientFactory.get(TEST_INTEGRATION_KEY);
        List<CircleCIBuild> response = client.getRecentBuilds(0);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }

    @Test
    public void projects() throws CircleCIClientException {
        CircleCIClient client = clientFactory.get(TEST_INTEGRATION_KEY);
        List<CircleCIProject> response = client.getProjects();
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }

}
