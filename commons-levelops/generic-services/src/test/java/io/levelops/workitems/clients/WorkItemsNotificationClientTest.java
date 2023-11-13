package io.levelops.workitems.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

public class WorkItemsNotificationClientTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testDeserialize() throws JsonProcessingException {
        String id = UUID.randomUUID().toString();
        Map result = Map.of("id", id);
        String serialized = MAPPER.writeValueAsString(result);
        WorkItemsNotificationClient.QueueResponse response = MAPPER.readValue(serialized, WorkItemsNotificationClient.QueueResponse.class);
        Assert.assertEquals(id, response.getId());
    }
}