package io.levelops.api.services;

import io.levelops.api.model.slack.SlackSlashCommandCallback;
import io.levelops.api.model.slack.SlackSlashCommandData;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SlackPropelCommandServiceTest {

    @Test
    public void testUnescapeQuotedString() {
        assertThat(SlackPropelCommandService.unescapeQuotedString("")).isEqualTo("");
        assertThat(SlackPropelCommandService.unescapeQuotedString("123")).isEqualTo("123");
        assertThat(SlackPropelCommandService.unescapeQuotedString("123\"4\"")).isEqualTo("123\"4\"");
        assertThat(SlackPropelCommandService.unescapeQuotedString("\"\"")).isEqualTo("");
        assertThat(SlackPropelCommandService.unescapeQuotedString("\"1 2 3\"")).isEqualTo("1 2 3");
        assertThat(SlackPropelCommandService.unescapeQuotedString("\"45 \\\" 6\"")).isEqualTo("45 \" 6");
        assertThat(SlackPropelCommandService.unescapeQuotedString("“a b \\” c”")).isEqualTo("a b \" c");
    }

    @Test
    public void testParseCommand() {
        testCommand("", List.of());
        testCommand("  Test123  ", List.of("Test123"));
        testCommand("  \"\"  ", List.of(""));
        testCommand("test 1_a!\"a   \"456", List.of("test", "1_a!\"a", "\"456"));
        testCommand("test \"1 2 3\" \"45 \\\" 6\" ", List.of("test", "1 2 3", "45 \" 6"));


        testCommand("test “a b \\” c” “d”", List.of("test", "a b \" c", "d"));
    }

    private void testCommand(String cmd, List<String> expected) {
        List<String> output = SlackPropelCommandService.tokenizeCommandText(cmd);
        System.out.println("'" + cmd + "' -> " + DefaultObjectMapper.writeAsPrettyJson(output));
        assertThat(output).isEqualTo(expected);
    }

    @Test
    public void testParseCommandData() {
        Map<String, Object> out = SlackPropelCommandService.parseCommandDataIntoEventData(SlackSlashCommandData.builder()
                .callback(SlackSlashCommandCallback.builder()
                        .text("my_Event myValueA \"my value \\\"B\\\"\"")
                        .channelName("channel-name")
                        .channelId("C123")
                        .userId("U456")
                        .userName("Maxime")
                        .build())
                .build());
        DefaultObjectMapper.prettyPrint(out);
        assertThat(out).containsExactlyInAnyOrderEntriesOf(Map.of(
                "slack_user_id", "U456",
                "slack_channel_id", "C123",
                "arg1", "my value \"B\"",
                "arg0", "myValueA",
                "custom_event", "my_Event",
                "slack_user_name", "Maxime",
                "slack_channel_name", "channel-name"
        ));

        out = SlackPropelCommandService.parseCommandDataIntoEventData(SlackSlashCommandData.builder()
                .callback(SlackSlashCommandCallback.builder()
                        .text("test")
                        .build())
                .build());
        DefaultObjectMapper.prettyPrint(out);
        assertThat(out).containsExactlyInAnyOrderEntriesOf(Map.of(
                "custom_event", "test"
        ));
    }
}