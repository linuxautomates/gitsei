package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CiCdJobQualifiedNameTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testSerialization() throws JsonProcessingException {
        List<CiCdJobQualifiedName> expected = List.of(
                CiCdJobQualifiedName.builder().jobName("job-name-0").build(),
                CiCdJobQualifiedName.builder().jobName("job-name-1").instanceName("instance-name-1").build()
        );
        String serialized = MAPPER.writeValueAsString(expected);
        List<CiCdJobQualifiedName> actual = MAPPER.readValue(serialized, MAPPER.getTypeFactory().constructCollectionType(List.class,CiCdJobQualifiedName.class));
        Assert.assertEquals(expected, actual);
    }
}