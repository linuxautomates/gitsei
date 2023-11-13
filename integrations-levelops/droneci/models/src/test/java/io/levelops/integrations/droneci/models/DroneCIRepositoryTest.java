package io.levelops.integrations.droneci.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

public class DroneCIRepositoryTest {

    @Test
    public void testDeserialize() throws URISyntaxException, IOException {
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("droneci/droneci_api_repository.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<DroneCIEnrichRepoData> repositories = mapper.readValue(testFile,
                mapper.getTypeFactory().constructCollectionType(List.class, DroneCIEnrichRepoData.class));

        Assert.assertNotNull(repositories);
        Assert.assertEquals(5, repositories.size());
        DroneCIEnrichRepoData repository = DroneCIEnrichRepoData.builder()
                .id(1L)
                .userId(0L)
                .namespace("test-owner")
                .name("elite")
                .slug("test-owner/elite")
                .scm("")
                .gitHttpUrl("https://github.com/test-owner/elite.git")
                .gitSshUrl("git@github.com:test-owner/elite.git")
                .link("https://github.com/test-owner/elite")
                .defaultBranch("master")
                .isPrivate(false)
                .visibility("public")
                .active(false)
                .configPath("")
                .trusted(false)
                .isProtected(false)
                .ignoreForks(false)
                .ignorePullRequests(false)
                .autoCancelPullRequests(false)
                .autoCancelPushes(false)
                .autoCancelRunning(false)
                .timeout(0L)
                .counter(0L)
                .synced(1651038702L)
                .created(1651038702L)
                .updated(1651038702L)
                .version(1L)
                .archived(false)
                .build();
        Assert.assertEquals(repositories.get(0), repository);
    }
}
