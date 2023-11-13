package io.levelops.integrations.slack.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class SlackChatPostMessageResponseTest {
    private final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeSerialize() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("slack/slack_chat_message_post.json");
        SlackApiResponse<SlackChatPostMessageResponse> apiResponse = MAPPER.readValue(serialized,SlackChatPostMessageResponse.getJavaType(MAPPER));
        Assert.assertTrue(apiResponse.isOk());
        Assert.assertEquals("C018KV88KG9", apiResponse.getPayload().getChannel());
        Assert.assertEquals("1600817240.000100", apiResponse.getPayload().getTs());
    }
}