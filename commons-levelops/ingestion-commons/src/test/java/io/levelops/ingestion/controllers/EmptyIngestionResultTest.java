package io.levelops.ingestion.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class EmptyIngestionResultTest {

    @Test
    public void testSerial() throws JsonProcessingException {
        EmptyIngestionResult emptyIngestionResult = new EmptyIngestionResult();
        assertThat(DefaultObjectMapper.get().writeValueAsString(emptyIngestionResult)).isEqualTo("{\"empty\":true}");
    }

    @Test
    public void testDeserial() throws IOException {
        EmptyIngestionResult emptyIngestionResult = DefaultObjectMapper.get().readValue("{\"empty\":true}", EmptyIngestionResult.class);
        assertThat(emptyIngestionResult.isEmpty()).isTrue();
    }

    @Test
    public void testDeserialFromObj() throws IOException {
        Object output = DefaultObjectMapper.get().readValue("{\"empty\":true}", Object.class);
        assertThat(output).isInstanceOf(Map.class);
        assertThat(((Map) output).get("empty")).isEqualTo(true);
    }

    @Test
    public void predicate() throws IOException {
        assertThat(EmptyIngestionResult.isEmpty(DefaultObjectMapper.get().readValue("{\"empty\":true}", Object.class))).isTrue();
        assertThat(EmptyIngestionResult.isEmpty(DefaultObjectMapper.get().readValue("{\"empty\":false}", Object.class))).isFalse();
        assertThat(EmptyIngestionResult.isEmpty(DefaultObjectMapper.get().readValue("{\"empty\":null}", Object.class))).isFalse();
        assertThat(EmptyIngestionResult.isEmpty(new EmptyIngestionResult())).isTrue();
        assertThat(EmptyIngestionResult.isEmpty(null)).isTrue();
    }
}