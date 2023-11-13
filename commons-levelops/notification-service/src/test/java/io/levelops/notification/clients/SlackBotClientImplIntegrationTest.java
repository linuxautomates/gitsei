package io.levelops.notification.clients;

import com.google.common.base.MoreObjects;
import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.notification.models.SlackApiViewResponse;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class SlackBotClientImplIntegrationTest {
    private SlackBotClientFactory clientFactory;
    private static final IntegrationKey KEY = IntegrationKey.builder().tenantId("coke").integrationId("slack1").build();

    @Before
    public void setUp() throws Exception {
        OkHttpClient okHttpClient = new OkHttpClient();
        clientFactory = SlackBotClientFactory.builder()
                .inventoryService(new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                        .oauthToken("coke", "slack1", "slack", null, null, null, null,
                                MoreObjects.firstNonNull(System.getenv("SLACK_TOKEN") , "token"))
                        .build()))
                .objectMapper(DefaultObjectMapper.get())
                .okHttpClient(okHttpClient)
                .build();
    }

    @Test
    public void postMessage() throws SlackClientException, IOException {
        String viewMessage = ResourceUtils.getResourceAsString("slack/slack_answer_questionnaire_modal_view.json");
        SlackApiViewResponse response = clientFactory.get(KEY).openView("1431377439254.717952128145.73fc940eabfc02b6fcdfbc7e4206f057", viewMessage);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getView().getId());
    }
}