package io.levelops.integrations.snyk.models.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.snyk.models.SnykOrg;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SnykApiListOrgsResponseTest {
    @Test
    public void testSerialize() throws URISyntaxException, IOException {
        SnykOrg apiOrg = SnykOrg.builder()
                .id("c85e8605-0a73-443e-a3f0-f21a871761b1")
                .name("testadmin1")
                .slug("testadmin1")
                .url("https://app.snyk.io/org/testadmin1")
                .build();

        List<SnykOrg> orgs = new ArrayList<>();
        orgs.add(apiOrg);

        SnykApiListOrgsResponse listOrgsResponse = SnykApiListOrgsResponse.builder()
                .orgs(orgs).build();

        ObjectMapper mapper = DefaultObjectMapper.get();
        String data = mapper.writeValueAsString(listOrgsResponse);
        Assert.assertNotNull(data);
    }

    @Test
    public void testDeserialize() throws URISyntaxException, IOException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("api_response/Snyk_List_Orgs_Response.json").toURI());
        String data = Files.readString(testFile.toPath());
        ObjectMapper mapper = DefaultObjectMapper.get();
        SnykApiListOrgsResponse listOrgsResponse = mapper.readValue(testFile, SnykApiListOrgsResponse.class);
        Assert.assertNotNull(listOrgsResponse);
        Assert.assertNotNull(listOrgsResponse.getOrgs());
        Assert.assertEquals(listOrgsResponse.getOrgs().size(), 1);

        SnykOrg apiOrg = SnykOrg.builder()
                .id("c85e8605-0a73-443e-a3f0-f21a871761b1")
                .name("testadmin1")
                .slug("testadmin1")
                .url("https://app.snyk.io/org/testadmin1")
                .build();

        Assert.assertEquals(listOrgsResponse.getOrgs().get(0), apiOrg);
    }
}