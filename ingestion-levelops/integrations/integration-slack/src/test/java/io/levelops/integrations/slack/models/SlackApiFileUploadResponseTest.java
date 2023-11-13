package io.levelops.integrations.slack.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class SlackApiFileUploadResponseTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialize() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("slack_files/slack_file_upload_1.json");
        SlackApiFileUploadResponse actual = MAPPER.readValue(serialized, SlackApiFileUploadResponse.class);
        Assert.assertNotNull(actual);

        SlackApiFileUpload file = SlackApiFileUpload.builder()
                .id("F01C9KW2KQU")
                .build();

        SlackApiFileUploadResponse expected = SlackApiFileUploadResponse.builder()
                .file(file)
                .build();

        Assert.assertEquals(expected, actual);
    }
}