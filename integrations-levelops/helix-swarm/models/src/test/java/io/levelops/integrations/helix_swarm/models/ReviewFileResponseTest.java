package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ReviewFileResponseTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeserialization() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix_swarm_cloud_files_info_1.json");
        ReviewFileResponse actual = MAPPER.readValue(content, ReviewFileResponse.class);
        Assert.assertNotNull(actual);
        Assert.assertEquals(1000, actual.getData().getFiles().size());
    }

}