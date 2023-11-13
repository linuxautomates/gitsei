package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class ObjectTypeObjectIdTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialization() throws JsonProcessingException {
        ObjectTypeObjectId expected = ObjectTypeObjectId.builder()
                .objectType(ObjectType.JIRA_ISSUE).objectId(UUID.randomUUID().toString())
                .build();
        String serialized = MAPPER.writeValueAsString(expected);
        ObjectTypeObjectId actual = MAPPER.readValue(serialized, ObjectTypeObjectId.class);
        Assert.assertEquals(expected, actual);
    }
}