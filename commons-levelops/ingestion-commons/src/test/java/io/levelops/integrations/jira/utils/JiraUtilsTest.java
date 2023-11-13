package io.levelops.integrations.jira.utils;

import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraIssue;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;

public class JiraUtilsTest {

    @Test
    public void test() throws IOException {
        JiraIssue jiraIssue = ResourceUtils.getResourceAsObject("integrations/jira/jira_issue.json", JiraIssue.class);
        Long firstCommentTime = JiraUtils.extractFirstCommentTimeFromJiraIssue(jiraIssue);
        Assertions.assertThat(firstCommentTime).isEqualTo(1611140972);
    }
}
