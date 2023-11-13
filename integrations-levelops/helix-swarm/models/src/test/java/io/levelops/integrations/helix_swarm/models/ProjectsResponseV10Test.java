package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ProjectsResponseV10Test {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeserialization() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix_swarm_cloud_projects_v10_1.json");
        ProjectsResponseV10 actual = MAPPER.readValue(content, ProjectsResponseV10.class);
        Assert.assertNotNull(actual);
        Assert.assertEquals(3, actual.getData().getProjects().size());
    }
}