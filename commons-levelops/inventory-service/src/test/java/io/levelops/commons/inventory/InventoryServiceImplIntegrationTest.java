package io.levelops.commons.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.standalone.JsonFileMappingsSource;
import io.levelops.commons.models.DbListResponse;
import okhttp3.OkHttpClient;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.inventory.exceptions.InventoryException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

public class InventoryServiceImplIntegrationTest {

    @ClassRule
    public static final WireMockClassRule wireMockRule = new WireMockClassRule(options().dynamicPort());

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    private InventoryServiceImpl inventoryService;

    @Before
    public void setUp() throws Exception {
        wireMockRule.loadMappingsUsing(new JsonFileMappingsSource(new ClasspathFileSource("src/test/resources/wiremock/mappings/inventory")));
        inventoryService = InventoryServiceImpl.builder()
                .inventoryServiceUrl(wireMockRule.baseUrl())
                .client(new OkHttpClient())
                .objectMapper(new ObjectMapper())
                .build();
    }

    @Test
    public void listTenants() throws InventoryException {
        List<Tenant> tenants = inventoryService.listTenants();
        assertThat(tenants).containsExactly(Tenant.builder().id("coke").tenantName("Coke Inc.").build(),
                Tenant.builder().id("pepsi").tenantName("Pepsi Inc.").build());
    }

    @Test
    public void testGetTenant() throws InventoryException {
        assertThat(inventoryService.getTenant("coke").getId()).isEqualTo("coke");
    }

    @Test
    public void listIntegrations() throws InventoryException {
        DbListResponse<Integration> integrations = inventoryService.listIntegrations("coke");
        assertThat(integrations.getRecords()).containsExactly(Integration.builder().id("uuid1").build(), Integration.builder().id("uuid2").build());
    }

}