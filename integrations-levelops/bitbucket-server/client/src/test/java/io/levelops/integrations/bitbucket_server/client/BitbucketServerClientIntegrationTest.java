package io.levelops.integrations.bitbucket_server.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommit;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerPRActivity;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

@Log4j2
public class BitbucketServerClientIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "bitbucket_server";
    private static final String APPLICATION = IntegrationType.BITBUCKET_SERVER.name();
    private static final String BITBUCKET_SERVER_URL = System.getenv("BITBUCKET_SERVER_URL");
    private static final String BITBUCKET_SERVER_USERNAME = System.getenv("BITBUCKET_SERVER_USERNAME");
    private static final String BITBUCKET_SERVER_PASSWORD = System.getenv("BITBUCKET_SERVER_PASSWORD");
    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private BitbucketServerClientFactory clientFactory;

    @Before
    public void setup() {
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
    }


    @Test
    public void testPRsAndCommits() throws BitbucketServerClientException {
        BitbucketServerClient client = clientFactory.get(TEST_INTEGRATION_KEY, true);
        client.streamProjects()
                .peek(project -> System.out.println(" >>>> PROJECT: " + project.getName()))
                .forEach(project -> {
                    try {
                        client.streamRepositories(project.getKey()).forEach(repo -> {
                            System.out.println(" --- REPO: " + repo.getName());
                            try {
                                client.streamPullRequests(repo.getProject().getKey(), repo.getSlug())
                                        .forEach(pr -> {
                                            System.out.println("    --- PR: " + pr.getId());
                                            try {
                                                client.streamPrCommits(project.getKey(), repo.getSlug(), pr.getId())
                                                        .map(BitbucketServerCommit::getMessage)
                                                        .forEach(s -> System.out.println("      commit: " + s));
                                            } catch (BitbucketServerClientException e) {
                                                //log.warn("Failed to fetch pr commits for repo={} and pr={}", repo.getSlug(), pr.getId());
                                                e.printStackTrace();
                                            }
                                            try {
                                                client.streamPrActivities(project.getKey(), repo.getSlug(), pr.getId())
                                                        .map(BitbucketServerPRActivity::getAction)
                                                        .forEach(a -> System.out.println(" activity: " + a));
                                            } catch (BitbucketServerClientException e) {
                                                //log.warn("Failed to fetch pr activities for repo={} and pr={}", repo.getSlug(), pr.getId());
                                                e.printStackTrace();
                                            }
                                        });
                            } catch (BitbucketServerClientException e) {
                                //log.warn("Failed to fetch repo pull requests for repo={}", repo.getSlug());
                                e.printStackTrace();
                            }
                        });
                    } catch (BitbucketServerClientException e) {
                        //log.warn("Failed to fetch repositories for project={}", project.getName());
                        e.printStackTrace();
                    }
                });
    }

    @Test
    public void testCommits() throws BitbucketServerClientException {
        BitbucketServerClient client = clientFactory.get(TEST_INTEGRATION_KEY, true);
        client.streamProjects()
                .peek(project -> System.out.println(" >>>> PROJECT: " + project.getName()))
                .forEach(project -> {
                    try {
                        client.streamRepositories(project.getKey()).forEach(repo -> {
                            System.out.println(" --- REPO: " + repo.getName());
                            try {
                                client.streamCommits(repo.getProject().getKey(), repo.getSlug())
                                        .forEach(c -> System.out.println("    commit: " + c.getMessage()));
                            } catch (BitbucketServerClientException e) {
                                //log.warn("Failed to fetch repo commits for repo={}", repo.getSlug());
                                e.printStackTrace();
                            }
                        });
                    } catch (BitbucketServerClientException e) {
                        //log.warn("Failed to fetch repositories for project={}", project.getName());
                        e.printStackTrace();
                    }
                });
    }

}
