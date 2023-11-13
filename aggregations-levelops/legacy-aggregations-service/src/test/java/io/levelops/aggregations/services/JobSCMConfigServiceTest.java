package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class JobSCMConfigServiceTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testJobSCMConfigService() throws URISyntaxException, IOException {
        JobSCMConfigService service = new JobSCMConfigService(MAPPER);
        File configFile = new File(this.getClass().getClassLoader().getResource("jenkins/config-scm-1.txt").toURI());
        JobSCMConfigService.SCMConfig scmConfig = service.readSCMConfig(configFile);
        Assert.assertNotNull(scmConfig);
        Assert.assertEquals("https://github.com/TechPrimers/jenkins-example.git", scmConfig.getUrl());
        Assert.assertEquals(null, scmConfig.getUserName());

        configFile = new File(this.getClass().getClassLoader().getResource("jenkins/config-scm-2.txt").toURI());
        scmConfig = service.readSCMConfig(configFile);
        Assert.assertNotNull(scmConfig);
        Assert.assertEquals("https://bitbucket.org", scmConfig.getUrl());
        Assert.assertEquals("virajajgaonkar", scmConfig.getUserName());

        File dir = new File(this.getClass().getClassLoader().getResource("jenkins").toURI());
        configFile = new File(dir, "config-does-not-exist.txt");
        scmConfig = service.readSCMConfig(configFile);
        Assert.assertNull(scmConfig);
    }
}