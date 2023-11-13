package io.levelops.bullseye_converter_commons.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class ResultTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialize() throws JsonProcessingException {
        Result expected = Result.builder()
                .success(true)
                .standardOutput("std output")
                .errorOutput("err output")
                .timedOut(false)
                .exitCode(0)
                .build();
        String string = MAPPER.writeValueAsString(expected);
        Result actual = MAPPER.readValue(string, Result.class);
        Assert.assertEquals(expected, actual);
    }
}