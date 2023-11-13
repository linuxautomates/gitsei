package io.levelops.api.services;

import com.google.common.base.MoreObjects;
import io.levelops.api.services.GithubSpotCheckService.GithubSpotCheckUserData;
import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.notification.clients.msteams.MSTeamsBotClientFactory;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class GithubSpotCheckServiceIntegrationTest {

    GithubSpotCheckService githubSpotCheckService;
    @Before
    public void setUp() throws Exception {
        InMemoryInventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey("test", "1", "github", "https://api.github.com", null, "", System.getenv("GITHUB_API_KEY"))
                .build());
        githubSpotCheckService = new GithubSpotCheckService(inventoryService, DefaultObjectMapper.get(), new OkHttpClient());
    }

    @Test
    public void test() {
        IntegrationKey key = IntegrationKey.builder()
                .tenantId("test")
                .integrationId("1")
                .build();
        GithubSpotCheckUserData output = githubSpotCheckService.fetchUserData(key, "maxime-levelops", "2023-02-28", "2023-02-28", null);
        DefaultObjectMapper.prettyPrint(output);
    }
}