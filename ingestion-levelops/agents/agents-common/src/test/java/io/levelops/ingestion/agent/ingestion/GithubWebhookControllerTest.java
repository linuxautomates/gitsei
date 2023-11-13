package io.levelops.ingestion.agent.ingestion;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.github.models.GithubCreateWebhookQuery;
import io.levelops.ingestion.integrations.github.models.GithubCreateWebhookResult;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.models.GithubWebhookConfig;
import io.levelops.integrations.github.models.GithubWebhookRequest;
import io.levelops.integrations.github.models.GithubWebhookResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;

public class GithubWebhookControllerTest {

    @Mock
    GithubClientFactory clientFactory;

    @Mock
    GithubClient client;

    GithubWebhookController controller;

    private final static String INTEGRATION_ID = "test";
    private final static String TENANT_ID = "tenant";
    private final static IntegrationKey INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID)
            .tenantId(TENANT_ID)
            .build();
    private final static String ORG1 = "org1";
    private final static String ORG2 = "org2";
    private final static String ORG3 = "org3";

    GithubWebhookResponse webhookResponse = GithubWebhookResponse.builder()
            .id(296776201)
            .name("web")
            .type("Organization")
            .active(true)
            .events(List.of("issues",
                    "project",
                    "project_card",
                    "project_column",
                    "push"))
            .config(GithubWebhookConfig.builder()
                    .contentType("json")
                    .secret("********")
                    .url("https://webhook.site/80be479e-1e49-409a-bc98-45f82a5b1c28")
                    .insecureSsl("0")
                    .build())
            .url("https://api.github.com/orgs/cog1/hooks/296776201")
            .pingUrl("https://api.github.com/orgs/cog1/hooks/296776201/pings")
            .build();

    GithubWebhookRequest webhookRequest = GithubWebhookRequest.builder()
            .name("web")
            .events(List.of("issues",
                    "project",
                    "project_card",
                    "project_column",
                    "push"))
            .config(GithubWebhookConfig.builder()
                    .contentType("json")
                    .secret("********")
                    .url("https://webhook.site/80be479e-1e49-409a-bc98-45f82a5b1c28")
                    .build())
            .build();

    @Before
    public void setup() throws GithubClientException {
        MockitoAnnotations.initMocks(this);
        when(clientFactory.get(INTEGRATION_KEY, false)).thenReturn(client);

        controller = new GithubWebhookController(DefaultObjectMapper.get(), clientFactory);

        when(client.streamWebhooks(ORG1, INTEGRATION_KEY)).thenReturn(Stream.of(webhookResponse));
        when(client.createWebhook(ORG1, webhookRequest)).thenReturn(webhookResponse);
        when(client.updateWebhook(ORG1, 296776201, webhookRequest)).thenReturn(webhookResponse);
        when(client.streamWebhooks(ORG2, INTEGRATION_KEY)).thenReturn(null);
        when(client.streamWebhooks(ORG3, INTEGRATION_KEY)).thenReturn(Stream.of(webhookResponse));
        when(client.createWebhook(ORG3, webhookRequest)).thenReturn(webhookResponse);
        when(client.updateWebhook(ORG3, 296776201, webhookRequest)).thenReturn(webhookResponse);
    }

    @Test
    public void test() throws IngestException {
        GithubCreateWebhookQuery createWebhookQuery = GithubCreateWebhookQuery.builder()
                .integrationKey(INTEGRATION_KEY)
                .organizations(List.of(ORG1))
                .events(List.of("issues",
                        "project",
                        "project_card",
                        "project_column",
                        "push"))
                .url("https://webhook.site/80be479e-1e49-409a-bc98-45f82a5b1c28")
                .secret("********")
                .build();
        ControllerIngestionResult ingest = controller.ingest(JobContext.builder().jobId("123").build(), createWebhookQuery);
        Assert.assertEquals(GithubCreateWebhookResult.builder().webhooks(List.of(webhookResponse)).build(), ingest);
    }

    @Test(expected = IngestException.class)
    public void testWithInvalidOrg() throws IngestException {
        GithubCreateWebhookQuery createWebhookQuery1 = GithubCreateWebhookQuery.builder()
                .integrationKey(INTEGRATION_KEY)
                .organizations(List.of(ORG1, ORG2, ORG3))
                .events(List.of("issues",
                        "project",
                        "project_card",
                        "project_column",
                        "push"))
                .url("https://webhook.site/80be479e-1e49-409a-bc98-45f82a5b1c28")
                .secret("********")
                .build();
        controller.ingest(JobContext.builder().jobId("123").build(), createWebhookQuery1);
    }
}
