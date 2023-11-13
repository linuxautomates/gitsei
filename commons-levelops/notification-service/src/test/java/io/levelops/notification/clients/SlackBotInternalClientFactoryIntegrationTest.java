package io.levelops.notification.clients;

import com.google.common.base.MoreObjects;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.notification.models.SlackChatPostMessageResponse;
import io.levelops.notification.utils.SlackHelper;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class SlackBotInternalClientFactoryIntegrationTest {

    private SlackBotInternalClientFactory clientFactory;

    @Before
    public void setUp() throws Exception {
        OkHttpClient okHttpClient = new OkHttpClient();
        var token = MoreObjects.firstNonNull(System.getenv("SLACK_TOKEN") , "token");
        clientFactory = SlackBotInternalClientFactory.builder()
                .objectMapper(DefaultObjectMapper.get())
                .okHttpClient(okHttpClient)
                .token(MoreObjects.firstNonNull(System.getenv("SLACK_TOKEN") , "token"))
                .build();
    }

    @Test
    public void postMessage() throws SlackClientException, IOException {
        var helper = new SlackHelper(DefaultObjectMapper.get());
        String text = helper.getPlainTextSlackBlock("Hello world");
        SlackChatPostMessageResponse response = clientFactory.get().postChatInteractiveMessage("C03P3JNU9JL", text, "MyBot");
        Assert.assertNotNull(response);
    }
}
