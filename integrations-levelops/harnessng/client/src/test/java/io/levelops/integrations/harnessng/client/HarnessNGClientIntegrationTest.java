package io.levelops.integrations.harnessng.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.data.Data;
import io.levelops.integrations.harnessng.models.HarnessNGExecutionInputSet;
import io.levelops.integrations.harnessng.models.HarnessNGIngestionQuery;
import io.levelops.integrations.harnessng.models.HarnessNGPipeline;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineExecution;
import io.levelops.integrations.harnessng.models.HarnessNGProject;
import io.levelops.integrations.harnessng.source.HarnessNGEnrichPipelineDataSource;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class HarnessNGClientIntegrationTest {

    public static final int PAGE = 0;
    private static final String TENANT_ID = "foo";
    private static final String INTEGRATION_ID = "test";
    private static final String APPLICATION = "harnessng";

    private static String HARNESSNG_TOKEN = System.getenv("HARNESSNG_TOKEN");

    private static String HARNESSNG_URL = System.getenv("HARNESSNG_URL");

    private static final String ORGANIZATION_NAME = System.getenv("HARNESSNG_ORGANIZATION");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private HarnessNGClientFactory clientFactory;

    @Before
    public void setup() {

        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, HARNESSNG_URL, Collections.emptyMap(),
                        ORGANIZATION_NAME, HARNESSNG_TOKEN)
                .build());

        clientFactory = HarnessNGClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(DefaultObjectMapper.get())
                .okHttpClient(client)
                .build();
    }

    @Test
    public void projects() throws HarnessNGClientException {
        List<HarnessNGProject> response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getProjects(HarnessNGClientResilienceTest.accountIdentifier, PAGE);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void pipelines() throws HarnessNGClientException {
        List<HarnessNGPipeline> response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getExecutions(HarnessNGClientResilienceTest.accountIdentifier, HarnessNGClientResilienceTest.projectIdentifier, HarnessNGClientResilienceTest.orgIdentifier, PAGE, null, null);;
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void pipelineExecutionDetails() throws HarnessNGClientException {
        HarnessNGPipelineExecution response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getExecutionDetails(HarnessNGClientResilienceTest.accountIdentifier, HarnessNGClientResilienceTest.projectIdentifier, HarnessNGClientResilienceTest.projectIdentifier, HarnessNGClientResilienceTest.orgIdentifier, true);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void getExecutionInputSet() throws HarnessNGClientException {
        HarnessNGExecutionInputSet response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getExecutionInputSet("wFHXHD0RRQWoO8tIZT5YVw", "Operations", "Harness", "gYnew5wrRBaiJyE_cyi0vA", true);
        DefaultObjectMapper.prettyPrint(response);

        // -- with tags
        HarnessNGPipelineExecution executionDetails = clientFactory.get(TEST_INTEGRATION_KEY)
                .getExecutionDetails("wFHXHD0RRQWoO8tIZT5YVw", "Operations", "Harness", "gYnew5wrRBaiJyE_cyi0vA", true);
        System.out.println("----");
        DefaultObjectMapper.prettyPrint(executionDetails);

        // -- with tags
//        HarnessNGPipelineExecution executionDetails = clientFactory.get(TEST_INTEGRATION_KEY)
//                .getExecutionDetails("wFHXHD0RRQWoO8tIZT5YVw", "Operations", "Harness", "6LVY50T1Sjm2gSLu4tsynQ", true);
//        System.out.println("----");
//        DefaultObjectMapper.prettyPrint(executionDetails);

    }
}
