package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ReviewResponseV10Test {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeserialization() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix_swarm_cloud_review_response_v10_1.json");
        ReviewResponseV10 actual = MAPPER.readValue(content, ReviewResponseV10.class);
        Assert.assertNotNull(actual);
        Assert.assertEquals(1, actual.getData().getReviews().size());
    }
}