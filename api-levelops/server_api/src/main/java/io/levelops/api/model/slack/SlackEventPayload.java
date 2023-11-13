package io.levelops.api.model.slack;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackEventPayload.SlackEventPayloadBuilder.class)
public class SlackEventPayload {
    @JsonProperty("token")
    private final String token;
    @JsonProperty("challenge")
    private final String challenge;
    @JsonProperty("type")
    private final String type;

    @JsonProperty("team_id")
    private final String teamId;
    @JsonProperty("api_app_id")
    private final String apiAppId;

    @JsonProperty("event")
    private final Event event;

    @JsonProperty("authed_users")
    private final List<String> authedUsers;
    @JsonProperty("event_id")
    private final String eventId;
    @JsonProperty("event_time")
    private final Long eventTime;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Event.EventBuilder.class)
    public static class Event {
        @JsonProperty("type")
        private final String type;
        @JsonProperty("event_ts")
        private final String eventTs;
        @JsonProperty("user")
        private final String user;

        @JsonProperty("client_msg_id")
        private final String clientMsgId;
        @JsonProperty("text")
        private final  String text;
        @JsonProperty("ts")
        private final String ts;
        @JsonProperty("team")
        private final String team;
        @JsonProperty("blocks")
        private final List <Block> blocks;
        @JsonProperty("thread_ts")
        private final String threadTs;
        @JsonProperty("parent_user_id")
        private final String parentUserId;
        @JsonProperty("channel")
        private final String channel;
        @JsonProperty("channel_type")
        private final String channelType;

        @JsonProperty("bot_id")
        private final String botId;
    }

    @Value
    @Builder
    @JsonDeserialize(builder = Block.BlockBuilder.class)
    public static class Block {
        @JsonProperty("type")
        private String type;
        @JsonProperty("block_id")
        private String blockId;
        @JsonProperty("elements")
        private final List <Element> elements;
    }

    @Value
    @Builder
    @JsonDeserialize(builder = Element.ElementBuilder.class)
    public static class Element {
        @JsonProperty("type")
        private String type;
        @JsonProperty("text")
        private String text;
        @JsonProperty("elements")
        private final List<Element> elements;
    }

}
