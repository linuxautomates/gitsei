package io.levelops.integrations.tenable.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.tenable.models.NetworkResponse;
import io.levelops.integrations.tenable.models.ScannerPoolResponse;
import io.levelops.integrations.tenable.models.ScannerResponse;
import io.levelops.integrations.tenable.models.WASResponse;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test case for tenable client. Methods {@link TenableClient#getWasResponse(Integer, Integer, String)},
 * {@link TenableClient#getNetworks(Integer, Integer)}, {@link TenableClient#getScanners()} and
 * {@link TenableClient#getScannerPools()} are tested.
 * Credentials are passed as env variables.
 */
public class TenableClientIntegrationTest {
    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "tenable1";
    private static final String APPLICATION = "tenable";
    private static final String TENABLE_URL = System.getenv("TENABLE_URL");
    private static final String TENABLE_USERNAME = System.getenv("TENABLE_USERNAME");
    private static final String TENABLE_API_KEY = System.getenv("TENABLE_API_KEY");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private TenableClientFactory tenableClientFactory;

    @Before
    public void setup() {
        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, TENABLE_URL, Collections.emptyMap(), TENABLE_USERNAME, TENABLE_API_KEY)
                .build());
        tenableClientFactory = TenableClientFactory.builder()
                .okHttpClient(client)
                .objectMapper(DefaultObjectMapper.get())
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void getWasVulnerabilities() throws TenableClientException {
        WASResponse wasVulnerabilityResp = tenableClientFactory.get(TEST_INTEGRATION_KEY)
                .getWasResponse(0, 1, "desc");
        DefaultObjectMapper.prettyPrint(wasVulnerabilityResp);
        assertThat(wasVulnerabilityResp).isNotNull();
        assertThat(wasVulnerabilityResp.getData()).isNotNull();
    }

    @Test
    public void getNetworks() throws TenableClientException {
        NetworkResponse networks = tenableClientFactory.get(TEST_INTEGRATION_KEY).getNetworks(0, 1);
        DefaultObjectMapper.prettyPrint(networks);
        assertThat(networks).isNotNull();
        assertThat(networks.getNetworks()).isNotNull();
    }

    @Test
    public void getScanners() throws TenableClientException {
        ScannerResponse scanners = tenableClientFactory.get(TEST_INTEGRATION_KEY).getScanners();
        DefaultObjectMapper.prettyPrint(scanners);
        assertThat(scanners).isNotNull();
        assertThat(scanners.getScanners()).isNotNull();
    }

    @Test
    public void getScannerGroups() throws TenableClientException {
        ScannerPoolResponse scannerPools = tenableClientFactory.get(TEST_INTEGRATION_KEY).getScannerPools();
        DefaultObjectMapper.prettyPrint(scannerPools);
        assertThat(scannerPools).isNotNull();
        assertThat(scannerPools.getScannerPools()).isNotNull();
    }

}
