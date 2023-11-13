package io.levelops.integrations.droneci.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.droneci.models.DroneCIBuild;
import io.levelops.integrations.droneci.models.DroneCIBuildStepLog;
import io.levelops.integrations.droneci.models.DroneCIEnrichRepoData;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;


public class DroneCIClientIntegrationTest {

    public static final int PAGE = 1;
    public static final int PER_PAGE = 2;
    private static final String TENANT_ID = "foo";
    private static final String INTEGRATION_ID = "test";
    private static final String APPLICATION = "droneci";

    private static String DRONECI_TOKEN = System.getenv("DRONECI_TOKEN");

    private static String DRONECI_URL = System.getenv("DRONECI_URL");

    private static final String OWNER_NAME = System.getenv("DRONECI_OWNER");

    private static final String REPO_NAME = System.getenv("DRONECI_REPO");

    private static final long BUILD_NUMBER = 1;

    private static final int STAGE_NUMBER = 1;

    private static final int STEP_NUMBER = 1;

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private DroneCIClientFactory clientFactory;

    @Before
    public void setup() throws DroneCIClientException {

        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, DRONECI_URL, Collections.emptyMap(),
                        OWNER_NAME, DRONECI_TOKEN)
                .build());

        clientFactory = DroneCIClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(DefaultObjectMapper.get())
                .okHttpClient(client)
                .build();
        Stream<DroneCIEnrichRepoData> response = clientFactory.get(TEST_INTEGRATION_KEY).streamRepositories();
    }

    @Test
    public void repositories() throws DroneCIClientException {
        List<DroneCIEnrichRepoData> response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getRepositories(PAGE, PER_PAGE);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void builds() throws DroneCIClientException {
        List<DroneCIBuild> response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getRepoBuilds(OWNER_NAME, REPO_NAME, PAGE, PER_PAGE);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void build() throws DroneCIClientException {
        DroneCIBuild response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getBuildInfo(OWNER_NAME, REPO_NAME, BUILD_NUMBER);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void buildStepLogs() throws DroneCIClientException {
        List<DroneCIBuildStepLog> response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getBuildStepLogs(OWNER_NAME, REPO_NAME, BUILD_NUMBER, STAGE_NUMBER, STEP_NUMBER);
        DefaultObjectMapper.prettyPrint(response);
    }
}
