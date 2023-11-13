package io.levelops.notification.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class SlackApiViewResponseTest {
    private final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeSerialize() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("slack/slack_view_open_response.json");
        SlackApiResponse<SlackApiViewResponse> apiResponse = MAPPER.readValue(serialized,SlackApiViewResponse.getJavaType(MAPPER));
        Assert.assertTrue(apiResponse.isOk());
        Assert.assertEquals("VMHU10V25", apiResponse.getPayload().getView().getId());
    }
}