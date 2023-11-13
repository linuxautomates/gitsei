package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class WorkItemNotificationTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testSerializeDeSerialize() throws JsonProcessingException {
        WorkItemNotification original = WorkItemNotification.builder()
                .id(UUID.randomUUID())
                .workItemId(UUID.randomUUID())
                .mode(NotificationMode.SLACK)
                .recipient("abc@levelops.io")
                .referenceId(UUID.randomUUID().toString())
                .channelId("C018KV88KG9")
                .build();

        String serialized = MAPPER.writeValueAsString(original);
        WorkItemNotification deSerialized = MAPPER.readValue(serialized, WorkItemNotification.class);
        Assert.assertEquals(original, deSerialized);
    }
}