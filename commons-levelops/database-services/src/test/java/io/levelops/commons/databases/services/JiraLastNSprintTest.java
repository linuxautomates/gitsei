package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
import io.levelops.commons.databases.models.database.jira.DbJiraProject;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.response.AcrossUniqueKey;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraField;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraUser;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraLastNSprintTest {
    private static final String company = "test";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static JiraIssueService jiraIssueService;
    private static Long ingestedAt;
    private static DbJiraIssue randomIssue;
    private static JiraIssueSprintMappingDatabaseService jiraIssueSprintMappingDatabaseService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        final ObjectMapper m = DefaultObjectMapper.get();

        DataSource dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        JiraFieldService jiraFieldService = jiraTestDbs.getJiraFieldService();
        JiraProjectService jiraProjectService = jiraTestDbs.getJiraProjectService();
        IntegrationService integrationService = jiraTestDbs.getIntegrationService();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        new TagsService(dataSource).ensureTableExistence(company);
        new TagItemDBService(dataSource).ensureTableExistence(company);
        integrationService.ensureTableExistence(company);
        var integrationId1 = integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        integrationService.insertConfig(company, IntegrationConfig.builder()
                .integrationId(integrationId1)
                .config(Map.of("agg_custom_fields",
                        List.of(IntegrationConfig.ConfigEntry.builder()
                                .key("customfield_20001")
                                .name("hello")
                                .delimiter(",")
                                .build())))
                .build());
        var integrationId2 = integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test 2")
                .status("enabled")
                .build());
        jiraIssueService.ensureTableExistence(company);
        jiraFieldService.ensureTableExistence(company);
        jiraProjectService.ensureTableExistence(company);
        jiraIssueSprintMappingDatabaseService = new JiraIssueSprintMappingDatabaseService(dataSource);
        jiraIssueSprintMappingDatabaseService.ensureTableExistence(company);
        String input = ResourceUtils.getResourceAsString("json/databases/jiraissues_jun62020.json");
        PaginatedResponse<JiraIssue> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        Date currentTime = new Date();
        jiraFieldService.batchUpsert(company,
                List.of(DbJiraField.builder().custom(true).name("yello").integrationId(integrationId1).fieldType("array").fieldKey("customfield_10048").fieldItems("user").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId(integrationId1).fieldType("string").fieldKey("customfield_12641").build(),
                        DbJiraField.builder().custom(true).name("hello").integrationId(integrationId1).fieldType("string").fieldKey("customfield_20001").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId(integrationId1).fieldType("string").fieldKey("customfield_12746").build()));
        List<IntegrationConfig.ConfigEntry> entries = List.of(
                IntegrationConfig.ConfigEntry.builder().key("customfield_12641").name("something").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_12746").name("something 1").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_10048").name("USER ARRAY").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_10149").name("USER SINGLE").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_20001").name("hello").delimiter(",").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_12716").name("something 2").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_10020").name("sprint").build());
        String inputProjects = ResourceUtils.getResourceAsString("json/databases/jira_projects_april.json");
        PaginatedResponse<JiraProject> projects = m.readValue(inputProjects,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraProject.class));
        List<DbJiraProject> dbJiraProjects = projects.getResponse().getRecords().stream()
                .map(project -> DbJiraProject.fromJiraProject(project, integrationId1))
                .collect(Collectors.toList());
        if (dbJiraProjects.size() > 0) {
            jiraProjectService.batchUpsert(company, dbJiraProjects);
        }
        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, integrationId1, currentTime,
                        JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build());
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3)).build();
                if (randomIssue == null) {
                    randomIssue = tmp;
                } else {
                    randomIssue = (new Random().nextInt(100)) > 50 ? tmp : randomIssue;
                }
                if ("LEV-273".equalsIgnoreCase(tmp.getKey())) {
                    jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, integrationId2, currentTime,
                            JiraIssueParser.JiraParserConfig.builder()
                                    .epicLinkField("customfield_10014")
                                    .storyPointsField("customfield_10030")
                                    .customFieldConfig(entries)
                                    .build()));
                }
                jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, integrationId1,
                        DateUtils.addSeconds(currentTime, 2 * -86400),
                        JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build()));
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(),
                        tmp.getIngestedAt() - 86400L).isPresent()) {
                    throw new RuntimeException("This issue shouldnt exist.");
                }
                jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, integrationId1,
                        DateUtils.addSeconds(currentTime, -86400),
                        JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build()));
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isPresent()) {
                    throw new RuntimeException("This issue shouldnt exist.");
                }
                jiraIssueService.insert(company, tmp);
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isEmpty()) {
                    throw new RuntimeException("This issue should exist.");
                }
                List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
                versions.forEach(dbJiraVersion -> jiraIssueService.insertJiraVersion(company, dbJiraVersion));
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });
        input = ResourceUtils.getResourceAsString("json/databases/jirausers_aug12.json");
        PaginatedResponse<JiraUser> jiraUsers = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraUser.class));
        jiraUsers.getResponse().getRecords().forEach(issue -> {
            DbJiraUser tmp = DbJiraUser.fromJiraUser(issue, integrationId1);
            jiraIssueService.insertJiraUser(company, tmp);
        });
        ingestedAt = io.levelops.commons.dates.DateUtils.truncate(currentTime, Calendar.DATE);
        jiraFieldService.batchUpsert(company, List.of(Objects.requireNonNull(DbJiraField.fromJiraField(
                JiraField.builder()
                        .key("customfield_12345")
                        .custom(true)
                        .name("test_field")
                        .schema(JiraField.Schema.builder()
                                .type("array")
                                .items("string")
                                .build())
                        .build(), integrationId1))));
        jiraIssueService.insert(company, randomIssue.toBuilder()
                .customFields(Map.of("customfield_12345", List.of("1.0", "2.0", "3.0")))
                .build());
        jiraIssueService.bulkUpdateEpicStoryPoints(company, integrationId1,
                DateUtils.truncate(currentTime, Calendar.DATE).toInstant().getEpochSecond());
        jiraIssueService.bulkUpdateEpicStoryPoints(company, integrationId2,
                DateUtils.truncate(currentTime, Calendar.DATE).toInstant().getEpochSecond());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(0).name("My sprint1").integrationId(2).state("closed").startDate(1617290095L).endDate(1617290195L).completedDate(1617290295L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(1).name("sprint1").integrationId(1).state("closed").startDate(1617290915L).endDate(1617290295L).completedDate(1617290295L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(2).name("sprint2").integrationId(1).state("active").startDate(1617290295L).endDate(1617290395L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(3).name("sprint3").integrationId(1).state("future").startDate(1617290395L).endDate(1617290495L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(4).name("sprint4").integrationId(1).state("future").startDate(1617290495L).endDate(1617290595L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(5).name("sprint5").integrationId(1).state("future").startDate(1617290595L).endDate(1617290695L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(6).name("sprint6").integrationId(2).state("future").startDate(1617290695L).endDate(1617290795L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(7).name("My sprint2").integrationId(2).state("closed").startDate(1617290795L).endDate(1617290895L).completedDate(1617290295L).updatedAt(currentTime.toInstant().getEpochSecond()).build());

        List.of(
                DbJiraIssue.builder().key("UN-1").integrationId(integrationId1).project("TS").summary("Remove access for user").priority("MEDIUM").reporter("").status("To Do").issueType("Task")
                        .descSize(0).bounces(1).hops(1).numAttachments(1).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L).ingestedAt(ingestedAt).sprintIds(List.of(2, 3)).build(),
                DbJiraIssue.builder().key("UN-2").integrationId(integrationId1).project("LEV").summary("Summary").priority("MEDIUM").reporter("").status("DONE").issueType("Epic")
                        .descSize(0).bounces(0).hops(0).numAttachments(0).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L).ingestedAt(ingestedAt).sprintIds(List.of(2, 3)).build(),
                DbJiraIssue.builder().key("UN-3").integrationId(integrationId1).project("TS").summary("Remove access for user").priority("MEDIUM").reporter("").status("To Do").issueType("Task")
                        .descSize(0).bounces(1).hops(1).numAttachments(1).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L).ingestedAt(ingestedAt).sprintIds(List.of(1, 3)).build(),
                DbJiraIssue.builder().key("UN-4").integrationId(integrationId1).project("LEV").summary("Summary").priority("MEDIUM").reporter("").status("DONE").issueType("Epic")
                        .descSize(0).bounces(0).hops(0).numAttachments(0).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L).ingestedAt(ingestedAt).sprintIds(List.of(1, 3)).build(),
                DbJiraIssue.builder().key("UN-5").integrationId(integrationId1).project("TS").summary("Remove access for user").priority("MEDIUM").reporter("").status("To Do").issueType("Task")
                        .descSize(0).bounces(1).hops(1).numAttachments(1).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L).ingestedAt(ingestedAt).sprintIds(List.of(3)).build(),
                DbJiraIssue.builder().key("UN-6").integrationId(integrationId1).project("LEV").summary("Summary").priority("MEDIUM").reporter("").status("DONE").issueType("Epic")
                        .descSize(0).bounces(0).hops(0).numAttachments(0).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L).ingestedAt(ingestedAt).sprintIds(List.of(2)).build(),
                DbJiraIssue.builder().key("UN-7").integrationId(integrationId1).project("TS").summary("Remove access for user").priority("MEDIUM").reporter("").status("To Do").issueType("Task")
                        .descSize(0).bounces(1).hops(1).numAttachments(1).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L).ingestedAt(ingestedAt).sprintIds(List.of(1, 2, 3)).build(),
                DbJiraIssue.builder().key("UN-8").integrationId(integrationId1).project("LEV").summary("Summary").priority("MEDIUM").reporter("").status("DONE").issueType("Epic")
                        .descSize(0).bounces(0).hops(0).numAttachments(0).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L).ingestedAt(ingestedAt).sprintIds(List.of(1, 2, 3)).build(),
                DbJiraIssue.builder().key("UN-9").integrationId(integrationId1).project("LEV").summary("Summary").priority("MEDIUM").reporter("").status("DONE").issueType("Epic")
                        .descSize(0).bounces(0).hops(0).numAttachments(0).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L).ingestedAt(ingestedAt).sprintIds(List.of(4, 5)).build(),
                DbJiraIssue.builder().key("UN-10").integrationId(integrationId2).project("LEV").summary("Summary").priority("MEDIUM").reporter("").status("DONE").issueType("Epic")
                        .descSize(0).bounces(0).hops(0).numAttachments(0).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L).ingestedAt(ingestedAt).sprintIds(List.of(4, 5)).build(),
                DbJiraIssue.builder().key("UN-11").integrationId(integrationId2).project("LEV").summary("Summary").priority("MEDIUM").reporter("").status("DONE").issueType("Epic")
                        .descSize(0).bounces(0).hops(0).numAttachments(0).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L).ingestedAt(ingestedAt).sprintIds(List.of(6)).build(),
                DbJiraIssue.builder().key("UN-12").integrationId(integrationId2).project("LEV").summary("Summary").priority("MEDIUM").reporter("").status("DONE").issueType("Epic")
                        .descSize(0).bounces(0).hops(0).numAttachments(0).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L).ingestedAt(ingestedAt).sprintIds(List.of(0)).build(),
                DbJiraIssue.builder().key("UN-13").integrationId(integrationId2).project("LEV").summary("Summary").priority("MEDIUM").reporter("").status("DONE").issueType("Epic")
                        .descSize(0).bounces(0).hops(0).numAttachments(0).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L).ingestedAt(ingestedAt).sprintIds(List.of(7)).build()
        ).forEach(issue -> {
            try {
                jiraIssueService.insert(company, issue);
            } catch (SQLException ignored) {
            }
        });
        List.of(DbJiraIssueSprintMapping.builder()
                                .integrationId("2")
                                .issueKey("UN-12")
                                .sprintId("7")
                                .planned(true)
                                .outsideOfSprint(false)
                                .storyPointsDelivered(3)
                                .storyPointsPlanned(3)
                                .ignorableIssueType(false)
                                .delivered(true)
                                .addedAt(ingestedAt)
                                .createdAt(new Date(ingestedAt).toInstant())
                                .build(),
                        DbJiraIssueSprintMapping.builder()
                                .integrationId("2")
                                .issueKey("UN-12")
                                .sprintId("0")
                                .planned(true)
                                .outsideOfSprint(false)
                                .storyPointsDelivered(3)
                                .storyPointsPlanned(3)
                                .ignorableIssueType(false)
                                .delivered(true)
                                .addedAt(ingestedAt)
                                .createdAt(new Date(ingestedAt).toInstant())
                                .build())
                .forEach(mapping -> {
                    try {
                        jiraIssueSprintMappingDatabaseService.insert(company, mapping);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * Setup
     * <p>
     * {1,2,3}    |   502
     * {}         |  1001
     * {2}        |     1
     * {3}        |     1
     * {1,3}      |     2
     * {2,3}      |     2
     */

    @Test
    public void testGroupByAndCalculate() throws SQLException {
        List<DbAggregationResult> records = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintCount(3)
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), false, null, null, Map.of())
                .getRecords();

        Assert.assertNotNull(records);
        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder().key("sprint5").totalTickets(1L).build(),
                DbAggregationResult.builder().key("sprint4").totalTickets(1L).build(),
                DbAggregationResult.builder().key("sprint3").totalTickets(27L).build()
        );
        verifyRecords(records, expected);

        records = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintCount(1)
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), false, null, null, Map.of())
                .getRecords();

        Assert.assertNotNull(records);
        expected = List.of(
                DbAggregationResult.builder().key("sprint5").totalTickets(1L).build()
        );
        verifyRecords(records, expected);

        records = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("2"))
                                .sprintCount(2)
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), false, null, null, Map.of())
                .getRecords();

        Assert.assertNotNull(records);
        expected = List.of(
                DbAggregationResult.builder().key("sprint6").totalTickets(1L).build(),
                DbAggregationResult.builder().key("My sprint2").totalTickets(1L).build());
        verifyRecords(records, expected);

        records = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("2"))
                                .sprintCount(1)
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();

        Assert.assertNotNull(records);
        expected = List.of(
                DbAggregationResult.builder().key("My sprint2").totalTickets(1L).build());
        verifyRecords(records, expected);

        records = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintCount(1)
                                .sprintIds(List.of("1", "2"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();

        Assert.assertNotNull(records);
        expected = List.of(
                DbAggregationResult.builder().key("sprint2").totalTickets(1L).build()
        );
        verifyRecords(records, expected);
    }

    /**
     * Setup
     * <p>
     * {1,2,3}    |   502
     * {}         |  1001
     * {2}        |     1
     * {3}        |     1
     * {1,3}      |     2
     * {2,3}      |     2
     */

    @Test
    public void testList() throws SQLException {
        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintCount(5)
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(30);

        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintCount(3)
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(3);

        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintCount(1)
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);

        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintCount(1)
                                .sprintNames(List.of("sprint1", "sprint2"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(25);

        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintCount(1)
                                .sprintNames(List.of("sprint2"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);
    }

    @Test
    public void groupByAndCalculateSprintMapping() throws SQLException {
        List<DbAggregationResult> records = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("2"))
                                .sprintCount(3)
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.sprint_mapping)
                                .calculation(JiraIssuesFilter.CALCULATION.sprint_mapping)
                                .sprintMappingSprintNameStartsWith("My sp")
                                .build(), false, null, null, Map.of())
                .getRecords();

        Assert.assertNotNull(records);
        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder().sprintName("My Sprint1").sprintId("0").integrationId("2").build(),
                DbAggregationResult.builder().sprintName("My Sprint2").sprintId("7").integrationId("2").build()
        );
        verifyRecords(records, expected);
    }

    private void verifyRecords(List<DbAggregationResult> actual, List<DbAggregationResult> expected) {
        Assert.assertEquals(CollectionUtils.isEmpty(actual), CollectionUtils.isEmpty(expected));
        if (CollectionUtils.isEmpty(actual)) {
            return;
        }
        Assert.assertEquals(actual.size(), expected.size());
        Map<Object, DbAggregationResult> actualMap = convertListToMap(actual);
        Map<Object, DbAggregationResult> expectedMap = convertListToMap(expected);
        for (Object key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private void verifyRecord(DbAggregationResult a, DbAggregationResult e) {
        Assert.assertEquals((e == null), (a == null));
        if (e == null || a == null) {
            return;
        }

        Assert.assertEquals(a.getKey(), e.getKey());
        Assert.assertEquals(a.getAdditionalKey(), e.getAdditionalKey());
        Assert.assertEquals(a.getTotalTickets(), e.getTotalTickets());
        verifyRecords(a.getStacks(), e.getStacks());
    }

    private Map<Object, DbAggregationResult> convertListToMap(List<DbAggregationResult> lst) {
        Map<Object, DbAggregationResult> map = new HashMap<>();
        for (DbAggregationResult dbAggregationResult : lst) {
            map.put(AcrossUniqueKey.fromDbAggregationResult(dbAggregationResult), dbAggregationResult);
        }
        return map;
    }
}
