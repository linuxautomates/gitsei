package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubIssue;
import io.levelops.integrations.gitlab.models.GitlabIssue;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class DbScmIssueTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testFromGithubIssue() throws IOException {
        String data = ResourceUtils.getResourceAsString("github/github_issue.json");
        GithubIssue githubIssue = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(GithubIssue.class));
        DbScmIssue dbScmIssue = DbScmIssue
                .fromGithubIssue(githubIssue, "repo_1", "1");
        Assert.assertEquals("repo_1", dbScmIssue.getRepoId());
        Assert.assertEquals("repo_1", dbScmIssue.getProject());
    }

    @Test
    public void testFromGitlabIssue() throws IOException {
        String data = ResourceUtils.getResourceAsString("gitlab/gitlab_issue.json");
        GitlabIssue gitlabIssue = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(GitlabIssue.class));
        DbScmIssue dbScmIssue = DbScmIssue
                .fromGitlabIssue(gitlabIssue, "repo_1", "1");
        Assert.assertEquals("repo_1", dbScmIssue.getRepoId());
        Assert.assertEquals("repo_1", dbScmIssue.getProject());
    }

}
