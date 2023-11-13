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

import static io.levelops.integrations.gitlab.models.GitlabUtils.buildDateFormatter;

public class GitlabCommitTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException, ParseException {
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().
                getResource("gitlab/gitlab_api_commit.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<GitlabCommit> response = mapper.readValue(testFile,
                mapper.getTypeFactory().constructCollectionType(List.class, GitlabCommit.class));
        Assert.assertNotNull(response);
        Assert.assertEquals(3, response.size());
        List<String> parents_ids = new ArrayList<>();
        parents_ids.add("836850fcdc54bdb3d1013334e35bb96e177fba33");
        parents_ids.add("b0f0473f87c8ab5b7770dcd9668304ac1d689001");
        GitlabCommit apiRes = GitlabCommit.builder()
                .authorEmail("modogitlab@gmail.com").authorName("Mohit Dokania")
                .authoredDate(GitlabUtils.buildDateFormatter().parse("2020-10-05T13:19:41.000+00:00"))
                .committedDate(GitlabUtils.buildDateFormatter().parse("2020-10-05T13:19:41.000+00:00"))
                .committerEmail("modogitlab@gmail.com").committerName("Mohit Dokania")
                .createdAt(GitlabUtils.buildDateFormatter().parse("2020-10-05T13:19:41.000+00:00"))
                .id("a8678d3fe3c00492d4812e1a2257ef222fc029aa")
                .message("Merge branch 'dev' into 'master'\n\nAdded New Files\n\nSee merge request cognitree1/dummyproject!1")
                .parentIds(parents_ids).shortId("a8678d3f").title("Merge branch 'dev' into 'master'")
                .webUrl("https://gitlab.com/cognitree1/dummyproject/-/commit/a8678d3fe3c00492d4812e1a2257ef222fc029aa")
                .build();
        Assert.assertEquals(response.get(0), apiRes);
    }
}
