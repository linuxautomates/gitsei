package io.levelops.ingestion.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JobTest {

    @Test
    public void deserialize() throws IOException {
        String input = "{\"id\":\"123\"}";
        Job job = DefaultObjectMapper.get().readValue(input, Job.class);
        assertThat(job.getId()).isEqualTo("123");
    }

    @Test
    public void serialize() throws IOException {
        Job input = Job.builder()
                .id("123")
                .build();
        String output = DefaultObjectMapper.get().writeValueAsString(input);
        assertThat(output).contains("123");
    }
}