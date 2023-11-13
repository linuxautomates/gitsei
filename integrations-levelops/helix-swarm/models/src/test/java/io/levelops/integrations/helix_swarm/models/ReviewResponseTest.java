package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ReviewResponseTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeserialization() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix_swarm_cloud_reviews_1.json");
        ReviewResponse actual = MAPPER.readValue(content, ReviewResponse.class);
        Assert.assertNotNull(actual);
    }
}