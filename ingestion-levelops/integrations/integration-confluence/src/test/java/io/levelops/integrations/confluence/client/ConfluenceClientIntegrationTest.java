package io.levelops.integrations.confluence.client;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.confluence.models.ConfluenceSearchResponse;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ConfluenceClientIntegrationTest {

    private ConfluenceClientFactory confluenceClientFactory;
    private static final IntegrationKey KEY = IntegrationKey.builder().tenantId("coke").integrationId("jira").build();

    @Before
    public void setUp() throws Exception {
        OkHttpClient okHttpClient = new OkHttpClient();
        confluenceClientFactory = ConfluenceClientFactory.builder()
                .inventoryService(new InventoryServiceImpl("http://localhost:9999", okHttpClient, DefaultObjectMapper.get()))
                .objectMapper(DefaultObjectMapper.get())
                .okHttpClient(okHttpClient)
                .build();
    }

    @Test
    public void search() throws ConfluenceClientException {
        // ingestion OR slack -> 9 results
        // ingestion AND slack -> 1 result
//        String cql = ConfluenceCqlBuilder.builder()
//                .contains("text", "ingestion")
//                .and()
//                .contains("text", "slack")
//                .orderByDesc("lastmodified")
//                .build();
        String cql = ConfluenceCqlBuilder.builder()
                .containsAny("text", List.of("ingestion", "slack"))
                .and()
                .since("lastmodified","-10d")
                .orderByDesc("lastmodified")
                .build();
        DefaultObjectMapper.prettyPrint(cql);
        ConfluenceSearchResponse search = confluenceClientFactory.get(KEY).search(cql, 4, 2);
        System.out.println(search.getResults().size() + " result(s)");
        search.getResults().forEach(r -> System.out.println(r.getTitle() + " - " + r.getExcerpt().replaceAll("\\n", " ")));
//        DefaultObjectMapper.prettyPrint(search);
    }

}