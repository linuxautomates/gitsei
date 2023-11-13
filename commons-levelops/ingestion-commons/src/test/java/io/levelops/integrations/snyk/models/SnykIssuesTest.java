package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SnykIssuesTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("integrations/snyk/SnykApiIssue.json").toURI());
        String data = new String(Files.readAllBytes(testFile.toPath()), StandardCharsets.UTF_8);
        ObjectMapper mapper = Utils.constructObjectMapper();
        SnykIssues issues = mapper.readValue(testFile, SnykIssues.class);
        Assert.assertNotNull(issues);
        Assert.assertEquals(24, issues.getDependencyCount().intValue());
        Assert.assertEquals("maven", issues.getPackageManager());

        Assert.assertEquals(93, issues.getIssues().getVulnerabilities().size());
        Assert.assertEquals(9, issues.getIssues().getLicenses().size());
    }

    @Test
    public void testDeserialize2() throws URISyntaxException, IOException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("integrations/snyk/SnykApiIssue2.json").toURI());
        String data = new String(Files.readAllBytes(testFile.toPath()), StandardCharsets.UTF_8);
        ObjectMapper mapper = Utils.constructObjectMapper();
        SnykIssues issues = mapper.readValue(testFile, SnykIssues.class);
        Assert.assertNotNull(issues);
        Assert.assertEquals(24, issues.getDependencyCount().intValue());
        Assert.assertEquals("maven", issues.getPackageManager());

        Assert.assertEquals(93, issues.getIssues().getVulnerabilities().size());
        Assert.assertEquals(9, issues.getIssues().getLicenses().size());

        String serializedData = mapper.writeValueAsString(issues);
        Assert.assertNotNull(serializedData);
    }

}