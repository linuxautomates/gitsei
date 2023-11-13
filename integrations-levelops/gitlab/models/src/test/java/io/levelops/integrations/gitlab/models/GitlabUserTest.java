package io.levelops.integrations.gitlab.models;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

public class GitlabUserTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException {
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().
                getResource("gitlab/gitlab_api_user.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<GitlabUser> response = mapper.readValue(testFile,
                mapper.getTypeFactory().constructCollectionType(List.class, GitlabUser.class));
        Assert.assertNotNull(response);
        Assert.assertEquals(20, response.size());
        GitlabUser apiRes = GitlabUser.builder()
                .id("7416586").name("890781FBMpA 890781FBMpA").username("890781FBMpA").state("active")
                .avatarUrl("https://secure.gravatar.com/avatar/da64d077aa5f92309559a9c3afd7f21b?s=80&d=identicon")
                .webUrl("https://gitlab.com/890781FBMpA")
                .build();
        Assert.assertEquals(response.get(0), apiRes);
    }
}
