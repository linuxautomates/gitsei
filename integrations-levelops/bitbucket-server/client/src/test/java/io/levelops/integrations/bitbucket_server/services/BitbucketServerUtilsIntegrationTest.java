package io.levelops.integrations.bitbucket_server.services;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClient;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientException;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientFactory;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class BitbucketServerUtilsIntegrationTest {
    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "bitbucket_server";
    private static final String APPLICATION = IntegrationType.BITBUCKET_SERVER.name();
    private static final String BITBUCKET_SERVER_URL = System.getenv("BITBUCKET_SERVER_URL");
    private static final String BITBUCKET_SERVER_USERNAME = System.getenv("BITBUCKET_SERVER_USERNAME");
    private static final String BITBUCKET_SERVER_PASSWORD = System.getenv("BITBUCKET_SERVER_PASSWORD");
    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private BitbucketServerClientFactory clientFactory;
    BitbucketServerClient bitbucketServerClient;

    @Before
    public void setup() throws BitbucketServerClientException {
        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory
                .builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, BITBUCKET_SERVER_URL, Collections.emptyMap(),
                        BITBUCKET_SERVER_USERNAME, BITBUCKET_SERVER_PASSWORD)
                .build());
        clientFactory = BitbucketServerClientFactory.builder()
                .okHttpClient(client)
                .objectMapper(DefaultObjectMapper.get())
                .inventoryService(inventoryService)
                .build();
        bitbucketServerClient = clientFactory.get(TEST_INTEGRATION_KEY, true);
    }


    @Test
    public void testStreamRepos() throws BitbucketServerClientException {
        var repos = BitbucketServerUtils.fetchRepos(bitbucketServerClient, List.of(), List.of("SBA"))
                .collect(Collectors.toList());
        repos.stream().forEach(repo -> System.out.println(" >>>> REPO: " + repo.getLeft() + "/" + repo.getRight()));
        assertThat(repos).hasSize(1);
    }
}