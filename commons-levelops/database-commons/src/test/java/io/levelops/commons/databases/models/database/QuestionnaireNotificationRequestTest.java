package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

public class QuestionnaireNotificationRequestTest {
    private final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testSerialize() throws JsonProcessingException {
        QuestionnaireNotificationRequest expected = QuestionnaireNotificationRequest.builder()
                .questionnaireId(UUID.randomUUID())
                .mode(NotificationMode.SLACK)
                .recipients(List.of("blink-ops", "viraj@levelops.io"))
                .requestorType(NotificationRequestorType.SLACK_USER)
                .requestorId("US2JC5ZM9")
                .requestorName("viraj")
                .build();
        String serialized = MAPPER.writeValueAsString(expected);
        QuestionnaireNotificationRequest actual = MAPPER.readValue(serialized, QuestionnaireNotificationRequest.class);
        Assert.assertEquals(expected, actual);
    }
}