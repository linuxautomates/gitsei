package io.levelops.commons.databases.models.database.jira.parsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraStoryPoints;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser.ParsedChangelog;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueChangeLog.ChangeLogEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueParserTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    List<JiraIssue> jiraIssues;
    List<JiraIssue> customTagJiraIssue;

    @Before
    public void setUp() throws Exception {
        jiraIssues = ResourceUtils.getResourceAsObject("samples/database/jira_issues.json", MAPPER.getTypeFactory().constructCollectionType(List.class, JiraIssue.class));
        customTagJiraIssue = ResourceUtils.getResourceAsObject("samples/database/jira_issues_with_custom_tags.json", MAPPER.getTypeFactory().constructCollectionType(List.class, JiraIssue.class));
    }

    @Test
    public void test() throws IOException {
        Date currentTime = new Date();
        for (JiraIssue jiraIssue : jiraIssues) {
            DbJiraIssue dbJiraIssue = JiraIssueParser.parseJiraIssue(jiraIssue, "1",
                    currentTime, null);
            Assert.assertNotNull(dbJiraIssue);
        }
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testSprintInCustomField() throws IOException {
        Date currentTime = new Date();
        List<DbJiraIssue> dbJiraIssues = new ArrayList<>();
        IntegrationConfig.ConfigEntry config1 = IntegrationConfig.ConfigEntry.builder()
                .key("customfield_10004").name("Sprint").build();
        IntegrationConfig.ConfigEntry config2 = IntegrationConfig.ConfigEntry.builder()
                .key("customfield_10605").name("LEV-3474").delimiter(",").build();
        List<IntegrationConfig.ConfigEntry> config = List.of(config1, config2);

        for (JiraIssue jiraIssue : jiraIssues) {
            DbJiraIssue dbJiraIssue = JiraIssueParser.parseJiraIssue(jiraIssue, "1",
                    currentTime, JiraIssueParser.JiraParserConfig.builder().customFieldConfig(config).build());
            dbJiraIssues.add(dbJiraIssue);
        }
        Assert.assertEquals(dbJiraIssues.size(), 3);

        Assert.assertEquals(1, dbJiraIssues.get(0).getCustomFields().size());
        Assert.assertEquals(3, ((List) (dbJiraIssues.get(0).getCustomFields().get("customfield_10605"))).size());
        Assert.assertEquals(List.of("RQIUskUtcqTk4W6LZgS26JcZLTqv5ZS1ttkhS3eqw3WMqPCdiS", "jM8rAMU3VnYz7qm9QR2Br1jVYMr3mPat9mXgwFy8oncRNhoMPk", "sJ6IoGQe5paMDaSPwLqilBkWmuDaMz839cOu2ZZ6ZYQtviQL1S"), ((List) (dbJiraIssues.get(0).getCustomFields().get("customfield_10605"))));

        Assert.assertEquals(((List) (dbJiraIssues.get(1).getCustomFields().get("customfield_10004"))).size(), 2);
        Assert.assertEquals(((List) (dbJiraIssues.get(2).getCustomFields().get("customfield_10004"))).size(), 1);
    }

    @Test
    public void testParseChangeLogEmpty() throws IOException {
        JiraIssue issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_no_changelog.json", JiraIssue.class);
        ParsedChangelog output = JiraIssueParser.parseChangelog("1", issue, "to do", "1234", DbJiraIssue.UNKNOWN, 0L, "customfield_10016", null, null);

        DefaultObjectMapper.prettyPrint(output);
        assertThat(output.getHops()).isEqualTo(0);
        assertThat(output.getBounces()).isEqualTo(0);
        assertThat(output.getOldestStatus()).isEqualTo("to do");
        assertThat(output.getOldestStatusId()).isEqualTo("1234");
        assertThat(output.getOldestAssignee()).isEqualTo("_UNKNOWN_");
        assertThat(output.getAssigneeTimeIterator()).isEqualTo(0L);
        assertThat(output.getAssigneeSet()).isEmpty();
        assertThat(output.getAssigneesList()).isEmpty();
        assertThat(output.getStatusLogs()).isEmpty();
        assertThat(output.getStoryPointsLogs()).isEmpty();
        assertThat(output.getSprintLogs()).isEmpty();

    }

    @Test
    public void testParseChangeLog1StoryPointsChange() throws IOException {
        var issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_1_story_points_change.json", JiraIssue.class);
        var output = JiraIssueParser.parseChangelog("1", issue, "to do", "1234", DbJiraIssue.UNKNOWN, 0L, "customfield_10016", null, null);

        DefaultObjectMapper.prettyPrint(output);
        assertThat(output.getHops()).isEqualTo(0);
        assertThat(output.getBounces()).isEqualTo(0);
        assertThat(output.getOldestAssignee()).isEqualTo("_UNKNOWN_");
        assertThat(output.getAssigneeTimeIterator()).isEqualTo(0L);
        assertThat(output.getAssigneeSet()).isEmpty();
        assertThat(output.getAssigneesList()).isEmpty();
        assertThat(output.getStatusLogs()).isEmpty();
        assertThat(output.getSprintLogs()).isEmpty();
        assertThat(output.getStoryPointsLogs()).containsExactly(JiraIssueParser.JiraLogItem.builder()
                .fromString("1")
                .toString("42")
                .time(1618281387L)
                .build());

    }

    @Test
    public void testParseChangeLogStatusAssigneeStoryPointsChanges() throws IOException {
        var issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_status_assignee_storypoints_changes.json", JiraIssue.class);
        var output = JiraIssueParser.parseChangelog("1", issue, "to do", "1234", "Maxime", 42L, "customfield_10016", null, null);

        DefaultObjectMapper.prettyPrint(output);
        assertThat(output.getHops()).isEqualTo(1);
        assertThat(output.getBounces()).isEqualTo(0);
        assertThat(output.getOldestAssignee()).isEqualTo("_UNASSIGNED_");
        assertThat(output.getAssigneeTimeIterator()).isEqualTo(1618291941L);
        assertThat(output.getAssigneeSet()).containsExactlyInAnyOrder("Harsh Jariwala", "Maxime");
        assertThat(output.getAssigneesList()).containsExactlyInAnyOrder(DbJiraAssignee.builder()
                        .assignee("Maxime")
                        .issueKey("DEVTEST-1")
                        .integrationId("1")
                        .startTime(1618291954L)
                        .endTime(42L)
                        .build(),
                DbJiraAssignee.builder()
                        .assignee("Harsh Jariwala")
                        .issueKey("DEVTEST-1")
                        .integrationId("1")
                        .startTime(1618291941L)
                        .endTime(1618291954L)
                        .build());
        assertThat(output.getStatusLogs()).containsExactly(JiraIssueParser.JiraLogItem.builder()
                .fromString("TO DO")
                .from("10063")
                .toString("DONE")
                .to("10065")
                .time(1618291991L)
                .build());
        assertThat(output.getStoryPointsLogs()).containsExactly(JiraIssueParser.JiraLogItem.builder()
                .fromString("1")
                .toString("42")
                .time(1618281387L)
                .build());
        assertThat(output.getSprintLogs()).isEmpty();
    }

    @Test
    public void testParseChangeLogManyStatuses() throws IOException {
        var issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_many_status_changes.json", JiraIssue.class);
        var output = JiraIssueParser.parseChangelog("1", issue, "to do", "1234", "Maxime", 42L, "customfield_10016", null, null);


        DefaultObjectMapper.prettyPrint(output);
        assertThat(output.getStatusLogs()).containsExactly(
                new JiraIssueParser.JiraLogItem("10046", "IN STAGING", "3", "IN PROGRESS", 1618384332L),
                new JiraIssueParser.JiraLogItem("3", "IN PROGRESS", "10046", "IN STAGING", 1618381944L),
                new JiraIssueParser.JiraLogItem("10046", "IN STAGING", "3", "IN PROGRESS", 1618363700L),
                new JiraIssueParser.JiraLogItem("3", "IN PROGRESS", "10046", "IN STAGING", 1618356101L),
                new JiraIssueParser.JiraLogItem("10000", "BACKLOG", "3", "IN PROGRESS", 1618297892L),
                new JiraIssueParser.JiraLogItem("3", "IN PROGRESS", "10000", "BACKLOG", 1618029774L),
                new JiraIssueParser.JiraLogItem("10000", "BACKLOG", "3", "IN PROGRESS", 1617904069L)
        );
        assertThat(output.getOldestStatus()).isEqualTo("BACKLOG");
        assertThat(output.getOldestStatusId()).isEqualTo("10000");

        List<DbJiraStatus> statuses = JiraIssueParser.parseStatusLogs(output.getStatusLogs(), output.getOldestStatus(), output.getOldestStatusId(), 42L, Instant.ofEpochSecond(999));
        DefaultObjectMapper.prettyPrint(statuses);

        assertThat(statuses).containsExactlyInAnyOrder(
                new DbJiraStatus("BACKLOG", "10000", null, null, 1617904069L, 42L, null),
                new DbJiraStatus("IN PROGRESS", "3", null, null, 1618029774L, 1617904069L, null),
                new DbJiraStatus("BACKLOG", "10000", null, null, 1618297892L, 1618029774L, null),
                new DbJiraStatus("IN PROGRESS", "3", null, null, 1618356101L, 1618297892L, null),
                new DbJiraStatus("IN STAGING", "10046", null, null, 1618363700L, 1618356101L, null),
                new DbJiraStatus("IN PROGRESS", "3", null, null, 1618381944L, 1618363700L, null),
                new DbJiraStatus("IN STAGING", "10046", null, null, 1618384332L, 1618381944L, null),
                new DbJiraStatus("IN PROGRESS", "3", null, null, 999L, 1618384332L, null)
        );
    }

    @Test
    public void testParseStoryPointsLogsWithoutChange() throws IOException {
        var issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_no_changelog.json", JiraIssue.class);
        var output = JiraIssueParser.parseChangelog("1", issue, "to do", "1234", "Maxime", 42L, "customfield_10016", null, null);

        List<DbJiraStoryPoints> dbJiraStoryPoints = JiraIssueParser.parseStoryPointsLogs(output.getStoryPointsLogs(), "1", "DEVTEST-1", 100, 1000L, 9999999999L);
        DefaultObjectMapper.prettyPrint(dbJiraStoryPoints);
        assertThat(dbJiraStoryPoints).containsExactly(DbJiraStoryPoints.builder()
                .integrationId("1")
                .issueKey("DEVTEST-1")
                .storyPoints(100)
                .startTime(1000L)
                .endTime(9999999999L)
                .build());
    }

    @Test
    public void testParseStoryPointsLogsWithChange() throws IOException {
        var issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_status_assignee_storypoints_changes.json", JiraIssue.class);
        var output = JiraIssueParser.parseChangelog("1", issue, "to do", "1234", "Maxime", 42L, "customfield_10016", null, null);

        List<DbJiraStoryPoints> dbJiraStoryPoints = JiraIssueParser.parseStoryPointsLogs(output.getStoryPointsLogs(), "1", "DEVTEST-1", 100, 1000L, 9999999999L);
        DefaultObjectMapper.prettyPrint(dbJiraStoryPoints);
        assertThat(dbJiraStoryPoints).containsExactly(DbJiraStoryPoints.builder()
                        .integrationId("1")
                        .issueKey("DEVTEST-1")
                        .storyPoints(1)
                        .startTime(1000L)
                        .endTime(1618281387L)
                        .build(),
                DbJiraStoryPoints.builder()
                        .integrationId("1")
                        .issueKey("DEVTEST-1")
                        .storyPoints(42)
                        .startTime(1618281387L)
                        .endTime(9999999999L)
                        .build());
    }

    @Test
    public void testParseStoryPointsLogsWith2Changes() throws IOException {
        var issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_2_story_points_changes.json", JiraIssue.class);
        var output = JiraIssueParser.parseChangelog("1", issue, "to do", "1234", "Maxime", 42L, "customfield_10016", null, null);

        List<DbJiraStoryPoints> dbJiraStoryPoints = JiraIssueParser.parseStoryPointsLogs(output.getStoryPointsLogs(), "1", "DEVTEST-1", 100, 1000L, 9999999999L);
        DefaultObjectMapper.prettyPrint(dbJiraStoryPoints);
        assertThat(dbJiraStoryPoints).containsExactly(DbJiraStoryPoints.builder()
                        .integrationId("1")
                        .issueKey("DEVTEST-1")
                        .storyPoints(1)
                        .startTime(1000L)
                        .endTime(1618281387L)
                        .build(),
                DbJiraStoryPoints.builder()
                        .integrationId("1")
                        .issueKey("DEVTEST-1")
                        .storyPoints(42)
                        .startTime(1618281387L)
                        .endTime(1618292708L)
                        .build(),
                DbJiraStoryPoints.builder()
                        .integrationId("1")
                        .issueKey("DEVTEST-1")
                        .storyPoints(420)
                        .startTime(1618292708L)
                        .endTime(9999999999L)
                        .build());
    }

    @Test
    public void testParseChangeLogsSprintsCreatedNoChange() throws IOException {
        var issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_created_with_sprint_no_change.json", JiraIssue.class);
        var output = JiraIssueParser.parseChangelog("1", issue, "to do", "1234", "Maxime", 42L, "customfield_10016", "customfield_10020", "Sprint");

        DefaultObjectMapper.prettyPrint(output.getSprintLogs());
        assertThat(output.getSprintLogs()).isEmpty();

        Map<String, List<DbJiraIssue.JiraIssueSprintEvent>> eventsBySprintId = JiraIssueParser.parseSprintsLogs(output.getSprintLogs(), List.of("1", "2"), 42L, 999L);
        DefaultObjectMapper.prettyPrint(eventsBySprintId);

        assertThat(eventsBySprintId).containsOnlyKeys("1", "2");
        assertThat(eventsBySprintId.get("1")).containsExactly(DbJiraIssue.JiraIssueSprintEvent.builder()
                .sprintId("1")
                .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                .startTime(42L)
                .endTime(999L)
                .build());
        assertThat(eventsBySprintId.get("2")).containsExactly(DbJiraIssue.JiraIssueSprintEvent.builder()
                .sprintId("2")
                .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                .startTime(42L)
                .endTime(999L)
                .build());
    }


    @Test
    public void testParseChangeLogsSprintsCreated1Change() throws IOException {
        var issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_created_with_sprint_1_change.json", JiraIssue.class);
        var output = JiraIssueParser.parseChangelog("1", issue, "to do", "1234", "Maxime", 42L, "customfield_10016", "customfield_10020", "Sprint");

        DefaultObjectMapper.prettyPrint(output.getSprintLogs());
        assertThat(output.getSprintLogs()).containsExactly(JiraIssueParser.JiraLogItem.builder()
                .from("67")
                .to("74")
                .time(1618339769L)
                .build());

        Map<String, List<DbJiraIssue.JiraIssueSprintEvent>> eventsBySprintId = JiraIssueParser.parseSprintsLogs(output.getSprintLogs(), List.of("1", "2"), 42L, 999L);
        DefaultObjectMapper.prettyPrint(eventsBySprintId);

        assertThat(eventsBySprintId).containsOnlyKeys("67", "74");
        assertThat(eventsBySprintId.get("67")).containsExactly(
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("67")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(42L)
                        .endTime(1618339769L)
                        .build(),
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("67")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                        .startTime(1618339769L)
                        .endTime(999L)
                        .build());
        assertThat(eventsBySprintId.get("74")).containsExactly(
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("74")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1618339769L)
                        .endTime(999L)
                        .build());

    }

    @Test
    public void testParseChangeLogsSprints() throws IOException {
        var issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_sprint_many_changes_and_removals.json", JiraIssue.class);
        var output = JiraIssueParser.parseChangelog("1", issue, "to do", "1234", "Maxime", 42L, "customfield_10016", "customfield_10020", "Sprint");

        assertThat(output.getSprintLogs()).containsExactly(
                JiraIssueParser.JiraLogItem.builder()
                        .from("67")
                        .to("")
                        .time(1618338018L)
                        .build(),
                JiraIssueParser.JiraLogItem.builder()
                        .from("72")
                        .to("67")
                        .time(1618338015L)
                        .build(),
                JiraIssueParser.JiraLogItem.builder()
                        .from("69")
                        .to("72")
                        .time(1618338012L)
                        .build(),
                JiraIssueParser.JiraLogItem.builder()
                        .from("67")
                        .to("69")
                        .time(1618338007L)
                        .build(),
                JiraIssueParser.JiraLogItem.builder()
                        .from("74")
                        .to("67")
                        .time(1618338001L)
                        .build(),
                JiraIssueParser.JiraLogItem.builder()
                        .from("")
                        .to("74")
                        .time(1618337994L)
                        .build(),
                JiraIssueParser.JiraLogItem.builder()
                        .from("74")
                        .to("")
                        .time(1618298111L)
                        .build(),
                JiraIssueParser.JiraLogItem.builder()
                        .from("67")
                        .to("74")
                        .time(1618298086L)
                        .build(),
                JiraIssueParser.JiraLogItem.builder()
                        .from("")
                        .to("67")
                        .time(1618298032L)
                        .build());

        Map<String, List<DbJiraIssue.JiraIssueSprintEvent>> eventsBySprintId = JiraIssueParser.parseSprintsLogs(output.getSprintLogs(), List.of("1", "2"), 42L, 999L);
        DefaultObjectMapper.prettyPrint(eventsBySprintId);

        assertThat(eventsBySprintId).containsOnlyKeys("67", "69", "72", "74");
        assertThat(eventsBySprintId.get("67")).containsExactly(
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("67")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1618298032L)
                        .endTime(1618298086L)
                        .build(),
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("67")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                        .startTime(1618298086L)
                        .endTime(1618338001L)
                        .build(),
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("67")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1618338001L)
                        .endTime(1618338007L)
                        .build(),
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("67")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                        .startTime(1618338007L)
                        .endTime(1618338015L)
                        .build(),
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("67")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1618338015L)
                        .endTime(1618338018L)
                        .build(),
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("67")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                        .startTime(1618338018L)
                        .endTime(999L)
                        .build());

        assertThat(eventsBySprintId.get("69")).containsExactly(
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("69")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1618338007L)
                        .endTime(1618338012L)
                        .build(),
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("69")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                        .startTime(1618338012L)
                        .endTime(999L)
                        .build());

        assertThat(eventsBySprintId.get("72")).containsExactly(
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("72")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1618338012L)
                        .endTime(1618338015L)
                        .build(),
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("72")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                        .startTime(1618338015L)
                        .endTime(999L)
                        .build());

        assertThat(eventsBySprintId.get("74")).containsExactly(
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("74")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1618298086L)
                        .endTime(1618298111L)
                        .build(),
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("74")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                        .startTime(1618298111L)
                        .endTime(1618337994L)
                        .build(),
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("74")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1618337994L)
                        .endTime(1618338001L)
                        .build(),
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("74")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                        .startTime(1618338001L)
                        .endTime(999L)
                        .build());
    }

    @Test
    public void testParseChangeLogsSprintsMany() throws IOException {
        var issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_many_sprints.json", JiraIssue.class);
        var output = JiraIssueParser.parseChangelog("1", issue, "to do", "1234", "Maxime", 42L, "customfield_10016", "customfield_10020", "Sprint");

        DefaultObjectMapper.prettyPrint(output.getSprintLogs());
        assertThat(output.getSprintLogs()).containsExactly(
                JiraIssueParser.JiraLogItem.builder()
                        .from("62, 66, 42")
                        .to("62, 66, 67")
                        .time(1617293819L)
                        .build(),
                JiraIssueParser.JiraLogItem.builder()
                        .from("62")
                        .to("62, 66, 42")
                        .time(1616688898L)
                        .build(),
                JiraIssueParser.JiraLogItem.builder()
                        .from("")
                        .to("62")
                        .time(1615935524L)
                        .build());

        Map<String, List<DbJiraIssue.JiraIssueSprintEvent>> eventsBySprintId = JiraIssueParser.parseSprintsLogs(output.getSprintLogs(), List.of("67"), 42L, 999L);
        DefaultObjectMapper.prettyPrint(eventsBySprintId);

        assertThat(eventsBySprintId).containsOnlyKeys("62", "66", "67", "42");
        assertThat(eventsBySprintId.get("62")).containsExactly(
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("62")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1615935524L)
                        .endTime(999L)
                        .build());
        assertThat(eventsBySprintId.get("66")).containsExactly(
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("66")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1616688898L)
                        .endTime(999L)
                        .build());
        assertThat(eventsBySprintId.get("67")).containsExactly(
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("67")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1617293819L)
                        .endTime(999L)
                        .build());
        assertThat(eventsBySprintId.get("42")).containsExactly(
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("42")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1616688898L)
                        .endTime(1617293819L)
                        .build(),
                DbJiraIssue.JiraIssueSprintEvent.builder()
                        .sprintId("42")
                        .eventType(DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                        .startTime(1617293819L)
                        .endTime(999L)
                        .build());
    }

    @Test
    public void testParseChangeLogSprintsWithNoId() throws IOException {
        var issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_sprint_change_no_id.json", JiraIssue.class);
        var output = JiraIssueParser.parseChangelog("1", issue, "to do", "1234", "Maxime", 42L, "customfield_10016", "customfield_10020", "Sprint");

        DefaultObjectMapper.prettyPrint(output.getSprintLogs());
        DefaultObjectMapper.prettyPrint(output.getSprintLogs());
        assertThat(output.getSprintLogs()).containsExactly(JiraIssueParser.JiraLogItem.builder()
                .from("67")
                .to("74")
                .time(1618339769L)
                .build());
    }

    @Test
    public void testIssuesWithFieldChild() throws IOException {
        String input = ResourceUtils.getResourceAsString("samples/database/jiraissues_2.json");
        PaginatedResponse<JiraIssue> issues = MAPPER.readValue(input,
                MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        Date currentTime = new Date();
        List<IntegrationConfig.ConfigEntry> entries = List.of(
                IntegrationConfig.ConfigEntry.builder().key("customfield_10112").name("OptionWithChild").build());
        List<DbJiraIssue> dbJiraIssues = new ArrayList<>();
        issues.getResponse().getRecords().forEach(issue -> {
            JiraIssueParser.JiraParserConfig config = JiraIssueParser.JiraParserConfig.builder()
                    .epicLinkField("customfield_10014")
                    .storyPointsField("customfield_10030")
                    .sprintFieldKey("Sprint")
                    .customFieldConfig(entries)
                    .customFieldProperties(List.of(
                            DbJiraField.builder()
                                    .custom(true)
                                    .name("yello")
                                    .integrationId("1")
                                    .fieldType("option-with-child")
                                    .fieldKey("customfield_10112")
                                    .fieldItems("child")
                                    .build())).build();
            DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, config);
            dbJiraIssues.add(tmp);
        });
        assertThat(dbJiraIssues.stream()
                .filter(dbJiraIssue -> dbJiraIssue.getCustomFields()
                        .equals(Map.of("customfield_10112", "\"parent_opt_1\":\"parent_1_child_opt_1\"")))
                .findFirst()
                .get().getKey()).isEqualTo("CGN-9");
        assertThat(dbJiraIssues.stream()
                .filter(dbJiraIssue -> dbJiraIssue.getCustomFields()
                        .equals(Map.of("customfield_10112", "\"parent_opt_3\":\"\"")))
                .findFirst()
                .get().getKey()).isEqualTo("CGN-7");
        assertThat(dbJiraIssues.stream().filter(dbJiraIssue -> dbJiraIssue.getStatus().equalsIgnoreCase("Done") && dbJiraIssue.getIssueResolvedAt() == null).collect(Collectors.toList())).size().isEqualTo(0);
    }

    @Test
    public void testChangeLogComparator() {

        ChangeLogEvent eNull = ChangeLogEvent.builder().created(null).build();
        ChangeLogEvent e1 = ChangeLogEvent.builder().created(DateUtils.fromEpochSecondToDate(1000L)).build();
        ChangeLogEvent e2 = ChangeLogEvent.builder().created(DateUtils.fromEpochSecondToDate(2000L)).build();
        ChangeLogEvent e3 = ChangeLogEvent.builder().created(DateUtils.fromEpochSecondToDate(3000L)).build();

        var cmp = JiraIssueParser.DESC_CHANGE_LOG_EVENT_COMPARATOR;
        assertThat(cmp.compare(e1, e2)).isGreaterThan(0);
        assertThat(cmp.compare(e2, e1)).isLessThan(0);
        assertThat(cmp.compare(e3, e1)).isLessThan(0);
        assertThat(cmp.compare(e3, e2)).isLessThan(0);
        assertThat(cmp.compare(eNull, e1)).isEqualTo(0);
        assertThat(cmp.compare(e1, eNull)).isEqualTo(0);
        assertThat(cmp.compare(e1, e1)).isEqualTo(0);

        var list = new ArrayList<ChangeLogEvent>();
        list.add(e1);
        list.add(e3);
        list.add(e2);
        list.add(eNull);
        list.sort(cmp);
        assertThat(list).containsExactly(e3, e2, e1, eNull);
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testCustomField() throws IOException {
        Date currentTime = new Date();
        List<DbJiraIssue> dbJiraIssues = new ArrayList<>();
        IntegrationConfig.ConfigEntry config1 = IntegrationConfig.ConfigEntry.builder()
                .key("customfield_10605")
                .delimiter("html_list")
                .build();

        List<IntegrationConfig.ConfigEntry> config = List.of(config1);

        for (JiraIssue jiraIssue : customTagJiraIssue) {
            JiraIssueParser.JiraParserConfig parserConfig = JiraIssueParser.JiraParserConfig.builder().customFieldConfig(config).build();
            DbJiraIssue dbJiraIssue = JiraIssueParser.parseJiraIssue(jiraIssue, "1", currentTime, parserConfig);
            dbJiraIssues.add(dbJiraIssue);
        }
        Assert.assertEquals(dbJiraIssues.size(), 1);

        Assert.assertEquals(1, dbJiraIssues.get(0).getCustomFields().size());
        Assert.assertEquals(5, ((List) (dbJiraIssues.get(0).getCustomFields().get("customfield_10605"))).size());
        Assert.assertEquals(List.of("eForm for Credit Card Remittance", "Common Services Printing from Flex Workflow to Drive Printers", "QA, Performance, and Production ready environments of Flex-Drive POC", "foo abc bar", "xyz bar foo"),
                ((List) (dbJiraIssues.get(0).getCustomFields().get("customfield_10605"))));
    }

    @Test
    public void testParseChangeLogIssueKeyChanges() throws IOException {
        var issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_key_changes.json", JiraIssue.class);
        var output = JiraIssueParser.parseChangelog("1", issue, "to do", "1234", "Maxime", 42L, null, null, null);
        assertThat(issue.getKey()).isEqualTo("CSE2-282");
        assertThat(output.getOldIssueKey()).isEqualTo("TBP13-581");
    }

    @Test
    public void testStatusOverlap() throws IOException {
        var issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_status_overlap.json", JiraIssue.class);
        var output = JiraIssueParser.parseChangelog("1", issue, "to do", "1234", "ashish", 42L, null, null, null);
        assertThat(output.getStatusLogs().size()).isEqualTo(1);
        assertThat(output.getStatusLogs().get(0).getFromString()).isEqualTo("BACKLOG");
        assertThat(output.getStatusLogs().get(0).getToString()).isEqualTo("IN DEVELOPMENT");
        assertThat(output.getStatusLogs().get(0).getTime()).isEqualTo(1664976969);
    }

    @Test
    public void testIssueParentType() throws IOException {
        var issue = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_with_parent.json", JiraIssue.class);
        DbJiraIssue output = JiraIssueParser.parseJiraIssue(issue, "1", new Date(0), null);
        assertThat(output.getParentKey()).isEqualTo("SEI-2262");
        assertThat(output.getParentIssueType()).isEqualTo("EPIC");

        var issue2 = ResourceUtils.getResourceAsObject("jira/issues/jira_issue_without_parent.json", JiraIssue.class);
        DbJiraIssue output2 = JiraIssueParser.parseJiraIssue(issue2, "1", new Date(0), null);
        assertThat(output2.getParentKey()).isNull();
        assertThat(output2.getParentIssueType()).isNull();
    }
}