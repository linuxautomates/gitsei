package io.levelops.integrations.bitbucket.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class BitbucketClientFactoryIntegrationTest {
    private BitbucketClientFactory clientFactory;
    private static final IntegrationKey KEY = IntegrationKey.builder().tenantId("foo").integrationId("43").build();


    @Before
    public void setUp() throws Exception {
        String dbServer = System.getenv("POSTGRES-DB-SERVER");
        String userName = System.getenv("POSTGRES-USERNAME");
        String password = System.getenv("POSTGRES-PASSWORD");
        String dbName = System.getenv("POSTGRES-DB-NAME");

        OkHttpClient okHttpClient = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .dbAuth("coke", "postgres1", "postgres", "http://levelops.atlassian.net", null, dbServer, userName, password, dbName)
                .build());

        clientFactory = BitbucketClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(DefaultObjectMapper.get())
                .okHttpClient(okHttpClient)
                .build();
    }


    @Test
    public void testPRsAndCommits() throws BitbucketClientException {
        BitbucketClient client = clientFactory.get(KEY, true);
        client.streamWorkspaces()
                .peek(w -> System.out.println(" >>>> WORKSPACE: " + w.getName()))
                .forEach(bitbucketWorkspace -> {
                    client.streamRepositories(bitbucketWorkspace.getSlug()).forEach(repo -> {
                        System.out.println(" --- REPO: " + repo.getFullName());
                        client.streamPullRequests(repo.getWorkspaceSlug(), repo.getUuid(), Instant.now().minus(90, ChronoUnit.DAYS), Instant.now())
                                .forEach(pr -> {
                                    System.out.println("    --- PR: " + pr.getId());
                                    client.streamPullRequestCommits(bitbucketWorkspace.getSlug(), repo.getUuid(), pr.getId())
                                            .map(BitbucketCommit::getMessage)
                                            .forEach(s -> System.out.println("      commit: " + s));
                                });
                    });
                });
    }

    @Test
    public void testCommitsAndDiffStats() throws BitbucketClientException {
        BitbucketClient client = clientFactory.get(KEY, true);
        client.streamWorkspaces()
                .peek(w -> System.out.println(" >>>> WORKSPACE: " + w.getName()))
                .forEach(bitbucketWorkspace -> {
                    client.streamRepositories(bitbucketWorkspace.getSlug()).forEach(repo -> {
                        System.out.println(" --- REPO: " + repo.getFullName());
                        client.streamRepoCommits(repo.getWorkspaceSlug(), repo.getUuid())
                                .forEach(c -> {
                                    System.out.println("    commit: " + c.getMessage());
                                    client.streamRepoCommitDiffSets(c.getWorkspaceSlug(), c.getRepoUuid(), c.getHash())
                                            .forEach(d -> System.out.println("      diffset: " + d.toString()));

                                });
                    });
                });
    }

}