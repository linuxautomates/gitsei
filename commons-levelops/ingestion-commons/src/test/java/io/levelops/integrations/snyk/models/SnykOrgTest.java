package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


public class SnykOrgTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("integrations/snyk/SnykApiOrg.json").toURI());
        String data = new String(Files.readAllBytes(testFile.toPath()), StandardCharsets.UTF_8);
        ObjectMapper mapper = Utils.constructObjectMapper();
        SnykOrg org = mapper.readValue(testFile, SnykOrg.class);
        Assert.assertNotNull(org);

        SnykOrg apiOrg = SnykOrg.builder()
                .id("c85e8605-0a73-443e-a3f0-f21a871761b1")
                .name("testadmin1")
                .slug("testadmin1")
                .url("https://app.snyk.io/org/testadmin1")
                .build();

        Assert.assertEquals(org, apiOrg);
    }
}