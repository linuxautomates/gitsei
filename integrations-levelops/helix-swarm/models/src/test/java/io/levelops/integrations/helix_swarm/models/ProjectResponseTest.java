package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ProjectResponseTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialization() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix_swarm_cloud_projects_1.json");
        ProjectResponse actual = MAPPER.readValue(content, ProjectResponse.class);
        Assert.assertNotNull(actual);
    }
}