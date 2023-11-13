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


import static io.levelops.integrations.gitlab.models.GitlabUtils.buildDateFormatter;

public class GitlabBranchTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException, ParseException {
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().
                getResource("gitlab/gitlab_api_branches.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<GitlabBranch> response = mapper.readValue(testFile,
                mapper.getTypeFactory().constructCollectionType(List.class, GitlabBranch.class));
        Assert.assertNotNull(response);
        Assert.assertEquals(2, response.size());

        GitlabCommit gitlabCommit = GitlabCommit.builder()
                .authorEmail("modogitlab@gmail.com").authorName("Mohit Dokania")
                .authoredDate(GitlabUtils.buildDateFormatter().parse("2020-10-05T12:47:05.000+00:00"))
                .committedDate(GitlabUtils.buildDateFormatter().parse("2020-10-05T12:47:05.000+00:00"))
                .committerEmail("modogitlab@gmail.com").committerName("Mohit Dokania")
                .createdAt(GitlabUtils.buildDateFormatter().parse("2020-10-05T12:47:05.000+00:00"))
                .id("b0f0473f87c8ab5b7770dcd9668304ac1d689001").message("Add new file")
                .shortId("b0f0473f").title("Add new file")
                .webUrl("https://gitlab.com/cognitree1/dummyproject/-/commit/b0f0473f87c8ab5b7770dcd9668304ac1d689001")
                .build();

        GitlabBranch apiRes = GitlabBranch.builder()
                .commit(gitlabCommit).canPush(true).defaulted(false)
                .webUrl("https://gitlab.com/cognitree1/dummyproject/-/tree/dev").developersCanMerge(false)
                .developersCanPush(false).merged(true).name("dev").protect(false)
                .build();
        Assert.assertEquals(response.get(0), apiRes);
    }
}
