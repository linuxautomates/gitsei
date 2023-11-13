package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;



public class GitlabGroupTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException, ParseException {
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().
                getResource("gitlab/gitlab_api_group.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<GitlabGroup> response = mapper.readValue(testFile,
                mapper.getTypeFactory().constructCollectionType(List.class, GitlabGroup.class));
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.size());
        GitlabGroup apiRes = GitlabGroup.builder()
                .id("9518731").webUrl("https://gitlab.com/groups/cognitree1").name("Cognitree")
                .path("cognitree1").description("").visibility("private")
                .shareWithGroupLock(false).requireTwoFactorAuthentication(false).twoFactorGracePeriod(48)
                .projectCreationLevel("developer")
                .subgroupCreationLevel("maintainer")
                .lfsEnabled(true).defaultBranchProtection(2).requestAccessEnabled(true)
                .fullName("Cognitree").fullPath("cognitree1")
                .createdAt(GitlabUtils.buildDateFormatter().parse("2020-09-28T12:29:27.495Z"))
                .build();
        Assert.assertEquals(response.get(0), apiRes);
    }
}
