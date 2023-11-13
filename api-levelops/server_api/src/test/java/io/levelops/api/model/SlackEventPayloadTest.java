package io.levelops.api.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.levelops.api.model.slack.SlackEventPayload;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class SlackEventPayloadTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testSerializationDeSerialization() throws JsonProcessingException {
        SlackEventPayload original = SlackEventPayload.builder()
                .token("token")
                .challenge("ahksauiy1lwl11u9w1u9")
                .type("url-verfication")
                .teamId("team-id")
                .apiAppId("api-app-id")
                .event(SlackEventPayload.Event.builder().type("name_of_event").user("1234567890.123456").eventTs("UXXXXXXX1").build())
                .authedUsers(List.of("user1", "uer2"))
                .eventId(UUID.randomUUID().toString())
                .eventTime(16786126891L)
                .build();

        String serialized = MAPPER.writeValueAsString(original);
        SlackEventPayload deSerialized = MAPPER.readValue(serialized, SlackEventPayload.class);
        Assert.assertEquals(original, deSerialized);
    }

    @Test
    public void testChatMessageReplyEvent() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("slack_events/slack_chat_message_reply_event.json");
        SlackEventPayload event = MAPPER.readValue(serialized, SlackEventPayload.class);
        Assert.assertNotNull(event);
        Assert.assertEquals("TM3U03S49", event.getTeamId());

        Assert.assertEquals("message", event.getEvent().getType());
        Assert.assertEquals("VA Reply 1", event.getEvent().getText());

        Assert.assertEquals("1601425074.000700", event.getEvent().getTs());
        Assert.assertEquals("1601424946.000100", event.getEvent().getThreadTs());
        Assert.assertEquals("C01B42LQFGD", event.getEvent().getChannel());
    }

    @Test
    public void testChatMessageAutoReplyEvent() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("slack_events/slack_chat_message_auto_reply_event.json");
        SlackEventPayload event = MAPPER.readValue(serialized, SlackEventPayload.class);
        Assert.assertNotNull(event);
        Assert.assertEquals("TM3U03S49", event.getTeamId());

        Assert.assertEquals("message", event.getEvent().getType());
        Assert.assertEquals("This content can't be displayed.", event.getEvent().getText());

        Assert.assertEquals("1602122194.027600", event.getEvent().getTs());
        Assert.assertEquals("1602122194.027600", event.getEvent().getThreadTs());
        Assert.assertEquals("D01342G4ZEY", event.getEvent().getChannel());
        Assert.assertEquals("B012WFFTLF7", event.getEvent().getBotId());
    }

    @Test (expected = MismatchedInputException.class)
    public void testSlackEventCallback1() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("slack_events/slack_event_callback_1.json");
        MAPPER.readValue(serialized, SlackEventPayload.class);
        Assert.fail("MismatchedInputException was expected");
    }
}