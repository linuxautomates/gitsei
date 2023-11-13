package io.levelops.integrations.scm_repo_mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.github.client.GithubClientFactory;
import okhttp3.OkHttpClient;
import org.junit.Test;

import java.util.List;

public class GithubRepoMappingClientIntegrationTest {
    @Test
    public void test() {
        OkHttpClient okHttpClient = new OkHttpClient();
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        InventoryService inventoryService = new InventoryServiceImpl("http://localhost:8080", okHttpClient, objectMapper);
        GithubClientFactory githubClientFactory = new GithubClientFactory(inventoryService, objectMapper, okHttpClient, 0);
        GithubRepoMappingClient client = new GithubRepoMappingClient(githubClientFactory);

        var repos = client.getReposForUser("harnessio", "19", "sid-propelo", List.of("levelops"));
        System.out.println(repos);
    }
}
