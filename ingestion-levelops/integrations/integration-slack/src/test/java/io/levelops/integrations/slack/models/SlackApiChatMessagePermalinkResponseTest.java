package io.levelops.integrations.slack.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class SlackApiChatMessagePermalinkResponseTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialize() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("slack/slack_chat_message_permalink.json");
        SlackApiChatMessagePermalinkResponse actual = MAPPER.readValue(serialized, SlackApiChatMessagePermalinkResponse.class);
        Assert.assertNotNull(actual);

        SlackApiChatMessagePermalinkResponse expected = SlackApiChatMessagePermalinkResponse.builder()
                .permalink("https://levelopsworkspace.slack.com/archives/G01C14VKRJP/p1602592157000100")
                .channel("G01C14VKRJP")
                .build();

        Assert.assertEquals(expected, actual);
    }


}