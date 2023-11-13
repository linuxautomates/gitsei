package io.levelops.notification.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SlackHelperTest {

    private final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testGetPlainTextSlackBlock() throws IOException {
        var helper = new SlackHelper(MAPPER);
        var block = helper.getPlainTextSlackBlock("Hello world!");
        String expectedText = ResourceUtils.getResourceAsString("slack/slack_chat_plain_text_section_block.json");

        var blockMap= MAPPER.readValue(block, List.class);
        var expectedTextMap = MAPPER.readValue(expectedText, List.class);
        assertThat(blockMap).isEqualTo(expectedTextMap);
    }

}