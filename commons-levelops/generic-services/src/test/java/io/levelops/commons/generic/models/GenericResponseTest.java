package io.levelops.commons.generic.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class GenericResponseTest {
    private final ObjectMapper objectMapper = DefaultObjectMapper.get();

    @Test
    public void testSerializeDeSerialize() throws JsonProcessingException {
        Map<String ,Object> payload = new HashMap<>();
        payload.put("response", "this is the response");
        GenericResponse expected = GenericResponse.builder()
                .responseType("JenkinsPluginJobRunClearanceResponse")
                .payload(objectMapper.writeValueAsString(payload))
                .build();

        String seriaized = objectMapper.writeValueAsString(expected);
        Assert.assertNotNull(seriaized);

        GenericResponse actual = objectMapper.readValue(seriaized, GenericResponse.class);
        Assert.assertEquals(actual, expected);
    }
}