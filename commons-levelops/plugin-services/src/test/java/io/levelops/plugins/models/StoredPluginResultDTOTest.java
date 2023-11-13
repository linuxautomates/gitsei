package io.levelops.plugins.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StoredPluginResultDTOTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialize() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("plugin_results/stored_plugin_results_1.json");
        StoredPluginResultDTO actual = MAPPER.readValue(serialized, StoredPluginResultDTO.class);
        Assert.assertEquals(UUID.fromString("d7ccc7a3-ae6c-4398-9bdd-372be7d5735e"), actual.getResultId());
        Assert.assertEquals("cicd-job-run-stage-step-logs/tenant-foo/2020/09/02/d7ccc7a3-ae6c-4398-9bdd-372be7d5735e", actual.getPluginResultStoragePath());
        Assert.assertNotNull(actual.getPluginResult());
        Assert.assertEquals("jenkins", actual.getPluginResult().getPluginName());
        Assert.assertEquals("monitoring", actual.getPluginResult().getPluginClass());
        Assert.assertEquals("jenkins_config", actual.getPluginResult().getTool());
        Assert.assertEquals("1", actual.getPluginResult().getVersion());
        Assert.assertTrue(CollectionUtils.isEmpty(actual.getPluginResult().getTags()));
        Assert.assertEquals(List.of("1"), actual.getPluginResult().getProductIds());
        Assert.assertTrue(actual.getPluginResult().getSuccessful());
        Assert.assertEquals(Map.of("env", List.of("us", "prod")), actual.getPluginResult().getLabels());
    }
}