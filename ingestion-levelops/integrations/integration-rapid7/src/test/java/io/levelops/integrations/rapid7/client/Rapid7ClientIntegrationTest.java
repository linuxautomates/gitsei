package io.levelops.integrations.rapid7.client;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.rapid7.models.api.Rapid7ApiVulnerability;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class Rapid7ClientIntegrationTest {

    IntegrationKey KEY = IntegrationKey.builder()
            .tenantId("coke")
            .integrationId("rapid7")
            .build();
    private Rapid7ClientFactory clientFactory;

    @Before
    public void setUp() throws Exception {
        OkHttpClient okHttpClient = new OkHttpClient();
        clientFactory = Rapid7ClientFactory.builder()
                .inventoryService(new InventoryServiceImpl("http://localhost:9999", okHttpClient, DefaultObjectMapper.get()))
                .objectMapper(DefaultObjectMapper.get())
                .okHttpClient(okHttpClient)
                .build();
    }

    @Test
    public void test() throws Rapid7ClientException {
        var apps = clientFactory.get(KEY).getApps(0);
        apps.getData().forEach(System.out::println);

        var vulnerabilites = clientFactory.get(KEY).getVulnerabilities(0);
        Map<String, Integer> vulPerApp = new HashMap<>();
        vulnerabilites.getData().stream()
                .map(Rapid7ApiVulnerability::getApp)
                .map(Rapid7ApiVulnerability.App::getId)
                .forEach(app -> vulPerApp.put(app, vulPerApp.getOrDefault(app, 0) + 1));
        vulPerApp.keySet().forEach(app -> System.out.println(app + " : " + vulPerApp.get(app) + " vulnerabilities"));
    }
}