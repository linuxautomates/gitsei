package io.levelops.integrations.azureDevops.models;

import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class BuildTimelineResponseTest {

    private static final String RESPONSE_FILE_NAME = "buildtimeline-response.json";

    @Test
    public void testStageStep() throws IOException {
        BuildTimelineResponse response = ResourceUtils.getResourceAsObject(RESPONSE_FILE_NAME,  BuildTimelineResponse.class);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStages());
    }
}
