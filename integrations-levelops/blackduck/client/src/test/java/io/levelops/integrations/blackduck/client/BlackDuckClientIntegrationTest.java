package io.levelops.integrations.blackduck.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.blackduck.BlackDuckClientException;
import io.levelops.integrations.blackduck.BlackDuckClientFactory;
import io.levelops.integrations.blackduck.models.BlackDuckIssue;
import io.levelops.integrations.blackduck.models.BlackDuckProject;
import io.levelops.integrations.blackduck.models.BlackDuckVersion;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BlackDuckClientIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "blackduck1";
    private static final String APPLICATION = "blackduck";

    private static final String BLACKDUCK_URL = System.getenv("BLACKDUCK_URL");
    private static final String BLACKDUCK_BEARER_TOKEN = System.getenv("BLACKDUCK_API_KEY");
    private static final String BLACKDUCK_PROJECT_ID = System.getenv("BLACKDUCK_PROJECT_ID");
    private static final String BLACKDUCK_VERSION_ID = System.getenv("BLACKDUCK_VERSION_ID");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey
            .builder().integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private BlackDuckClientFactory clientFactory;

    @Before
    public void setup() {
        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .oauthToken(TENANT_ID, INTEGRATION_ID, APPLICATION, BLACKDUCK_URL, Collections.emptyMap(),
                        BLACKDUCK_BEARER_TOKEN, BLACKDUCK_BEARER_TOKEN, null)
                .build());
        clientFactory = BlackDuckClientFactory.builder()
                .okHttpClient(client)
                .objectMapper(DefaultObjectMapper.get())
                .pageSize(20)
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void getProjects() throws BlackDuckClientException {
        List<BlackDuckProject> blackDuckProjects = clientFactory.get(TEST_INTEGRATION_KEY).getProjects();
        BlackDuckProject blackDuckProject = blackDuckProjects.get(0);
        DefaultObjectMapper.prettyPrint(blackDuckProject);
        assertThat(blackDuckProjects).isNotNull();
        assertThat(blackDuckProjects).isNotEmpty();
    }

    @Test
    public void getVersions() throws BlackDuckClientException {
        List<BlackDuckVersion> versions = clientFactory.get(TEST_INTEGRATION_KEY).getVersions(BLACKDUCK_PROJECT_ID);
        BlackDuckVersion blackDuckVersion = versions.get(0);
        DefaultObjectMapper.prettyPrint(blackDuckVersion);
        assertThat(blackDuckVersion).isNotNull();
        assertThat(blackDuckVersion.getBlackDuckMetadata().getProjectHref()).isNotEmpty();
    }

    @Test
    public void getIssues() throws BlackDuckClientException {
        List<BlackDuckIssue> issues = clientFactory.get(TEST_INTEGRATION_KEY).getIssues(BLACKDUCK_PROJECT_ID, BLACKDUCK_VERSION_ID,0);
        BlackDuckIssue blackDuckIssue = issues.get(0);
        DefaultObjectMapper.prettyPrint(blackDuckIssue);
        assertThat(blackDuckIssue).isNotNull();
        assertThat(blackDuckIssue.getComponentVersionName()).isNotEmpty();
    }
}
