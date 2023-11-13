package io.levelops.integrations.snyk.models.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.snyk.models.SnykIssues;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

@SuppressWarnings("unused")
public class SnykApiListIssuesResponseTest  {
    @Test
    public void testDeSerealize() throws IOException, URISyntaxException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("api_response/Snyk_List_Issues_Response.json").toURI());
        String data = Files.readString(testFile.toPath());
        ObjectMapper mapper = DefaultObjectMapper.get();
        SnykIssues listIssuesResponse = mapper.readValue(testFile, SnykIssues.class);
        Assert.assertNotNull(listIssuesResponse);
        Assert.assertEquals(93, listIssuesResponse.getIssues().getVulnerabilities().size());
        Assert.assertEquals(9, listIssuesResponse.getIssues().getLicenses().size());
    }

    @Test
    public void testDeSerealize2() throws IOException, URISyntaxException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("api_response/Snyk_List_Issues_Response_2.json").toURI());
        String data = Files.readString(testFile.toPath());
        ObjectMapper mapper = DefaultObjectMapper.get();
        SnykIssues listIssuesResponse = mapper.readValue(testFile, SnykIssues.class);
        Assert.assertNotNull(listIssuesResponse);
        Assert.assertEquals(93, listIssuesResponse.getIssues().getVulnerabilities().size());
        Assert.assertEquals(9, listIssuesResponse.getIssues().getLicenses().size());
    }

    @Test
    public void testDeSerealize3() throws IOException, URISyntaxException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("api_response/Snyk_List_Licenses_Response.json").toURI());
        String data = Files.readString(testFile.toPath());
        ObjectMapper mapper = DefaultObjectMapper.get();
        SnykIssues listIssuesResponse = mapper.readValue(testFile, SnykIssues.class);
        Assert.assertNotNull(listIssuesResponse);
        Assert.assertEquals(0, listIssuesResponse.getIssues().getVulnerabilities().size());
        Assert.assertEquals(3, listIssuesResponse.getIssues().getLicenses().size());

    }

}