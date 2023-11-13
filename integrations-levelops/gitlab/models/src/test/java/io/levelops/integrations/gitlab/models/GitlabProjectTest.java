package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;



public class GitlabProjectTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException, ParseException {
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().
                getResource("gitlab/gitlab_api_projects.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<GitlabProject> response = mapper.readValue(testFile,
                mapper.getTypeFactory().constructCollectionType(List.class, GitlabProject.class));

        Assert.assertNotNull(response);
        Assert.assertEquals(2, response.size());

        GitlabProject.Namespace namespace = GitlabProject.Namespace.builder()
                .id("9518731").name("Cognitree").path("cognitree1").kind("group")
                .fullPath("cognitree1").webUrl("https://gitlab.com/groups/cognitree1")
                .build();
        List<String> tagList=new ArrayList<>();
        GitlabProject gitlabProject = GitlabProject.builder()
                .id("21420441").name("dummyproject").nameWithNamespace("Cognitree / dummyproject")
                .path("dummyproject").pathWithNamespace("cognitree1/dummyproject")
                .createdAt(GitlabUtils.buildDateFormatter().parse("2020-09-28T12:29:41.325Z")).defaultBranch("master")
                .sshUrlToRepo("git@gitlab.com:cognitree1/dummyproject.git")
                .httpUrlToRepo("https://gitlab.com/cognitree1/dummyproject.git")
                .webUrl("https://gitlab.com/cognitree1/dummyproject").forksCount(0).starCount(0)
                .lastActivityAt(GitlabUtils.buildDateFormatter().parse("2020-10-14T04:33:40.663Z"))
                .namespace(namespace).tagList(tagList)
                .build();
        Assert.assertEquals(response.get(1), gitlabProject);
    }
}
