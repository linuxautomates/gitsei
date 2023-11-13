package io.levelops.integrations.snyk.models.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

public class SnykApiListProjectsResponseTest {
    @Test
    public void testDeSerealize() throws IOException, URISyntaxException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("api_response/Snyk_List_Projects_Response.json").toURI());
        String data = Files.readString(testFile.toPath());
        ObjectMapper mapper = DefaultObjectMapper.get();
        SnykApiListProjectsResponse listProjectsResponse = mapper.readValue(testFile, SnykApiListProjectsResponse.class);
        Assert.assertNotNull(listProjectsResponse);

    }

}