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



public class GitlabIssueTest {
    @Test
    public void testDeserialize() throws IOException, URISyntaxException, ParseException {
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("gitlab/gitlab_api_issues.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<GitlabIssue> response = mapper.readValue(testFile,
                mapper.getTypeFactory().constructCollectionType(List.class, GitlabIssue.class));
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.size());
        List<GitlabUser> assignees = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        labels.add("Novice");

        GitlabUser singleAssignee = GitlabUser.builder().id("7199321").name("Mohit Dokania").state("active")
                .avatarUrl("https://secure.gravatar.com/avatar/114f625ba6649f453924a4c43989da1d?s=80&d=identicon")
                .username("modo27")
                .webUrl("https://gitlab.com/modo27")
                .build();
        assignees.add(singleAssignee);
        GitlabUser author = GitlabUser.builder()
                .avatarUrl("https://secure.gravatar.com/avatar/114f625ba6649f453924a4c43989da1d?s=80&d=identicon")
                .id("7199321").name("Mohit Dokania").state("active").username("modo27")
                .webUrl("https://gitlab.com/modo27")
                .build();
        GitlabUser assignee = GitlabUser.builder()
                .avatarUrl("https://secure.gravatar.com/avatar/114f625ba6649f453924a4c43989da1d?s=80&d=identicon")
                .id("7199321").name("Mohit Dokania").state("active").username("modo27")
                .webUrl("https://gitlab.com/modo27")
                .build();
        GitlabIssue.TimeEstimate timeEstimate = GitlabIssue.TimeEstimate.builder()
                .timeEstimate(0)
                .totalTimeSpent(0)
                .build();
        GitlabIssue.TaskCompletionStatus status = GitlabIssue.TaskCompletionStatus.builder()
                .count(1)
                .completedCount(0)
                .build();
        GitlabIssue apiRes = GitlabIssue.builder()
                .assignee(assignee).assignees(assignees).author(author)
                .id("71798354").iid("13").labels(labels)
                .confidential(false).upVotes(0).downVotes(0)
                .createdAt(GitlabUtils.buildDateFormatter().parse("2020-09-28T12:29:44.513Z"))
                .description("At any point while using the free version of GitLab you can start a trial of GitLab Gold for free for 30 days.")
                .hasTasks(true).mergeRequestsCount(0).projectId("21420443")
                .state("opened").taskCompletionStatus(status).timeStats(timeEstimate)
                .title("Start a free trial of GitLab Gold - no credit card required :rocket:")
                .userNotesCount(0).updatedAt(GitlabUtils.buildDateFormatter().parse("2020-09-28T12:29:44.513Z"))
                .webUrl("https://gitlab.com/cognitree1/learn-gitlab/-/issues/13")
                .build();
        Assert.assertEquals(response.get(0), apiRes);
    }
}
