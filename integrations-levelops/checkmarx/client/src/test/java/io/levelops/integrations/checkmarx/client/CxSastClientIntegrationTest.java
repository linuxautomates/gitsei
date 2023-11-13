package io.levelops.integrations.checkmarx.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.checkmarx.client.cxsast.CxSastClientException;
import io.levelops.integrations.checkmarx.client.cxsast.CxSastClientFactory;
import io.levelops.integrations.checkmarx.models.CxSastProject;
import io.levelops.integrations.checkmarx.models.CxSastScan;
import io.levelops.integrations.checkmarx.models.VCSSettings;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class CxSastClientIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "cXSast1";
    private static final String APPLICATION = "cxsast";
    private static final String CXSAST_URL = System.getenv("CXSAST_URL");
    private static final String CXSAST_ACCESS_TOKEN = System.getenv("CXSAST_USERNAME");
    private static final String CXSAST_REFRESH_TOKEN = System.getenv("CXSAST_PASSWORD");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey
            .builder().integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();
    private static String TEST_TICKET_ID;
    private CxSastClientFactory clientFactory;

    @Before
    public void setup() throws CxSastClientException {
        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .oauthToken(TENANT_ID, INTEGRATION_ID, APPLICATION, CXSAST_URL, Collections.emptyMap(),
                        CXSAST_ACCESS_TOKEN, CXSAST_REFRESH_TOKEN, null)
                .build());
        clientFactory = CxSastClientFactory.builder()
                .okHttpClient(client)
                .objectMapper(DefaultObjectMapper.get())
                .inventoryService(inventoryService)
                .build();
        List<CxSastProject> projectResponse = clientFactory.get(TEST_INTEGRATION_KEY).getProjects();
        if (projectResponse != null) {
            TEST_TICKET_ID = projectResponse.get(0).getId();
        }
        DefaultObjectMapper.prettyPrint(projectResponse);
    }

    @Test
    public void getScansTest() throws CxSastClientException {
        List<CxSastScan> issueResponse = clientFactory.get(TEST_INTEGRATION_KEY)
                .getScans();
        DefaultObjectMapper.prettyPrint(issueResponse);
    }

    @Test
    public void getSettingsTest() throws CxSastClientException {
        VCSSettings response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getSettings(TEST_TICKET_ID, false);
        DefaultObjectMapper.prettyPrint(response);
    }
}
