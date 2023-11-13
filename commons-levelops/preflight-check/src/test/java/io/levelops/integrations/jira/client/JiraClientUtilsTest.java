package io.levelops.integrations.jira.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraApiSearchResult;
import io.levelops.integrations.jira.models.JiraComment;
import io.levelops.integrations.jira.models.JiraCommentsResult;
import io.levelops.integrations.jira.models.JiraCreateMeta;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueChangeLog;
import io.levelops.integrations.jira.models.JiraMyself;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraUser;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JiraClientUtilsTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    List<String> sensitiveFields = List.of("Summary", "summary", "Description", "description",
            "customfield_10021", "customfield_10029", "emailAddress", "comment");

    @Test
    public void test() throws IOException {
        String data = ResourceUtils.getResourceAsString("jira/jira_search_issues.json");
        JiraApiSearchResult jiraApiSearchResult = MAPPER.readValue(data, JiraApiSearchResult.class);
        JiraApiSearchResult sanitizedResults = JiraClientUtils.sanitizeResults(jiraApiSearchResult, sensitiveFields);
        List<JiraIssue> issues = sanitizedResults.getIssues();
        Assertions.assertThat(issues.size()).isEqualTo(50);
        issues.forEach(this::assertEachJiraIssue);
    }

    public void assertEachJiraIssue(JiraIssue issue) {
        Assertions.assertThat(issue.getFields().getSummary()).isNull();
        Assertions.assertThat(issue.getFields().getDescriptionText()).isNullOrEmpty();
        Assertions.assertThat(issue.getFields().getDescription()).isNull();
        Assertions.assertThat(issue.getFields().getDescriptionLength().intValue()).isEqualTo(0);
        if (issue.getChangeLog() != null && CollectionUtils.isNotEmpty(issue.getChangeLog().getHistories())) {
            List<JiraIssueChangeLog.ChangeLogEvent> changeLogEvents = issue.getChangeLog().getHistories();
            changeLogEvents.forEach(this::assertEachChangeLog);
        }
        if (issue.getChangeLog() != null && CollectionUtils.isNotEmpty(issue.getChangeLog().getValues())) {
            List<JiraIssueChangeLog.ChangeLogEvent> changeLogEvents = issue.getChangeLog().getValues();
            changeLogEvents.forEach(this::assertEachChangeLog);
        }
        if (CollectionUtils.isNotEmpty(issue.getFields().getIssueLinks())) {
            issue.getFields().getIssueLinks().forEach(jiraIssueLink -> {
                if (jiraIssueLink.getInwardIssue() != null) {
                    assertEachJiraIssue(jiraIssueLink.getInwardIssue());
                }
                if (jiraIssueLink.getOutwardIssue() != null) {
                    assertEachJiraIssue(jiraIssueLink.getOutwardIssue());
                }
            });
        }
        if (CollectionUtils.isNotEmpty(issue.getFields().getSubtasks())) {
            issue.getFields().getSubtasks().forEach(this::assertEachJiraIssue);
        }
        if (issue.getFields().getDynamicFields().containsKey("parent")) {
            JiraIssue parentJiraIssue = (JiraIssue) issue.getFields().getDynamicFields().get("parent");
            assertEachJiraIssue(parentJiraIssue);
        }
        for (Object dynamicField : issue.getFields().getDynamicFields().values()) {
            assertEachObject(dynamicField, "emailAddress");
        }
    }

    @SuppressWarnings("unchecked")
    private void assertEachObject(Object object, String sensitiveField) {
        if (object == null) {
            return;
        }
        if (object instanceof Map) {
            assertEachMap((Map<String, Object>) object, sensitiveField);
        } else if (object instanceof List) {
            assertEachList((List<Object>) object, sensitiveField);
        }
    }

    private void assertEachMap(Map<String, Object> map, String sensitiveField) {
        if (MapUtils.isEmpty(map)) {
            return;
        }
        if (map.containsKey(sensitiveField)) {
            Assertions.assertThat(map.get(sensitiveField)).isNull();
        }
        for (Object object : map.values()) {
            assertEachObject(object, sensitiveField);
        }
    }

    private void assertEachList(List<Object> list, String sensitiveField) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        for (Object object : list) {
            assertEachObject(object, sensitiveField);
        }
    }

    public void assertEachChangeLog(JiraIssueChangeLog.ChangeLogEvent changeLogEvent) {
        if (changeLogEvent != null && changeLogEvent.getAuthor() != null) {
            Assertions.assertThat(changeLogEvent.getAuthor().getEmailAddress()).isNull();
        }
        List<JiraIssueChangeLog.ChangeLogDetails> items = changeLogEvent.getItems();
        List<JiraIssueChangeLog.ChangeLogDetails> logDetails = items.stream()
                .filter(changeLogDetails -> sensitiveFields.stream()
                        .anyMatch(sensitiveField -> sensitiveField.equalsIgnoreCase(changeLogDetails.getFieldId()) || sensitiveField.equalsIgnoreCase(changeLogDetails.getField())))
                .peek(details -> System.out.println("checking changeLog event field=" + details.getField() + " fieldId=" + details.getFieldId()))
                .collect(Collectors.toList());
        logDetails.forEach(changeLogDetails -> {
            Assertions.assertThat(changeLogDetails.getFrom()).isNull();
            Assertions.assertThat(changeLogDetails.getFromString()).isNull();
            Assertions.assertThat(changeLogDetails.getTo()).isNull();
            Assertions.assertThat(changeLogDetails.getToString()).isNull();
        });
    }

    @Test
    public void testSanitizeJiraMyself() {
        JiraMyself sanitizedJiraMyself = JiraClientUtils.sanitizeJiraMyself(
                JiraMyself.builder().emailAddress("jirauser123@atlassian.com").build(),
                sensitiveFields
        );
        Assert.assertNull(sanitizedJiraMyself.getEmailAddress());
        sanitizedJiraMyself = JiraClientUtils.sanitizeJiraMyself(null, sensitiveFields);
        Assert.assertNull(sanitizedJiraMyself);
        sanitizedJiraMyself = JiraClientUtils.sanitizeJiraMyself(
                JiraMyself.builder().build(),
                sensitiveFields
        );
        Assert.assertNull(sanitizedJiraMyself.getEmailAddress());
        sanitizedJiraMyself = JiraClientUtils.sanitizeJiraMyself(
                JiraMyself.builder().emailAddress("jirauser123@atlassian.com").build(),
                null
        );
        Assert.assertNotNull(sanitizedJiraMyself.getEmailAddress());
    }

    @Test
    public void testSanitizeJiraUser() {
        JiraUser sanitizedJiraUser = JiraClientUtils.sanitizeJiraUser(
                JiraUser.builder().emailAddress("jirauser123@atlassian.com").build(),
                sensitiveFields
        );
        Assert.assertNull(sanitizedJiraUser.getEmailAddress());
        sanitizedJiraUser = JiraClientUtils.sanitizeJiraUser(null, sensitiveFields);
        Assert.assertNull(sanitizedJiraUser);
        sanitizedJiraUser = JiraClientUtils.sanitizeJiraUser(
                JiraUser.builder().build(),
                sensitiveFields
        );
        Assert.assertNull(sanitizedJiraUser.getEmailAddress());
        sanitizedJiraUser = JiraClientUtils.sanitizeJiraUser(
                JiraUser.builder().emailAddress("jirauser123@atlassian.com").build(),
                null
        );
        Assert.assertNotNull(sanitizedJiraUser.getEmailAddress());
    }

    @Test
    public void testSanitizeJiraUsers() {
        JiraUser jiraUser1 = JiraUser.builder().emailAddress("jirauser123@atlassian.com").build();
        JiraUser jiraUser2 = JiraUser.builder().build();
        List<JiraUser> actual = JiraClientUtils.sanitizeJiraUsers(List.of(jiraUser1, jiraUser2), sensitiveFields);
        List<JiraUser> expected = actual.stream()
                .filter(jiraUser -> jiraUser.getEmailAddress() == null)
                .collect(Collectors.toList());
        Assert.assertEquals(actual.size(), expected.size());
    }

    @Test
    public void testSanitizeJiraProject() {
        JiraProject sanitizeJiraProject = JiraClientUtils.sanitizeJiraProject(
                JiraProject.builder().lead(
                        JiraUser.builder().emailAddress("jirauser123@atlassian.com").build()
                ).build(),
                sensitiveFields
        );
        Assert.assertNull(sanitizeJiraProject.getLead().getEmailAddress());
        sanitizeJiraProject = JiraClientUtils.sanitizeJiraProject(
                JiraProject.builder().lead(
                        JiraUser.builder().build()
                ).build(),
                sensitiveFields
        );
        Assert.assertNull(sanitizeJiraProject.getLead().getEmailAddress());
        sanitizeJiraProject = JiraClientUtils.sanitizeJiraProject(
                JiraProject.builder().build(),
                sensitiveFields
        );
        Assert.assertNull(sanitizeJiraProject.getLead());
        sanitizeJiraProject = JiraClientUtils.sanitizeJiraProject(
                JiraProject.builder().build(),
                null
        );
        Assert.assertNull(sanitizeJiraProject.getLead());
    }

    @Test
    public void testSanitizeJiraProjects() {
        JiraProject jiraProject1 = JiraProject.builder().lead(
                JiraUser.builder().emailAddress("jirauser123@atlassian.com").build()
        ).build();
        JiraProject jiraProject2 = JiraProject.builder().lead(
                JiraUser.builder().build()
        ).build();
        JiraProject jiraProject3 = JiraProject.builder().build();
        List<JiraProject> actual = JiraClientUtils.sanitizeJiraProjects(
                List.of(jiraProject1, jiraProject2, jiraProject3), sensitiveFields);
        List<JiraProject> expected = actual.stream()
                .filter(jiraProject -> jiraProject.getLead() == null || jiraProject.getLead().getEmailAddress() == null)
                .collect(Collectors.toList());
        Assert.assertEquals(actual.size(), expected.size());
    }

    @Test
    public void testSanitizeJiraCreateMeta() {
        JiraProject jiraProject1 = JiraProject.builder().lead(
                JiraUser.builder().emailAddress("jirauser123@atlassian.com").build()
        ).build();
        JiraProject jiraProject2 = JiraProject.builder().lead(
                JiraUser.builder().build()
        ).build();
        JiraProject jiraProject3 = JiraProject.builder().build();
        JiraCreateMeta jiraCreateMeta = JiraClientUtils.sanitizeJiraCreateMeta(
                JiraCreateMeta.builder()
                        .projects(List.of(jiraProject1, jiraProject2, jiraProject3))
                        .build(),
                sensitiveFields
        );
        Assert.assertNotNull(jiraCreateMeta);
        Assert.assertNotNull(jiraCreateMeta.getProjects());
        Assert.assertEquals(3,
                jiraCreateMeta.getProjects()
                        .stream()
                        .filter(jiraProject -> jiraProject.getLead() == null || jiraProject.getLead().getEmailAddress() == null)
                        .count());
    }

    @Test
    public void testSanitizeJiraCommentsResult() {
        JiraCommentsResult jiraCommentsResult = JiraClientUtils.sanitizeJiraCommentsResult(
                JiraCommentsResult.builder()
                        .comments(List.of(
                                JiraComment.builder()
                                        .author(JiraUser.builder().emailAddress("jirauser1@atlassian.com").build())
                                        .updateAuthor(JiraUser.builder().emailAddress("jirauser2@atlassian.com").build())
                                        .build(),
                                JiraComment.builder()
                                        .author(JiraUser.builder().emailAddress("jirauser2@atlassian.com").build())
                                        .updateAuthor(JiraUser.builder().emailAddress("jirauser1@atlassian.com").build())
                                        .build()
                        ))
                        .build(),
                sensitiveFields.stream()
                        .filter(sensitiveField -> !sensitiveField.equalsIgnoreCase("comment"))
                        .collect(Collectors.toList())
        );
        Assert.assertNotNull(jiraCommentsResult);
        Assert.assertNotNull(jiraCommentsResult.getComments());
        Assert.assertEquals(2,
                jiraCommentsResult.getComments()
                        .stream()
                        .filter(jiraComment -> jiraComment.getAuthor() == null
                                || jiraComment.getAuthor().getEmailAddress() == null)
                        .filter(jiraComment -> jiraComment.getUpdateAuthor() == null
                                || jiraComment.getUpdateAuthor().getEmailAddress() == null)
                        .count());
    }

    @Test
    public void testSanitizeJiraCommentsResultWithoutComments() {
        JiraCommentsResult jiraCommentsResult = JiraClientUtils.sanitizeJiraCommentsResult(
                JiraCommentsResult.builder()
                        .comments(List.of(
                                JiraComment.builder()
                                        .author(JiraUser.builder().emailAddress("jirauser1@atlassian.com").build())
                                        .updateAuthor(JiraUser.builder().emailAddress("jirauser2@atlassian.com").build())
                                        .build(),
                                JiraComment.builder()
                                        .author(JiraUser.builder().emailAddress("jirauser2@atlassian.com").build())
                                        .updateAuthor(JiraUser.builder().emailAddress("jirauser1@atlassian.com").build())
                                        .build()
                        ))
                        .build(),
                sensitiveFields
        );
        Assert.assertNotNull(jiraCommentsResult);
        Assert.assertNull(jiraCommentsResult.getComments());
    }

}
