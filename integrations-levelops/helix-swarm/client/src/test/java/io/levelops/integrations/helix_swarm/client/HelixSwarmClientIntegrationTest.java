package io.levelops.integrations.helix_swarm.client;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import io.levelops.integrations.helix_swarm.models.ProjectResponse;
import io.levelops.integrations.helix_swarm.models.ProjectsResponseV10;
import io.levelops.integrations.helix_swarm.models.ReviewFileResponse;
import io.levelops.integrations.helix_swarm.models.ReviewResponse;
import io.levelops.integrations.helix_swarm.models.ReviewResponseV10;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HelixSwarmClientIntegrationTest {

    private static final String TENANT_ID = "test";

    private static final String HELIX_SWARM_INTEGRATION_ID = "1";
    private static final String HELIX_CORE_INTEGRATION_ID = "2";
    private static final String HELIX_INTEGRATION_ID = "3";

    private static final String HELIX_SWARM_APPLICATION = "helix_swarm";
    private static final String HELIX_CORE_APPLICATION = "helix_core";
    private static final String HELIX_APPLICATION = "helix";

    private static final String HELIX_SWARM_URL = System.getenv("HELIX_SWARM_URL");    
    private static final String HELIX_CORE_URL = System.getenv("HELIX_CORE_URL");
    private static final String HELIX_USERNAME = System.getenv("HELIX_SWARM_USERNAME");
    private static final String HELIX_PWD = System.getenv("HELIX_SWARM_PWD");
    private static final String SSL_ENABLED = System.getenv("HELIX_SSL_ENABLED");
    private static final String SSL_AUTO_ACCEPT = System.getenv("HELIX_SSL_AUTO_ACCEPT");
    private static final String SSL_FINGERPRINT = System.getenv("HELIX_SSL_FINGERPRINT");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(HELIX_INTEGRATION_ID).tenantId(TENANT_ID).build();

    private HelixSwarmClientFactory clientFactory;

    @Before
    public void setup() {
        OkHttpClient client = new OkHttpClient();
        Map<String, Object> metadata = Map.of("ssl_enabled", Boolean.valueOf(SSL_ENABLED), "ssl_auto_accept",
                Boolean.valueOf(SSL_AUTO_ACCEPT), "ssl_fingerprint", SSL_FINGERPRINT, "helix_swarm_url", HELIX_SWARM_URL,
                "helix_core_integration_key", 2);

        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(TENANT_ID, HELIX_SWARM_INTEGRATION_ID, HELIX_SWARM_APPLICATION, HELIX_SWARM_URL,
                        metadata, HELIX_USERNAME, HELIX_PWD)
                .apiKey(TENANT_ID, HELIX_CORE_INTEGRATION_ID, HELIX_CORE_APPLICATION, HELIX_CORE_URL,
                        metadata, HELIX_USERNAME, HELIX_PWD)
                .apiKey(TENANT_ID, HELIX_INTEGRATION_ID, HELIX_APPLICATION, HELIX_CORE_URL,
                        metadata, HELIX_USERNAME, HELIX_PWD)
                .build());
        clientFactory = HelixSwarmClientFactory.builder()
                .okHttpClient(client)
                .objectMapper(DefaultObjectMapper.get())
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void reviews() throws HelixSwarmClientException {
        HelixSwarmClient helixSwarmClient = clientFactory.get(TEST_INTEGRATION_KEY);
        ReviewResponse reviewResponse = helixSwarmClient.getReviews(null);
        assertThat(reviewResponse).isNotNull();
        assertThat(reviewResponse.getReviews()).isNotEmpty();
        DefaultObjectMapper.prettyPrint(reviewResponse);
        if(CollectionUtils.isNotEmpty(reviewResponse.getReviews())) {
            for(HelixSwarmReview helixSwarmReview : reviewResponse.getReviews()) {
                try {
                    ReviewFileResponse reviewFileResponse = helixSwarmClient.getReviewFiles(helixSwarmReview.getId(), null);
                    Assert.fail("HelixSwarmClientException - Not Found error expected!");
                    assertThat(reviewFileResponse).isNotNull();
                } catch (HelixSwarmClientException e) {
                    Assert.assertEquals(String.format("Response not successful: Response{protocol=http/1.1, code=404, message=Not Found, url=http://10.128.0.35/api/v10/reviews/%s/files?max=100}", helixSwarmReview.getId()), e.getMessage());
                    Assert.assertEquals(true, HelixSwarmClient.isApiNotFoundExceptions(e));
                }
            }
        }
    }

    @Test
    public void reviewsV10() throws HelixSwarmClientException {
        HelixSwarmClient helixSwarmClient = clientFactory.get(TEST_INTEGRATION_KEY);
        ReviewResponseV10 reviewResponse = helixSwarmClient.getReviewsV10(null);
        assertThat(reviewResponse).isNotNull();
        assertThat(reviewResponse.getData().getReviews()).isNotEmpty();
        DefaultObjectMapper.prettyPrint(reviewResponse);
        int successful = 0;
        int failed = 0;
        if(CollectionUtils.isNotEmpty(reviewResponse.getData().getReviews())) {
            for(HelixSwarmReview helixSwarmReview : reviewResponse.getData().getReviews()) {
                try {
                    ReviewFileResponse reviewFileResponse = helixSwarmClient.getReviewFiles(helixSwarmReview.getId(), null);
                    successful++;
                    assertThat(reviewFileResponse).isNotNull();
                    assertThat(reviewFileResponse.getData().getFiles().size()).isEqualTo(1000);
                } catch (HelixSwarmClientException e) {
                    failed++;
                    System.out.println(e);
                }
            }
        }
        System.out.println(successful);
        System.out.println(failed);
        ProjectsResponseV10 projectResponse = helixSwarmClient.getProjectsV10();
        assertThat(projectResponse).isNotNull();
    }

    @Test
    public void testIsApiNotFoundExceptions() {
        Assert.assertEquals(true, HelixSwarmClient.isApiNotFoundExceptions(new HelixSwarmClientException("Response not successful: Response{protocol=http/1.1, code=404, message=Not Found, url=http://10.128.0.35/api/v10/reviews/10/files?max=100}")));
        Assert.assertEquals(false, HelixSwarmClient.isApiNotFoundExceptions(new HelixSwarmClientException("Response not successful: Response{protocol=http/1.1, code=401, message=Not Found, url=http://10.128.0.35/api/v10/reviews/10/files?max=100}")));
        Assert.assertEquals(false, HelixSwarmClient.isApiNotFoundExceptions(new HelixSwarmClientException("Response not successful: Response{protocol=http/1.1, code=500, message=Not Found, url=http://10.128.0.35/api/v10/reviews/10/files?max=100}")));
    }
}
