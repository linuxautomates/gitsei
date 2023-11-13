package io.levelops.commons.databases.utils;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ScmIssueCorrelationUtilTest {

    @Test
    public void testGetWorkitemIdsAndKeys() {
        List<String> workitemIdsAndKeys = ScmIssueCorrelationUtil.extractWorkitems("#4511 Test1");
        List<String> workitemIdsAndKeys1 = ScmIssueCorrelationUtil.extractWorkitems("#LEV-2344 Test2");
        List<String> workitemIdsAndKeys2 = ScmIssueCorrelationUtil.extractWorkitems("lev-1234, #1234, 1234 - my commit message");
        List<String> workitemIdsAndKeys3 = ScmIssueCorrelationUtil.extractWorkitems("878059 - Pt canceled by");
        assertThat(workitemIdsAndKeys).containsExactly("1", "4511");
        assertThat(workitemIdsAndKeys1).containsExactly("2", "2344");
        assertThat(workitemIdsAndKeys2).containsExactly("1234");
        assertThat(workitemIdsAndKeys3).containsExactly("878059");

        List<String> workitemIdsAndKeys4 = ScmIssueCorrelationUtil.extractWorkitems("#3456 Test1");
        List<String> workitemIdsAndKeys5 = ScmIssueCorrelationUtil.extractWorkitems("#LEV-2344 Test2");
        List<String> workitemIdsAndKeys6 = ScmIssueCorrelationUtil.extractWorkitems("LEV-4534 Test3");
        List<String> workitemIdsAndKeys7 = ScmIssueCorrelationUtil.extractWorkitems("LEV-3344:commit message LEV-9011:branch");
        List<String> workitemIdsAndKeys8 = ScmIssueCorrelationUtil.extractWorkitems("LEV-1222: Added time series PD aggregations.");
        assertThat(workitemIdsAndKeys4).containsExactly("1", "3456");
        assertThat(workitemIdsAndKeys5).containsExactly("2", "2344");
        assertThat(workitemIdsAndKeys6).containsExactly("3", "4534");
        assertThat(workitemIdsAndKeys7).containsExactly("3344", "9011");
        assertThat(workitemIdsAndKeys8).containsExactly("1222");
    }

    @Test
    public void testGetJiraKeys() {
        List<String> jiraKeys1 = ScmIssueCorrelationUtil.extractJiraKeys("#4511 Test1");
        List<String> jiraKeys2 = ScmIssueCorrelationUtil.extractJiraKeys("#LEV-2344 Test2");
        List<String> jiraKeys3 = ScmIssueCorrelationUtil.extractJiraKeys("lev-1234, #1234, 1234 - my commit message");
        List<String> jiraKeys4 = ScmIssueCorrelationUtil.extractJiraKeys("878059 - Pt canceled by");
        List<String> jiraKeys5 = ScmIssueCorrelationUtil.extractJiraKeys("LEV-3344:commit message LEV-9011:branch");
        List<String> jiraKeys6 = ScmIssueCorrelationUtil.extractJiraKeys("LEV-1222: Added time series PD aggregations.");
        assertThat(jiraKeys1).containsExactly();
        assertThat(jiraKeys2).containsExactly("LEV-2344");
        assertThat(jiraKeys3).containsExactly("LEV-1234");
        assertThat(jiraKeys4).containsExactly();
        assertThat(jiraKeys5).containsExactly("LEV-3344", "LEV-9011");
        assertThat(jiraKeys6).containsExactly("LEV-1222");
    }

    @Test
    public void extractJiraKeys() {
        assertThat(ScmIssueCorrelationUtil.extractJiraKeys("[LEV-123] Description")).containsExactly("LEV-123");
        assertThat(ScmIssueCorrelationUtil.extractJiraKeys("123-345-LEV-123")).containsExactly("LEV-123");
        assertThat(ScmIssueCorrelationUtil.extractJiraKeys("[I9P-123] Description")).containsExactly("I9P-123");
        assertThat(ScmIssueCorrelationUtil.extractJiraKeys("A1-123")).containsExactly("A1-123");
        assertThat(ScmIssueCorrelationUtil.extractJiraKeys("0A1-123")).containsExactly("A1-123"); // must start with letter
        assertThat(ScmIssueCorrelationUtil.extractJiraKeys("1A-123")).isEmpty(); // starts with letter and followed by one more characters
    }
}