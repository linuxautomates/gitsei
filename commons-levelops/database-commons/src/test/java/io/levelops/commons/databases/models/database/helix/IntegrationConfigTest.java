package io.levelops.commons.databases.models.database.helix;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.IntegrationConfig.ConfigEntry;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationConfigTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testRepoIntegrationConfig() throws IOException {
        IntegrationConfig config = parseIntegrationConfig("helix/integration_config.json");
        Assert.assertEquals(3, config.getRepoConfig().size());
    }

    private IntegrationConfig parseIntegrationConfig(String resourceUrl) throws IOException {
        String data = ResourceUtils.getResourceAsString(resourceUrl);
        return MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(IntegrationConfig.class));
    }

    @Test
    public void compareConfigTo() {
        IntegrationConfig a = IntegrationConfig.builder()
                .config(Map.of())
                .build();
        IntegrationConfig b = IntegrationConfig.builder()
                .config(Map.of())
                .build();
        assertThat(a.isConfigEqualTo(b)).isTrue();
        assertThat(b.isConfigEqualTo(a)).isTrue();
        assertThat(a.isConfigEqualTo(null)).isTrue();

        ConfigEntry a1 = ConfigEntry.builder().key("a").name("a1").build();
        ConfigEntry a2 = ConfigEntry.builder().key("a").name("a2").build();
        ConfigEntry b1 = ConfigEntry.builder().key("b").name("b1").build();
        assertThat(a1).isNotEqualTo(a2);
        assertThat(a1.hashCode()).isNotEqualTo(a2.hashCode());
        assertThat(b1).isNotEqualTo(a1);
        assertThat(b1.hashCode()).isNotEqualTo(a1.hashCode());

        assertConfigsEqual(
                Map.of("test", List.of(a1, b1)),
                Map.of(
                        "test", List.of(a1, b1),
                        "test2", List.of()));
        assertConfigsEqual(
                Map.of("test", List.of(b1, a1)),
                Map.of(
                        "test", List.of(a1, b1)));
        assertConfigsNotEqual(
                Map.of("test", List.of(a2, b1)),
                Map.of(
                        "test", List.of(a1, b1)));
        assertConfigsNotEqual(
                Map.of("test", List.of(b1, a1, a2)),
                Map.of(
                        "test", List.of(a1, b1)));
        assertConfigsNotEqual(
                Map.of("test1", List.of(a1, b1)),
                Map.of(
                        "test2", List.of(a1, b1)));
    }

    private void assertConfigsEqual(Map<String, List<ConfigEntry>> a, Map<String, List<ConfigEntry>> b) {
        assertThat(compareConfigs(a, b)).isTrue();
        assertThat(compareConfigs(b, a)).isTrue();
    }

    private void assertConfigsNotEqual(Map<String, List<ConfigEntry>> a, Map<String, List<ConfigEntry>> b) {
        assertThat(compareConfigs(a, b)).isFalse();
        assertThat(compareConfigs(b, a)).isFalse();
    }

    private boolean compareConfigs(Map<String, List<ConfigEntry>> configA, Map<String, List<ConfigEntry>> configB) {
        IntegrationConfig a = IntegrationConfig.builder()
                .config(configA)
                .build();
        IntegrationConfig b = IntegrationConfig.builder()
                .config(configB)
                .build();
        return a.isConfigEqualTo(b);
    }

}
