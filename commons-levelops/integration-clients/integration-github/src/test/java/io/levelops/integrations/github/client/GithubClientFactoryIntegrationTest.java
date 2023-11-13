package io.levelops.integrations.github.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.Test;

import java.util.stream.Collectors;

public class GithubClientFactoryIntegrationTest {
    @Test
    public void test() throws GithubClientException {
        OkHttpClient okHttpClient = new OkHttpClient();
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        InventoryService inventoryService = new InventoryServiceImpl("http://localhost:8080", okHttpClient, objectMapper);
        GithubClientFactory githubClientFactory = new GithubClientFactory(
                inventoryService, objectMapper, okHttpClient, 0
        );

        var client = githubClientFactory.get(IntegrationKey.builder()
                .tenantId("sidofficial2")
                .integrationId("4")
                .build(), true);

        System.out.println("First request");
        var a = client.streamAppInstallationOrgs().collect(Collectors.toList());
        System.out.println(a);

        System.out.println("Second request");
        a = client.streamAppInstallationOrgs().collect(Collectors.toList());
        System.out.println(a);

        System.out.println("Third request");
        a = client.streamAppInstallationOrgs().collect(Collectors.toList());
        System.out.println(a);
    }

}