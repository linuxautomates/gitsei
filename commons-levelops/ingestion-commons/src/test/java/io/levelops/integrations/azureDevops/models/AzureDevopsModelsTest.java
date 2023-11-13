package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class AzureDevopsModelsTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testRuns() throws IOException {
        Run run = MAPPER.readValue("{\"createdDate\": \"2020-08-12T15:29:37.2865377Z\"}", MAPPER.getTypeFactory().constructType(Run.class));
        Assert.assertNotNull(run.getCreatedDate());
    }

    @Test
    public void testAzureDevopsReleases() throws IOException {
        AzureDevopsRelease release = ResourceUtils.getResourceAsObject("integrations/azureDevops/release-details.json",
                AzureDevopsRelease.class);
        Assert.assertNotNull(release);
        Assert.assertNotNull(release.getDefinition());
        Assert.assertNotNull(release.getEnvironments());
        Assert.assertNotNull(release.getVariables());
        Assert.assertNotNull(release.getVariableGroups());
        Assert.assertNotNull(release.getArtifacts());
    }
}
