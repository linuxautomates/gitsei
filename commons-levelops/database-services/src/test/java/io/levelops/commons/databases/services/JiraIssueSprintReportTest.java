package io.levelops.commons.databases.services;

import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.JiraIssueSprintMappingAggResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueSprintReportTest {
    private final String company = "test";

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private JiraIssueService jiraIssueService;
    private JiraIssueSprintMappingDatabaseService sprintMappingDatabaseService;
    private String integrationId;
    private String integrationId2;

    @Before
    public void setup() throws SQLException, IOException {
        DataSource dataSource =  DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();
        sprintMappingDatabaseService = new JiraIssueSprintMappingDatabaseService(dataSource);
        IntegrationService integrationService = jiraTestDbs.getIntegrationService();
        JiraFieldService jiraFieldService = jiraTestDbs.getJiraFieldService();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        new TagsService(dataSource).ensureTableExistence(company);
        new TagItemDBService(dataSource).ensureTableExistence(company);
        IntegrationTrackingService integrationTrackingService = new IntegrationTrackingService(dataSource);
        sprintMappingDatabaseService.ensureTableExistence(company);
        integrationId = integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        integrationId2 = integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test 2")
                .status("enabled")
                .build());
        jiraIssueService.ensureTableExistence(company);
        jiraFieldService.ensureTableExistence(company);
        integrationTrackingService.ensureTableExistence(company);
    }

    private DbJiraIssue buildBaseIssue() {
        return DbJiraIssue.builder()
                .key("LEV-123")
                .integrationId(integrationId)
                .project("def")
                .summary("sum")
                .descSize(3)
                .priority("high")
                .reporter("max")
                .status("won't do")
                .issueType("bug")
                .hops(2)
                .bounces(4)
                .numAttachments(0)
                .issueCreatedAt(1L)
                .issueUpdatedAt(2L)
                .ingestedAt(0L)
                .reporter("gandalf")
                .assignee("frodo")
                .build();
    }

    @Test
    public void testSprintMappings() throws SQLException {
        Date currentTime = new Date();
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder()
                .sprintId(1)
                .integrationId(Integer.valueOf(integrationId))
                .name("LO-1")
                .endDate(12L)
                .completedDate(12L)
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder()
                .sprintId(2)
                .integrationId(Integer.valueOf(integrationId))
                .name("LO-2")
                .endDate(42L)
                .completedDate(42L)
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder()
                .sprintId(1)
                .integrationId(Integer.valueOf(integrationId2))
                .name("LFE-1")
                .endDate(108L)
                .completedDate(108L)
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build());

        // integration=1, sprint=1: a, b
        // integration=1, sprint=2: b, c(ignorable), d
        // integration=2, sprint=1: a
        DbJiraIssueSprintMapping mapping1 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("a")
                .sprintId("1")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(2)
                .storyPointsDelivered(3)
                .removedMidSprint(false)
                .build();
        String id1 = sprintMappingDatabaseService.insert("test", mapping1);
        mapping1 = mapping1.toBuilder().id(id1).build();

        DbJiraIssueSprintMapping mapping2 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("b")
                .sprintId("1")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(2)
                .storyPointsDelivered(3)
                .removedMidSprint(false)
                .build();
        String id2 = sprintMappingDatabaseService.insert("test", mapping2);
        mapping2 = mapping2.toBuilder().id(id2).build();

        DbJiraIssueSprintMapping mapping3 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("c")
                .sprintId("2")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(true)
                .storyPointsPlanned(2)
                .storyPointsDelivered(3)
                .removedMidSprint(false)
                .build();
        String id3 = sprintMappingDatabaseService.insert("test", mapping3);
        mapping3 = mapping3.toBuilder().id(id3).build();

        DbJiraIssueSprintMapping mapping4 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("d")
                .sprintId("2")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(2)
                .storyPointsDelivered(3)
                .removedMidSprint(false)
                .build();
        String id4 = sprintMappingDatabaseService.insert("test", mapping4);
        mapping4 = mapping4.toBuilder().id(id4).build();

        DbJiraIssueSprintMapping mapping5 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId2)
                .issueKey("a")
                .sprintId("1")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(2)
                .storyPointsDelivered(3)
                .removedMidSprint(false)
                .build();
        String id5 = sprintMappingDatabaseService.insert("test", mapping5);
        mapping5 = mapping5.toBuilder().id(id5).build();

        DbJiraIssueSprintMapping mapping6 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("b")
                .sprintId("2")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(2)
                .storyPointsDelivered(3)
                .removedMidSprint(false)
                .build();
        String id6 = sprintMappingDatabaseService.insert("test", mapping6);
        mapping6 = mapping6.toBuilder().id(id6).build();

        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .sprintIds(List.of(1))
                .status("todo")
                .storyPoints(null)
                .issueType("task")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .integrationId(integrationId2)
                .sprintIds(List.of(1))
                .key("a")
                .status("todo")
                .storyPoints(null)
                .issueType("bug")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("b")
                .sprintIds(List.of(1))
                .status("done")
                .project("SUPPORT")
                .storyPoints(2)
                .issueType("story")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("c")
                .sprintIds(List.of(2))
                .project("SUPPORT")
                .status("todo")
                .storyPoints(3)
                .issueType("subtask")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("d")
                .sprintIds(List.of(2))
                .status("done")
                .storyPoints(7)
                .issueType("epic")
                .build());

        JiraIssueSprintMappingAggResult aggMapping1 = JiraIssueSprintMappingAggResult.builder()
                .sprintMapping(mapping1)
                .issueType("TASK")
                .build();
        JiraIssueSprintMappingAggResult aggMapping2 = JiraIssueSprintMappingAggResult.builder()
                .sprintMapping(mapping2)
                .issueType("STORY")
                .build();
        JiraIssueSprintMappingAggResult aggMapping3 = JiraIssueSprintMappingAggResult.builder()
                .sprintMapping(mapping3)
                .issueType("SUBTASK")
                .build();
        JiraIssueSprintMappingAggResult aggMapping4 = JiraIssueSprintMappingAggResult.builder()
                .sprintMapping(mapping4)
                .issueType("EPIC")
                .build();
        JiraIssueSprintMappingAggResult aggMapping5 = JiraIssueSprintMappingAggResult.builder()
                .sprintMapping(mapping5) // 2 a
                .issueType("BUG")
                .build();
        JiraIssueSprintMappingAggResult aggMapping6 = JiraIssueSprintMappingAggResult.builder()
                .sprintMapping(mapping6)
                .issueType("STORY")
                .build();
        // -- agg

        DbListResponse<DbAggregationResult> result;

        // no filters
        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.sprint_mapping)
                        .calculation(JiraIssuesFilter.CALCULATION.sprint_mapping)
                        .build(),
                false, null, null, Map.of());
        DefaultObjectMapper.prettyPrint(result);
        verifyCounts(result, 3, 3);
        assertThat(result.getRecords().get(2).getSprintName()).isEqualTo("LO-1");
        assertThat(result.getRecords().get(2).getSprintCompletedAt()).isEqualTo(12L);
        assertThat(result.getRecords().get(1).getSprintName()).isEqualTo("LO-2");
        assertThat(result.getRecords().get(1).getSprintCompletedAt()).isEqualTo(42L);
        assertThat(result.getRecords().get(0).getSprintName()).isEqualTo("LFE-1");
        assertThat(result.getRecords().get(0).getSprintCompletedAt()).isEqualTo(108L);
        verifySprintMappings(result, 2, "1", "1", aggMapping1, aggMapping2);
        verifySprintMappings(result, 1, "1", "2", aggMapping3, aggMapping4, aggMapping6);
        verifySprintMappings(result, 0, "2", "1", aggMapping5);

        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.sprint_mapping)
                        .calculation(JiraIssuesFilter.CALCULATION.sprint_mapping)
                        .sprintCount(3)
                        .build(),
                false, null, null, Map.of());

        verifyCounts(result, 3, 3);
        assertThat(result.getRecords().get(2).getSprintName()).isEqualTo("LO-1");
        assertThat(result.getRecords().get(1).getSprintName()).isEqualTo("LO-2");
        assertThat(result.getRecords().get(0).getSprintName()).isEqualTo("LFE-1");

        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.sprint_mapping)
                        .calculation(JiraIssuesFilter.CALCULATION.sprint_mapping)
                        .pageSize(1)
                        .build(),
                false, null, null, Map.of());

        DefaultObjectMapper.prettyPrint(result);
        verifyCounts(result, 1, 1);
        assertThat(result.getRecords().get(0).getSprintName()).isEqualTo("LFE-1");
    }

    private void verifyCounts(DbListResponse<DbAggregationResult> result, int total, int count) {
        assertThat(result.getTotalCount()).isEqualTo(total);
        assertThat(result.getCount()).isEqualTo(count);
        assertThat(result.getRecords()).hasSize(count);
    }

    private void verifySprintMappings(DbListResponse<DbAggregationResult> result, int index, String integration, String sprintId, JiraIssueSprintMappingAggResult... mappings) {
        assertThat(result.getRecords().get(index).getIntegrationId()).isEqualTo(integration);
        assertThat(result.getRecords().get(index).getSprintId()).isEqualTo(sprintId);
        List<JiraIssueSprintMappingAggResult> sanitized = result.getRecords().get(index).getSprintMappingAggs().stream()
                .map(o -> o.toBuilder()
                        .sprintMapping(o.getSprintMapping().toBuilder()
                                .createdAt(null)
                                .build())
                        .build())
                .collect(Collectors.toList());
        assertThat(sanitized).containsExactlyInAnyOrder(mappings);
    }
}