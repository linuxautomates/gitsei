package io.levelops.commons.databases.models.database;

import io.levelops.commons.databases.models.database.Plugin.PluginClass;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginTest {
    @Test
    public void serializatonTest() throws IOException {
        var plugin = Plugin.builder()
            .pluginClass(PluginClass.REPORT_FILE)
            .name("Test")
            .custom(false)
            .description("description")
            .gcsPath("gcsPath")
            .tool("myTool")
            .version("1.0")
            .readme(Map.of())
            .build();
        var json = DefaultObjectMapper.get().writeValueAsString(plugin);
        assertThat(json).isNotBlank();
        assertThat(json).isEqualTo(ResourceUtils.getResourceAsString("samples/database/plugin.json"));
    }

    @Test
    public void deSerializationTest() throws IOException{
        var plugin = ResourceUtils.getResourceAsObject("samples/database/plugin.json", Plugin.class);
        var expected = Plugin.builder()
            .pluginClass(PluginClass.REPORT_FILE)
            .name("Test")
            .custom(false)
            .description("description")
            .gcsPath("gcsPath")
            .tool("myTool")
            .version("1.0")
            .readme(Map.of())
            .build();
        assertThat(plugin).isEqualTo(expected);
    }
}