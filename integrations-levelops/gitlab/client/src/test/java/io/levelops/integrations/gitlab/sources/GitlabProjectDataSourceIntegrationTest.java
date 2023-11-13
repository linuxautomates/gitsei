package io.levelops.integrations.gitlab.sources;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.controllers.IntermediateStateUpdater;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.gitlab.client.GitlabClientException;
import io.levelops.integrations.gitlab.client.GitlabClientFactory;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.stream.Collectors;

public class GitlabProjectDataSourceIntegrationTest {
    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "gitlab1";
    private static final String APPLICATION = "gitlab";
    private static final String GITLAB_TOKEN = "glpat-UhnH1RCzxTUGhm6pPP6c";//System.getenv("GITLAB_TOKEN");
    private static final String GITLAB_URL =  "https://gitlab.com/";// System.getenv("GITLAB_URL");
    private static final String GITLAB_REFRESH_TOKEN = "glpat-UhnH1RCzxTUGhm6pPP6c";// System.getenv("GITLAB_REFRESH_TOKEN");
    private GitlabClientFactory clientFactory;

    @Before
    public void setup() throws GitlabClientException {
        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .oauthToken(TENANT_ID, INTEGRATION_ID, APPLICATION, GITLAB_URL, Collections.emptyMap(),
                        GITLAB_TOKEN, GITLAB_REFRESH_TOKEN, null)
                .build());
        clientFactory = GitlabClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(DefaultObjectMapper.get())
                .okHttpClient(client)
                .build();
    }

    @Test
    public void testFetchMergeRequests() throws FetchException {
        IntermediateStateUpdater intermediateStateUpdater = Mockito.mock(IntermediateStateUpdater.class);
        GitlabProjectDataSource projectDataSource = new GitlabProjectDataSource(clientFactory, EnumSet.of(GitlabProjectDataSource.Enrichment.MERGE_REQUESTS));
        var results = projectDataSource.fetchMany(
                JobContext.builder().build(),
                GitlabProjectDataSource.GitlabProjectQuery.builder()
                        .integrationKey(IntegrationKey.builder()
                                .integrationId(INTEGRATION_ID)
                                .tenantId(TENANT_ID)
                                .build())
                        .fetchStateEvents(false)
                        .fetchPrPatches(false)
                        .from(new Date(122,1,1))
                        .to(new Date())
                        .checkProjectMembership(true)
                .build(), intermediateStateUpdater)
                .collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(results);
    }

}