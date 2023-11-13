package io.levelops.commons.inventory.keys;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationKeyTest {

    @Test
    public void deserialize() throws IOException {
        String input = "{\"tenant_id\":\"coke\",\"integration_id\":\"github123\"}";
        IntegrationKey output = DefaultObjectMapper.get().readValue(input, IntegrationKey.class);
        assertThat(output.getIntegrationId()).isEqualTo("github123");
        assertThat(output.getTenantId()).isEqualTo("coke");
    }

    @Test
    public void serialize() throws IOException {
        IntegrationKey input = new IntegrationKey("coke", "github123");
        String output = DefaultObjectMapper.get().writeValueAsString(input);
        assertThat(output).isEqualToIgnoringWhitespace("{\"tenant_id\":\"coke\",\"integration_id\":\"github123\"}");
    }
}