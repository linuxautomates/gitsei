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



public class GitlabMergeRequestTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException, ParseException {
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("gitlab/gitlab_api_mr.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<GitlabMergeRequest> response = mapper.readValue(testFile,
                mapper.getTypeFactory().constructCollectionType(List.class, GitlabMergeRequest.class));
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.size());

        GitlabUser merged_by = GitlabUser.builder()
                .id("7199321")
                .name("Mohit Dokania")
                .username("modo27")
                .state("active")
                .avatarUrl("https://secure.gravatar.com/avatar/114f625ba6649f453924a4c43989da1d?s=80&d=identicon")
                .webUrl("https://gitlab.com/modo27")
                .build();
        GitlabUser author = GitlabUser.builder()
                .id("7199321")
                .name("Mohit Dokania")
                .username("modo27")
                .state("active")
                .avatarUrl("https://secure.gravatar.com/avatar/114f625ba6649f453924a4c43989da1d?s=80&d=identicon")
                .webUrl("https://gitlab.com/modo27")
                .build();
        GitlabMergeRequest mergeRequest = GitlabMergeRequest.builder()
                .author(author).mergedBy(merged_by).id(String.valueOf(73237313)).iid(String.valueOf(1))
                .projectId(21420441).title("Added New Files").state("merged")
                .createdAt(GitlabUtils.buildDateFormatter().parse("2020-10-05T12:47:36.813Z"))
                .updatedAt(GitlabUtils.buildDateFormatter().parse("2020-10-05T13:19:42.994Z"))
                .mergedAt(GitlabUtils.buildDateFormatter().parse("2020-10-05T13:19:43.245Z")).closedAt(null)
                .targetBranch("master").sourceBranch("dev").userNotesCount(0).upVotes(0)
                .downVotes(0).description("")
                .build();
        Assert.assertEquals(response.get(0), mergeRequest);
    }
}
