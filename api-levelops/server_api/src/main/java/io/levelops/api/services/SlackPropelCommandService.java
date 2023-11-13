package io.levelops.api.services;

import io.levelops.api.model.slack.SlackSlashCommandData;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class supports the parsing and execution of the Slack bot slash command /propel
 * which triggers custom propels.
 * For example, the command: /propel My_Event myValueA "my value \"B\"
 * Will run custom trigger playbooks with this input:
 * {
 * "custom_event" : "My_Event",
 * "arg0" : "myValueA",
 * "arg1" : "my value \"B\"",
 * "slack_user_name" : "Maxime",
 * "slack_user_id" : "U456",
 * "slack_channel_name" : "channel-name",
 * "slack_channel_id" : "C123"
 * }
 */
@Log4j2
@Service
public class SlackPropelCommandService {

    private static final String QUOTES = "[\"“”]";
    private static final String QUOTED_STRING_PATTERN = QUOTES + "(?:[^" + QUOTES + "\\\\]|\\\\.)*" + QUOTES;
    private static final Pattern QUOTED_STRING_OR_UNQUOTED_TOKEN = Pattern.compile("(" + QUOTED_STRING_PATTERN + "|\\S+)");
    private final EventsClient eventsClient;

    @Autowired
    public SlackPropelCommandService(EventsClient eventsClient) {
        this.eventsClient = eventsClient;
    }

    public Map<String, Object> executePropelCommand(SlackSlashCommandData commandData) {
        Map<String, Object> eventData = parseCommandDataIntoEventData(commandData);
        if (eventData == null) {
            return Map.of(
                    "response_type", "ephemeral",
                    "text", ":warning: Please provide an event type.\n" +
                            "For example: `/propel my_event`\n" +
                            "You can also provide one or more arguments:\n" +
                            "`/propel my_event 123 \"a b c\" def`");
        }

        int count = 0, failures = 0;
        for (String tenant : commandData.getTenants()) {
            count++;
            try {
                sendCustomTriggerEvent(tenant, eventData);
            } catch (EventsClientException e) {
                log.warn("Error sending trigger event from slack command for tenant={}, commandData={}", tenant, commandData, e);
                failures++;
            }
        }

        if (failures > 0) {
            return Map.of(
                    "response_type", "ephemeral",
                    "text", ":warning: Something went wrong...");
        }

        return Map.of("text", ":done: Propels listening to the `" + eventData.getOrDefault("custom_event", "") + "` custom event will start running soon!");
    }

    public void sendCustomTriggerEvent(String company, Map<String, Object> eventData) throws EventsClientException {
        log.info("Sending custom trigger event from Slack command: tenant={}, data={}", company, eventData);
        eventsClient.emitEvent(company, EventType.CUSTOM_TRIGGER, eventData);
    }

    @Nullable
    public static Map<String, Object> parseCommandDataIntoEventData(SlackSlashCommandData commandData) {
        if (commandData.getCallback() == null) {
            return null;
        }
        String text = commandData.getCallback().getText();
        List<String> tokens = tokenizeCommandText(text);
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("custom_event", tokens.get(0));
        for (int i = 1; i < tokens.size(); i++) {
            data.put("arg" + (i - 1), StringUtils.defaultString(tokens.get(i)));
        }
        if (commandData.getCallback().getChannelId() != null) {
            data.put("slack_channel_id", commandData.getCallback().getChannelId());
        }
        if (commandData.getCallback().getChannelName() != null) {
            data.put("slack_channel_name", commandData.getCallback().getChannelName());
        }
        if (commandData.getCallback().getUserId() != null) {
            data.put("slack_user_id", commandData.getCallback().getUserId());
        }
        if (commandData.getCallback().getUserName() != null) {
            data.put("slack_user_name", commandData.getCallback().getUserName());
        }
        return data;
    }

    /**
     * Tokenize a command text containing a series of strings.
     * Each string can be quoted, like "1 2 \" 3", or unquoted, like Token_123\"abc\"
     */
    public static List<String> tokenizeCommandText(String commandText) {
        if (StringUtils.isBlank(commandText)) {
            return List.of();
        }
        Matcher matcher = QUOTED_STRING_OR_UNQUOTED_TOKEN.matcher(commandText);
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            tokens.add(unescapeQuotedString(matcher.group()));
        }
        return tokens;
    }

    /**
     * Take a quoted String like {@code '"1 2 \" 3"'} and turn it into a regular String like {@code '1 2 " 3'}
     */
    public static String unescapeQuotedString(String str) {
        if (!str.matches(QUOTED_STRING_PATTERN)) {
            return str;
        }
        if (str.length() <= 2) {
            return "";
        }
        return str.substring(1, str.length() - 1)
                .replaceAll("\\\\" + QUOTES, "\"");
    }

}
