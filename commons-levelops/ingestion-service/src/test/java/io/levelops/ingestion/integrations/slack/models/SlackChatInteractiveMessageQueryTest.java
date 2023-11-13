package io.levelops.ingestion.integrations.slack.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

public class SlackChatInteractiveMessageQueryTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialize() throws JsonProcessingException {
        SlackChatInteractiveMessageQuery expected = SlackChatInteractiveMessageQuery.builder()
                .integrationKey(IntegrationKey.builder().integrationId("intg1").tenantId("foo").build())
                .workItemId(UUID.randomUUID())
                .questionnaireId(UUID.randomUUID())
                .recipients(List.of("channel1", "viraj@levelops.io"))
                .botName("mogambo")
                .text("[some blocks]")
                .fileUploads(List.of(
                        SlackChatInteractiveMessageQuery.FileUpload.builder().fileName("f1.txt").fileContent("content1").build(),
                        SlackChatInteractiveMessageQuery.FileUpload.builder().fileName("f2.txt").fileContent("content2").build()
                ))
                .build();
        String serialized = MAPPER.writeValueAsString(expected);

        SlackChatInteractiveMessageQuery actual = MAPPER.readValue(serialized, SlackChatInteractiveMessageQuery.class);
        Assert.assertEquals(expected, actual);
    }
}