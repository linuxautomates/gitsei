package io.levelops.internal_api.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginsSpecTest {

    @Test
    public void desrial() throws JsonProcessingException {
        String input = "{\n" +
                "  \"plugins\": {\n" +
                "    \"jenkins_config\": {\n" +
                "      \"tool\": \"jenkins_config\",\n" +
                "      \"paths\": [\n" +
                "        \"/config\",\n" +
                "        \"/users\",\n" +
                "        \"/plugins\"\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";
        PluginsSpec o = DefaultObjectMapper.get().readValue(input, PluginsSpec.class);
        System.out.println(o);
        assertThat(o.getPlugins()).containsEntry("jenkins_config", PluginsSpec.PluginSpec.builder()
                .tool("jenkins_config")
                .paths(List.of("/config", "/users", "/plugins"))
                .build());
    }
}