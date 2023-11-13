package io.levelops.api.services;

import io.levelops.api.model.spotchecks.GitlabSpotCheckProjectRequest;
import io.levelops.api.model.spotchecks.GitlabSpotCheckProjectResponse;
import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GitlabSpotCheckServiceIntegrationTest {
    private static final String COMPANY = "pearson";
    private static final String API_KEY = System.getenv("GITLAB_API_KEY");

    GitlabSpotCheckService gitlabSpotCheckService;
    @Before
    public void setUp() throws Exception {
        InMemoryInventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(COMPANY, "1", "gitlab", "https://gitlab.com", null, "", API_KEY)
                .build());
        gitlabSpotCheckService = new GitlabSpotCheckService(inventoryService, DefaultObjectMapper.get(), new OkHttpClient());
    }

    @Test
    public void test() {
        GitlabSpotCheckProjectRequest request = GitlabSpotCheckProjectRequest.builder()
                .integrationId(1).projectName("Pegasus").from("05/01/2023").to("05/31/2023").limit(10000)
                .build();
        GitlabSpotCheckProjectResponse output = gitlabSpotCheckService.fetchProjectData(COMPANY, request);
        DefaultObjectMapper.prettyPrint(output);
    }
}