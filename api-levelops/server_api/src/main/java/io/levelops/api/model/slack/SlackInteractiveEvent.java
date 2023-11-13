package io.levelops.api.model.slack;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackInteractiveEvent.SlackInteractiveEventBuilder.class)
public class SlackInteractiveEvent {
    @JsonProperty("type")
    private final String type;
    @JsonProperty("user")
    private final User user;
    @JsonProperty("api_app_id")
    private final String apiAppId;
    @JsonProperty("token")
    private final String token;
    @JsonProperty("container")
    private final Container container;
    @JsonProperty("trigger_id")
    private final String triggerId;
    @JsonProperty("team")
    private final Team team;
    @JsonProperty("channel")
    private final Channel channel;
    @JsonProperty("message")
    private final Message message;
    @JsonProperty("response_url")
    private final String responseUrl;
    @JsonProperty("actions")
    private final List<Action> actions;
    @JsonProperty("view")
    private final View view;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = User.UserBuilder.class)
    public static class User {
        @JsonProperty("id")
        private String id;
        @JsonProperty("username")
        private String username;
        @JsonProperty("name")
        private String name;
        @JsonProperty("team_id")
        private String teamId;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Container.ContainerBuilder.class)
    public static class Container {
        @JsonProperty("type")
        private String type;
        @JsonProperty("message_ts")
        private String messageTs;
        @JsonProperty("channel_id")
        private String channelId;
        @JsonProperty("is_ephemeral")
        private boolean isEphemeral;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Team.TeamBuilder.class)
    public static class Team {
        @JsonProperty("id")
        private String id;
        @JsonProperty("domain")
        private String domain;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Channel.ChannelBuilder.class)
    public static class Channel {
        @JsonProperty("id")
        private String id;
        @JsonProperty("name")
        private String name;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Action.ActionBuilder.class)
    public static class Action {
        @JsonProperty("type")
        private final String type;
        @JsonProperty("action_id")
        private final String actionId;
        @JsonProperty("block_id")
        private final String blockId;
        @JsonProperty("text")
        private final Text text;

        @JsonProperty("value")
        private final String value;

        @JsonProperty("selected_user")
        private final String selectedUser;

        @JsonProperty("selected_option")
        private final SelectedOption selectedOption;

        @JsonProperty("selected_conversations")
        private final List<String> selectedConversations;

        @JsonProperty("action_ts")
        private final String actionTs;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SelectedOption.SelectedOptionBuilder.class)
    public static class SelectedOption {
        @JsonProperty("value")
        private final String value;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Message.MessageBuilder.class)
    public static class Message {
        @JsonProperty("type")
        private final String type;
        @JsonProperty("subtype")
        private final String subtype;
        @JsonProperty("text")
        private final String text;
        @JsonProperty("ts")
        private final String ts;
        @JsonProperty("username")
        private final String username;
        @JsonProperty("bot_id")
        private final String botId;
        @JsonProperty("blocks")
        private final List<Block> blocks;
        @JsonProperty("user")
        private final String user;
        @JsonProperty("team")
        private final String team;

    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Block.BlockBuilder.class)
    public static class Block {
        @JsonProperty("type")
        private final String type;
        @JsonProperty("block_id")
        private final String blockId;
        @JsonProperty("text")
        private final Text text;
        @JsonProperty("accessory")
        private final Accessory accessory;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Text.TextBuilder.class)
    public static class Text {
        @JsonProperty("type")
        private final String type;
        @JsonProperty("text")
        private final String text;
        @JsonProperty("verbatim")
        private final Boolean verbatim;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Accessory.AccessoryBuilder.class)
    public static class Accessory {
        @JsonProperty("type")
        private final String type;
        @JsonProperty("action_id")
        private final String actionId;
        @JsonProperty("value")
        private final String value;
        @JsonProperty("text")
        private final Text text;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TextControl.TextControlBuilder.class)
    public static class TextControl {
        @JsonProperty("type")
        private final String type;
        @JsonProperty("text")
        private final String text;
        @JsonProperty("emoji")
        private final Boolean emoji;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ViewState.ViewStateBuilder.class)
    public static class ViewState {
        @JsonProperty("values")
        private final Map<String, ViewStateValue> values;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ViewStateValue.ViewStateValueBuilder.class)
    public static class ViewStateValue {
        @JsonProperty("custom_action_id")
        private final ViewStateValueAction customActionId;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ViewStateValueAction.ViewStateValueActionBuilder.class)
    public static class ViewStateValueAction {
        @JsonProperty("type")
        private final String type;

        @JsonProperty("selected_options")
        private final List<SelectedOption> selectedOptions;

        @JsonProperty("selected_option")
        private final SelectedOption selectedOption;

        @JsonProperty("value")
        private final String value;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = View.ViewBuilder.class)
    public static class View {
        @JsonProperty("id")
        private String id;
        @JsonProperty("username")
        private String username;
        @JsonProperty("name")
        private String name;
        @JsonProperty("team_id")
        private String teamId;
        @JsonProperty("type")
        private final String type;

        @JsonProperty("callback_id")
        private final String callbackId;
        @JsonProperty("private_metadata")
        private final String privateMetadata;

        @JsonProperty("hash")
        private final String hash;

        @JsonProperty("clear_on_close")
        private final Boolean clearOnClose;
        @JsonProperty("notify_on_close")
        private final Boolean notifyOnClose;

        @JsonProperty("root_view_id")
        private final String rootViewId;
        @JsonProperty("app_id")
        private final String appId;
        @JsonProperty("external_id")
        private final String externalId;
        @JsonProperty("app_installed_team_id")
        private final String appInstalledTeamId;
        @JsonProperty("bot_id")
        private final String botId;

        @JsonProperty("title")
        private final TextControl title;
        @JsonProperty("submit")
        private final TextControl submit;

        @JsonProperty("state")
        private final ViewState state;


    }
}