package io.levelops.commons.databases.models.database.automation_rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class AutomationRuleHitTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialization() throws JsonProcessingException {
        Instant time = (new Date()).toInstant();
        AutomationRuleHit expected = AutomationRuleHit.builder()
                .id(UUID.randomUUID())
                .objectId("object id")
                .objectType(ObjectType.JIRA_ISSUE)
                .ruleId(UUID.randomUUID())
                .count(53)
                .hitContent("Changing Infra - Security Issue")
                .context(Map.of("criterea 1", "matches"))
                .createdAt(time).updatedAt(time)
                .build();

        String serialized = MAPPER.writeValueAsString(expected);
        AutomationRuleHit actual = MAPPER.readValue(serialized, AutomationRuleHit.class);
        Assert.assertEquals(expected, actual);
    }
}