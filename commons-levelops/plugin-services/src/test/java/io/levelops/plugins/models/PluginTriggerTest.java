package io.levelops.plugins.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PluginTriggerTest {

    @Test
    public void test() throws IOException {
        ObjectMapper mapper = DefaultObjectMapper.get();
        PluginTrigger trigger = mapper.readValue(ResourceUtils.getResourceAsString("plugins/trigger.json"), PluginTrigger.class);
        assertEquals(UUID.fromString("901e744b-9516-42e1-9a09-6dd378459b98"), trigger.getPluginId(), "Invalid plugin id");
        assertEquals("file", trigger.getTrigger().getType(), "Invalid plugin id");
    }

}