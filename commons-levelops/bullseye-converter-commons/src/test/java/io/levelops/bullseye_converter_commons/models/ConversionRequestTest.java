package io.levelops.bullseye_converter_commons.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class ConversionRequestTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialize() throws JsonProcessingException {
        ConversionRequest expected = ConversionRequest.builder()
                .customer("test")
                .referenceId(UUID.randomUUID().toString())
                .jobRunId(UUID.randomUUID())
                .fileName("abc.cov")
                .build();
        String string = MAPPER.writeValueAsString(expected);
        ConversionRequest actual = MAPPER.readValue(string, ConversionRequest.class);
        Assert.assertEquals(expected, actual);
    }
}