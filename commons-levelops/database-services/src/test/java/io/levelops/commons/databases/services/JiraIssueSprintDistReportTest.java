package io.levelops.commons.databases.services;

import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.response.JiraSprintDistMetric;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueSprintDistReportTest {
    private final String company = "test";

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private JiraIssueService jiraIssueService;
    private JiraIssueSprintMappingDatabaseService sprintMappingDatabaseService;
    private String integrationId;

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
                .startDate(1L)
                .endDate(10L)
                .completedDate(10L)
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder()
                .sprintId(2)
                .integrationId(Integer.valueOf(integrationId))
                .name("LO-2")
                .startDate(11L)
                .endDate(20L)
                .completedDate(20L)
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build());


        DbJiraIssueSprintMapping mapping1 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("a")
                .sprintId("1")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(1)
                .storyPointsDelivered(1)
                .build();
        String id1 = sprintMappingDatabaseService.insert(company, mapping1);

        DbJiraIssueSprintMapping mapping2 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("b")
                .sprintId("1")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(3)
                .storyPointsDelivered(3)
                .build();
        String id2 = sprintMappingDatabaseService.insert(company, mapping2);

        DbJiraIssueSprintMapping mapping3 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("c")
                .sprintId("2")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(true)
                .storyPointsPlanned(3)
                .storyPointsDelivered(3)
                .build();
        String id3 = sprintMappingDatabaseService.insert(company, mapping3);

        DbJiraIssueSprintMapping mapping4 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("d")
                .sprintId("2")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(5)
                .storyPointsDelivered(5)
                .build();
        String id4 = sprintMappingDatabaseService.insert(company, mapping4);

        DbJiraIssueSprintMapping mapping5 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("e")
                .sprintId("2")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(7)
                .storyPointsDelivered(7)
                .build();
        String id5 = sprintMappingDatabaseService.insert(company, mapping5);

        DbJiraIssueSprintMapping mapping6 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("f")
                .sprintId("2")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(11)
                .storyPointsDelivered(11)
                .build();
        String id6 = sprintMappingDatabaseService.insert(company, mapping6);

        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .sprintIds(List.of(1))
                .statuses(List.of(DbJiraStatus.builder()
                        .startTime(3L)
                        .endTime(3L)
                        .integrationId(integrationId)
                        .issueKey("a")
                        .status("DONE")
                        .statusId("2")
                        .build()))
                .storyPoints(null)
                .issueType("task")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("b")
                .sprintIds(List.of(1))
                .status("done")
                .statuses(List.of(DbJiraStatus.builder()
                        .startTime(9L)
                        .endTime(9L)
                        .integrationId(integrationId)
                        .issueKey("b")
                        .status("DONE")
                        .statusId("2")
                        .build()))
                .project("SUPPORT")
                .storyPoints(2)
                .issueType("story")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("c")
                .sprintIds(List.of(2))
                .project("SUPPORT")
                .statuses(List.of(DbJiraStatus.builder()
                        .startTime(12L)
                        .endTime(12L)
                        .integrationId(integrationId)
                        .issueKey("c")
                        .status("DONE")
                        .statusId("2")
                        .build()))
                .storyPoints(3)
                .issueType("subtask")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("d")
                .sprintIds(List.of(2))
                .statuses(List.of(DbJiraStatus.builder()
                        .startTime(13L)
                        .endTime(13L)
                        .integrationId(integrationId)
                        .issueKey("d")
                        .status("DONE")
                        .statusId("2")
                        .build()))
                .storyPoints(7)
                .issueType("epic")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("e")
                .sprintIds(List.of(2))
                .statuses(List.of(DbJiraStatus.builder()
                        .startTime(16L)
                        .endTime(16L)
                        .integrationId(integrationId)
                        .issueKey("e")
                        .status("DONE")
                        .statusId("2")
                        .build()))
                .storyPoints(7)
                .issueType("epic")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("f")
                .sprintIds(List.of(2))
                .statuses(List.of(DbJiraStatus.builder()
                        .startTime(19L)
                        .endTime(19L)
                        .integrationId(integrationId)
                        .issueKey("f")
                        .status("DONE")
                        .statusId("2")
                        .build()))
                .storyPoints(7)
                .issueType("epic")
                .build());

        DbListResponse<JiraSprintDistMetric> results = jiraIssueService.getSprintDistributionReport(
                company, JiraSprintFilter.CALCULATION.sprint_ticket_count_report, JiraSprintFilter.builder().integrationIds(List.of("1"))
                        .sprintIds(List.of("1", "2")).distributionStages(List.of("DONE")).build());

        List<JiraSprintDistMetric> sprint1Metrics = results.getRecords().stream()
                .filter(r -> r.getSprint().equalsIgnoreCase("LO-1")).collect(Collectors.toList());
        List<JiraSprintDistMetric> sprint2Metrics = results.getRecords().stream()
                .filter(r -> r.getSprint().equalsIgnoreCase("LO-2")).collect(Collectors.toList());

        compareAggResult(List.of(
                JiraSprintDistMetric.builder().key("25").sprint("LO-1").deliveredStoryPoints(4)
                        .deliveredKeys(Set.of("a")).totalKeys(2).totalTimeTaken(2).planned(1).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("50").sprint("LO-1").deliveredStoryPoints(4)
                        .deliveredKeys(Set.of("a")).totalKeys(2).totalTimeTaken(2).planned(1).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("75").sprint("LO-1").deliveredStoryPoints(4)
                        .deliveredKeys(Set.of("a", "b")).totalKeys(2).totalTimeTaken(8).planned(2).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("100").sprint("LO-1").deliveredStoryPoints(4)
                        .deliveredKeys(Set.of("a", "b")).totalKeys(2).totalTimeTaken(8).planned(2).unplanned(0).build()
        ), sprint1Metrics);

        compareAggResult(List.of(
                JiraSprintDistMetric.builder().key("25").sprint("LO-2").deliveredStoryPoints(26)
                        .deliveredKeys(Set.of("c")).totalKeys(4).totalTimeTaken(1).planned(1).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("50").sprint("LO-2").deliveredStoryPoints(26)
                        .deliveredKeys(Set.of("c", "d")).totalKeys(4).totalTimeTaken(2).planned(2).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("75").sprint("LO-2").deliveredStoryPoints(26)
                        .deliveredKeys(Set.of("c", "d", "e")).totalKeys(4).totalTimeTaken(5).planned(3).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("100").sprint("LO-2").deliveredStoryPoints(26)
                        .deliveredKeys(Set.of("c", "d", "e", "f")).totalKeys(4).totalTimeTaken(8).planned(4).unplanned(0).build()
        ), sprint2Metrics);

        results = jiraIssueService.getSprintDistributionReport(
                company, JiraSprintFilter.CALCULATION.sprint_ticket_count_report, JiraSprintFilter.builder().integrationIds(List.of("1"))
                        .sprintIds(List.of("1")).distributionStages(List.of("DONE")).build());
        assertThat(results.getRecords().size()).isEqualTo(4);

        sprint1Metrics = results.getRecords().stream()
                .filter(r -> r.getSprint().equalsIgnoreCase("LO-1")).collect(Collectors.toList());

        compareAggResult(List.of(
                JiraSprintDistMetric.builder().key("25").sprint("LO-1").deliveredStoryPoints(4)
                        .deliveredKeys(Set.of("a")).totalKeys(2).totalTimeTaken(2).planned(1).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("50").sprint("LO-1").deliveredStoryPoints(4)
                        .deliveredKeys(Set.of("a")).totalKeys(2).totalTimeTaken(2).planned(1).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("75").sprint("LO-1").deliveredStoryPoints(4)
                        .deliveredKeys(Set.of("a", "b")).totalKeys(2).totalTimeTaken(8).planned(2).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("100").sprint("LO-1").deliveredStoryPoints(4)
                        .deliveredKeys(Set.of("a", "b")).totalKeys(2).totalTimeTaken(8).planned(2).unplanned(0).build()
        ), sprint1Metrics);


        results = jiraIssueService.getSprintDistributionReport(
                company, JiraSprintFilter.CALCULATION.sprint_ticket_count_report, JiraSprintFilter.builder().integrationIds(List.of("1"))
                        .sprintIds(List.of("1", "2")).completionPercentiles(List.of(60)).distributionStages(List.of("DONE")).build());

        sprint1Metrics = results.getRecords().stream()
                .filter(r -> r.getSprint().equalsIgnoreCase("LO-1")).collect(Collectors.toList());
        sprint2Metrics = results.getRecords().stream()
                .filter(r -> r.getSprint().equalsIgnoreCase("LO-2")).collect(Collectors.toList());

        compareAggResult(List.of(
                JiraSprintDistMetric.builder().key("60").sprint("LO-1").deliveredStoryPoints(4)
                        .deliveredKeys(Set.of("a", "b")).totalKeys(2).totalTimeTaken(8).planned(2).unplanned(0).build()
        ), sprint1Metrics);

        compareAggResult(List.of(
                JiraSprintDistMetric.builder().key("60").sprint("LO-2").deliveredStoryPoints(26)
                        .deliveredKeys(Set.of("c", "d", "e")).totalKeys(4).totalTimeTaken(5).planned(3).unplanned(0).build()
        ), sprint2Metrics);

        results = jiraIssueService.getSprintDistributionReport(
                company, JiraSprintFilter.CALCULATION.sprint_story_points_report, JiraSprintFilter.builder().integrationIds(List.of("1"))
                        .sprintIds(List.of("1", "2")).distributionStages(List.of("DONE")).build());

        sprint1Metrics = results.getRecords().stream()
                .filter(r -> r.getSprint().equalsIgnoreCase("LO-1")).collect(Collectors.toList());
        sprint2Metrics = results.getRecords().stream()
                .filter(r -> r.getSprint().equalsIgnoreCase("LO-2")).collect(Collectors.toList());

        compareAggResult(List.of(
                JiraSprintDistMetric.builder().key("25").sprint("LO-1").deliveredStoryPoints(4)
                        .deliveredKeys(Set.of("a")).totalKeys(2).totalTimeTaken(2).planned(1).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("50").sprint("LO-1").deliveredStoryPoints(4)
                        .deliveredKeys(Set.of("a", "b")).totalKeys(2).totalTimeTaken(8).planned(4).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("75").sprint("LO-1").deliveredStoryPoints(4)
                        .deliveredKeys(Set.of("a", "b")).totalKeys(2).totalTimeTaken(8).planned(4).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("100").sprint("LO-1").deliveredStoryPoints(4)
                        .deliveredKeys(Set.of("a", "b")).totalKeys(2).totalTimeTaken(8).planned(4).unplanned(0).build()
        ), sprint1Metrics);

        compareAggResult(List.of(
                JiraSprintDistMetric.builder().key("25").sprint("LO-2").deliveredStoryPoints(26)
                        .deliveredKeys(Set.of("c", "d")).totalKeys(4).totalTimeTaken(2).planned(8).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("50").sprint("LO-2").deliveredStoryPoints(26)
                        .deliveredKeys(Set.of("c", "d", "e")).totalKeys(4).totalTimeTaken(5).planned(15).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("75").sprint("LO-2").deliveredStoryPoints(26)
                        .deliveredKeys(Set.of("c", "d", "e", "f")).totalKeys(4).totalTimeTaken(8).planned(26).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("100").sprint("LO-2").deliveredStoryPoints(26)
                        .deliveredKeys(Set.of("c", "d", "e", "f")).totalKeys(4).totalTimeTaken(8).planned(26).unplanned(0).build()
        ), sprint2Metrics);

        results = jiraIssueService.getSprintDistributionReport(
                company, JiraSprintFilter.CALCULATION.sprint_story_points_report, JiraSprintFilter.builder().integrationIds(List.of("1"))
                        .sprintIds(List.of("1", "2")).sprintCount(1).distributionStages(List.of("DONE")).build());
        assertThat(results.getRecords().size()).isEqualTo(4);

        sprint2Metrics = results.getRecords().stream()
                .filter(r -> r.getSprint().equalsIgnoreCase("LO-2")).collect(Collectors.toList());

        compareAggResult(List.of(
                JiraSprintDistMetric.builder().key("25").sprint("LO-2").deliveredStoryPoints(26)
                        .deliveredKeys(Set.of("c", "d")).totalKeys(4).totalTimeTaken(2).planned(8).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("50").sprint("LO-2").deliveredStoryPoints(26)
                        .deliveredKeys(Set.of("c", "d", "e")).totalKeys(4).totalTimeTaken(5).planned(15).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("75").sprint("LO-2").deliveredStoryPoints(26)
                        .deliveredKeys(Set.of("c", "d", "e", "f")).totalKeys(4).totalTimeTaken(8).planned(26).unplanned(0).build(),
                JiraSprintDistMetric.builder().key("100").sprint("LO-2").deliveredStoryPoints(26)
                        .deliveredKeys(Set.of("c", "d", "e", "f")).totalKeys(4).totalTimeTaken(8).planned(26).unplanned(0).build()
        ), sprint2Metrics);

        results = jiraIssueService.getSprintDistributionReport(
                company, JiraSprintFilter.CALCULATION.sprint_story_points_report, JiraSprintFilter.builder().integrationIds(List.of("1"))
                        .sprintIds(List.of("1", "2")).completionPercentiles(List.of(60)).distributionStages(List.of("DONE")).build());

        sprint1Metrics = results.getRecords().stream()
                .filter(r -> r.getSprint().equalsIgnoreCase("LO-1")).collect(Collectors.toList());
        sprint2Metrics = results.getRecords().stream()
                .filter(r -> r.getSprint().equalsIgnoreCase("LO-2")).collect(Collectors.toList());

        compareAggResult(List.of(
                JiraSprintDistMetric.builder().key("60").sprint("LO-1").deliveredStoryPoints(4)
                        .deliveredKeys(Set.of("a", "b")).totalKeys(2).totalTimeTaken(8).planned(4).unplanned(0).build()
        ), sprint1Metrics);

        compareAggResult(List.of(
                JiraSprintDistMetric.builder().key("60").sprint("LO-2").deliveredStoryPoints(26)
                        .deliveredKeys(Set.of("c", "d", "e", "f")).totalKeys(4).totalTimeTaken(8).planned(26).unplanned(0).build()
        ), sprint2Metrics);
    }

    public void compareAggResult(List<JiraSprintDistMetric> expected, List<JiraSprintDistMetric> actual) {
        assertThat(expected.size()).isEqualTo(actual.size());

        for(int i=0; i<expected.size(); i++) {
            JiraSprintDistMetric expectedResult = expected.get(i);
            JiraSprintDistMetric actualResult = actual.get(i);

            assertThat(expectedResult.getKey()).isEqualTo(actualResult.getKey());
            assertThat(expectedResult.getDeliveredStoryPoints()).isEqualTo(actualResult.getDeliveredStoryPoints());
            assertThat(expectedResult.getDeliveredKeys()).isEqualTo(actualResult.getDeliveredKeys());
            assertThat(expectedResult.getTotalKeys()).isEqualTo(actualResult.getTotalKeys());
            assertThat(expectedResult.getTotalTimeTaken()).isEqualTo(actualResult.getTotalTimeTaken());
            assertThat(expectedResult.getPlanned()).isEqualTo(actualResult.getPlanned());
            assertThat(expectedResult.getUnplanned()).isEqualTo(actualResult.getUnplanned());
        }
    }
}