package io.levelops.commons.databases.models.database;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginResultDTOTest {

    @Test
    public void serialDate() throws IOException {
        String input = "{ \"created_at\" : \"2020-01-02T10:20:30.000Z\" }";
        PluginResultDTO pluginResultDTO = DefaultObjectMapper.get().readValue(input, PluginResultDTO.class);
        assertThat(pluginResultDTO.getCreatedAt()).isEqualTo("2020-01-02T10:20:30Z");

        String output = DefaultObjectMapper.get().writeValueAsString(pluginResultDTO);
        assertThat(output).isEqualTo("{\"tags\":[],\"created_at\":1577960430000}");
    }

    @Test
    public void serialEpoch() throws IOException {
        String input = "{ \"created_at\" : 1577960430000 }";
        PluginResultDTO pluginResultDTO = DefaultObjectMapper.get().readValue(input, PluginResultDTO.class);
        assertThat(pluginResultDTO.getCreatedAt()).isEqualTo("2020-01-02T10:20:30Z");

        String output = DefaultObjectMapper.get().writeValueAsString(pluginResultDTO);
        assertThat(output).isEqualTo("{\"tags\":[],\"created_at\":1577960430000}");
    }
}