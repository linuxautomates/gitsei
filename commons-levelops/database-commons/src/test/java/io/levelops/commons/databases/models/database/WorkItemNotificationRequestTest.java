package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

public class WorkItemNotificationRequestTest {
    private final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testSerialize() throws JsonProcessingException {
        WorkItemNotificationRequest expected = WorkItemNotificationRequest.builder()
                .workItemId(UUID.randomUUID())
                .mode(NotificationMode.SLACK)
                .recipients(List.of("blink-ops", "viraj@levelops.io"))
                .requestorType(NotificationRequestorType.SLACK_USER)
                .requestorId("US2JC5ZM9")
                .requestorName("viraj")
                .message("Could you please look into this error?")
                .build();
        String serialized = MAPPER.writeValueAsString(expected);
        WorkItemNotificationRequest actual = MAPPER.readValue(serialized, WorkItemNotificationRequest.class);
        Assert.assertEquals(expected, actual);
    }
}