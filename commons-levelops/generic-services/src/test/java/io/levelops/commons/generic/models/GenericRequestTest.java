package io.levelops.commons.generic.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class GenericRequestTest {
    private final ObjectMapper objectMapper = DefaultObjectMapper.get();
    @Test
    public void testSerializeDeSerialize() throws JsonProcessingException {
        Map<String,Object> data = new HashMap<>();
        data.put("build_name", "Pipeline 1");
        data.put("success", true);

        String payload = objectMapper.writeValueAsString(data);

        GenericRequest genericRequest = GenericRequest.builder()
                .requestType("JenkinsPluginJobRunClearanceRequest")
                .payload(payload)
                .build();

        String seriaized = objectMapper.writeValueAsString(genericRequest);
        Assert.assertNotNull(seriaized);

        GenericRequest actual = objectMapper.readValue(seriaized, GenericRequest.class);
        Assert.assertEquals(actual, genericRequest);
    }

}