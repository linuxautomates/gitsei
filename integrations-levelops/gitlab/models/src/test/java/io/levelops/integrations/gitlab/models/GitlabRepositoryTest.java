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

public class GitlabRepositoryTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException {
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("gitlab/gitlab_api_repository.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<GitlabRepository> repositories = mapper.readValue(testFile,
                mapper.getTypeFactory().constructCollectionType(List.class, GitlabRepository.class));
        Assert.assertNotNull(repositories);
        Assert.assertEquals(2, repositories.size());
        GitlabRepository repository = GitlabRepository.builder()
                .id("a1c2a238a965f004ff76978ac1086aa6fe95caea")
                .name(".gitignore")
                .type("blob")
                .path(".gitignore")
                .mode("100644")
                .build();
        Assert.assertEquals(repositories.get(0), repository);
    }
}
