package io.levelops.api.model.slack;

import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.FormHttpMessageConverter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackSlashCommandCallback.SlackSlashCommandCallbackBuilder.class)
public class SlackSlashCommandCallback {

    /**
     * token
     * <p>
     * This is a verification token, a deprecated feature that you shouldn't use any more. It was used to verify that requests were legitimately being sent by Slack to your app, but you should use the signed secrets functionality to do this instead.
     */
    @JsonProperty("token")
    String token;

    /**
     * command
     * <p>
     * The command that was typed in to trigger this request. This value can be useful if you want to use a single Request URL to service multiple Slash Commands, as it lets you tell them apart.
     */
    @JsonProperty("command")
    String command;

    /**
     * text
     * <p>
     * This is the part of the Slash Command after the command itself, and it can contain absolutely anything that the user might decide to type. It is common to use this text parameter to provide extra context for the command.
     * <p>
     * You can prompt users to adhere to a particular format by showing them in the Usage Hint field when creating a command.
     */
    @JsonProperty("text")
    String text;

    /**
     * user_id
     * <p>
     * The ID of the user who triggered the command.
     */
    @JsonProperty("user_id")
    String userId;
    /**
     * user_name
     * <p>
     * The plain text name of the user who triggered the command. As above, do not rely on this field as it is being phased out, use the user_id instead.
     */
    @JsonProperty("user_name")
    String userName;
    /**
     * api_app_id
     * <p>
     * Your Slack app's unique identifier. Use this in conjunction with request signing to verify context for inbound requests.
     */
    @JsonProperty("api_app_id")
    String apiAppId;
    /**
     * team_id, enterprise_id, channel_id, etc.
     * <p>
     * These IDs provide context about where the user was in Slack when they triggered your app's command (eg. which workspace, Enterprise Grid, or channel). You may need these IDs for your command response.
     * <p>
     * The various accompanying *_name values provide you with the plain text names for these IDs, but as always you should only rely on the IDs as the names might change arbitrarily.
     * <p>
     * We'll include enterprise_id and enterprise_name parameters on command invocations when the executing workspace is part of an Enterprise Grid.
     */

    @JsonProperty("team_id")
    String teamId;
    @JsonProperty("team_name")
    String teamName;
    @JsonProperty("team_domain")
    String teamDomain;

    @JsonProperty("channel_id")
    String channelId;
    @JsonProperty("channel_name")
    String channelName;

    @JsonProperty("enterprise_id")
    String enterpriseId;
    @JsonProperty("enterprise_name")
    String enterpriseName;
    @JsonProperty("is_enterprise_install")
    Boolean isEnterpriseInstall;

    /**
     * response_url
     * <p>
     * A temporary webhook URL that you can use to generate messages responses.
     */
    @JsonProperty("response_url")
    String responseUrl;

    /**
     * trigger_id
     * <p>
     * A short-lived ID that will let your app open a modal.
     */
    @JsonProperty("trigger_id")
    String triggerId;

    private static final FormHttpMessageConverter FORM_HTTP_MESSAGE_CONVERTER = new FormHttpMessageConverter();

    public static SlackSlashCommandCallback fromFormDataString(String inputMessage) throws IOException {
        Map<String, String> map = FORM_HTTP_MESSAGE_CONVERTER.read(null, new HttpInputMessage() {
            @NotNull
            @Override
            public InputStream getBody() throws IOException {
                return new StringInputStream(inputMessage);
            }

            @NotNull
            @Override
            public HttpHeaders getHeaders() {
                return HttpHeaders.EMPTY;
            }
        }).toSingleValueMap();

        return DefaultObjectMapper.get().convertValue(map, SlackSlashCommandCallback.class);
    }
}