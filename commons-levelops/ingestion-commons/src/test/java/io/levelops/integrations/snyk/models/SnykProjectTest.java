package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Date;

public class SnykProjectTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("integrations/snyk/SnykApiProject.json").toURI());
        String data = new String(Files.readAllBytes(testFile.toPath()), StandardCharsets.UTF_8);
        ObjectMapper mapper = Utils.constructObjectMapper();
        SnykProject project = mapper.readValue(testFile, SnykProject.class);
        Assert.assertNotNull(project);

        SnykProject apiProject = SnykProject.builder()
                .id("36c0486e-300c-48ad-95ab-d8dda694c94d")
                .name("testadmin1-levelops/sirix:bundles/sirix-jax-rx/pom.xml")
                .created(Date.from(Instant.parse("2020-01-24T22:55:37.736Z")))
                .origin("github")
                .type("maven")
                .readOnly("false")
                .testFrequency("daily")
                .totalDependencies(18)
                .issueCountsBySeverity(SnykProject.IssueCountsBySeverity.builder().high(2).low(0).medium(4).build())
                .imageTag("0.1.0")
                .remoteRepoUrl("https://github.com/testadmin1-levelops/sirix.git")
                .lastTestedDate(Date.from(Instant.parse("2020-01-31T08:43:18.651Z")))
                .browseUrl("https://app.snyk.io/org/testadmin1/project/36c0486e-300c-48ad-95ab-d8dda694c94d")
                .importingUser(SnykProject.User.builder().id("eac56b5c-589a-41ca-bd2e-ba2711a926c0").name("App Account App Admin").username("testadmin1").email("testadmin1@levelops.io").build())
                .build();
        Assert.assertEquals(project, apiProject);
    }

}