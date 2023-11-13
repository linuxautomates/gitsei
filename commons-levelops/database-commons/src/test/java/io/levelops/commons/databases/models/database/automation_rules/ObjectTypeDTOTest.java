package io.levelops.commons.databases.models.database.automation_rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ObjectTypeDTOTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialization() throws JsonProcessingException {
        ObjectTypeDTO expected = ObjectTypeDTO.builder()
                .id(ObjectType.JIRA_ISSUE.toString())
                .name(ObjectType.JIRA_ISSUE.getName())
                .fields(List.of("body", "title", "comments"))
                .build();
        String serialized = MAPPER.writeValueAsString(expected);
        ObjectTypeDTO actual = MAPPER.readValue(serialized, ObjectTypeDTO.class);
        Assert.assertEquals(expected, actual);
    }
}