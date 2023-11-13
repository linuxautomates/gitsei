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



public class GitlabIssueNoteTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException, ParseException {
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().
                getResource("gitlab/gitlab_api_issue_note.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<GitlabIssueNote> response = mapper.readValue(testFile,
                mapper.getTypeFactory().constructCollectionType(List.class, GitlabIssueNote.class));
        Assert.assertNotNull(response);
        Assert.assertEquals(2, response.size());
        GitlabUser author = GitlabUser.builder()
                .username("modo27")
                .state("active")
                .name("Mohit Dokania")
                .id("7199321")
                .avatarUrl("https://secure.gravatar.com/avatar/114f625ba6649f453924a4c43989da1d?s=80&d=identicon")
                .webUrl("https://gitlab.com/modo27")
                .build();

        GitlabIssueNote apiRes = GitlabIssueNote.builder()
                .id("439683391")
                .body("marked this issue as related to #2")
                .createdAt(GitlabUtils.buildDateFormatter().parse("2020-11-02T05:05:00.500Z"))
                .updatedAt(GitlabUtils.buildDateFormatter().parse("2020-11-02T05:05:00.501Z"))
                .system(true)
                .noteableId("73469347")
                .noteableType("Issue")
                .noteableIID("1")
                .resolvable(false)
                .confidential(false)
                .author(author)
                //.commandChanges(Collections.emptyList())
                .build();
        Assert.assertEquals(response.get(0), apiRes);
    }
}
