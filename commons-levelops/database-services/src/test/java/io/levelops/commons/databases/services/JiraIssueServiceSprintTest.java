package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraProject;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
import io.levelops.commons.databases.models.database.jira.JiraAssigneeTime;
import io.levelops.commons.databases.models.database.jira.JiraStatusTime;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueServiceSprintTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static JiraIssueService jiraIssueService;
    private static DbJiraIssue randomIssue;
    private static Date currentTime;
    private static Long ingestedAt;
    private static IntegrationService integrationService;
    private static UserIdentityService userIdentityService;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null) {
            return;
        }

        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);

        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);

        final JiraFieldService jiraFieldService = new JiraFieldService(dataSource);
        final JiraProjectService jiraProjectService = new JiraProjectService(dataSource);
        integrationService = new IntegrationService(dataSource);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        new TagsService(dataSource).ensureTableExistence(company);
        new TagItemDBService(dataSource).ensureTableExistence(company);
        integrationService.ensureTableExistence(company);
        integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        integrationService.insertConfig(company, IntegrationConfig.builder()
                .integrationId("1")
                .config(Map.of("agg_custom_fields",
                        List.of(IntegrationConfig.ConfigEntry.builder()
                                .key("customfield_20001")
                                .name("hello")
                                .delimiter(",")
                                .build())))
                .build());
        integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test 2")
                .status("enabled")
                .build());
        jiraIssueService.ensureTableExistence(company);
        jiraFieldService.ensureTableExistence(company);
        jiraProjectService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService = jiraTestDbs.getUserIdentityService();
        userIdentityService.ensureTableExistence(company);
        String input = ResourceUtils.getResourceAsString("json/databases/jiraissues_jun62020.json");
        PaginatedResponse<JiraIssue> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = new Date(Instant.parse("2021-06-01T12:00:00-08:00").getEpochSecond());
        jiraFieldService.batchUpsert(company,
                List.of(DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("array").fieldKey("customfield_10048").fieldItems("user").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("string").fieldKey("customfield_12641").build(),
                        DbJiraField.builder().custom(true).name("hello").integrationId("1").fieldType("string").fieldKey("customfield_20001").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("string").fieldKey("customfield_12746").build()));
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
                .map(project -> DbJiraProject.fromJiraProject(project, "1"))
                .collect(Collectors.toList());
        if (dbJiraProjects.size() > 0) {
            jiraProjectService.batchUpsert(company, dbJiraProjects);
        }
        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .customFieldConfig(entries)
                        .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3)).build();
                if (randomIssue == null) {
                    randomIssue = tmp;
                } else {
                    randomIssue = (new Random().nextInt(100)) > 50 ? tmp : randomIssue;
                }
                if ("LEV-273".equalsIgnoreCase(tmp.getKey())) {
                    jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, "2", currentTime,
                            JiraIssueParser.JiraParserConfig.builder()
                                    .epicLinkField("customfield_10014")
                                    .storyPointsField("customfield_10030")
                                    .customFieldConfig(entries)
                                    .build()));
                }
                jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, "1",
                        DateUtils.addSeconds(currentTime, 2 * -86400), JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build()));
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(),
                        tmp.getIngestedAt() - 86400L).isPresent()) {
                    throw new RuntimeException("This issue shouldnt exist.");
                }
                jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, "1",
                        DateUtils.addSeconds(currentTime, -86400), JiraIssueParser.JiraParserConfig.builder()
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
            DbJiraUser tmp = DbJiraUser.fromJiraUser(issue, "1");
            jiraIssueService.insertJiraUser(company, tmp);
        });
        ingestedAt = DateUtils.truncate(currentTime, Calendar.DATE).toInstant().getEpochSecond();
        jiraFieldService.batchUpsert(company, List.of(Objects.requireNonNull(DbJiraField.fromJiraField(
                JiraField.builder()
                        .key("customfield_12345")
                        .custom(true)
                        .name("test_field")
                        .schema(JiraField.Schema.builder()
                                .type("array")
                                .items("string")
                                .build())
                        .build(), "1"))));
        jiraIssueService.insert(company, randomIssue.toBuilder()
                .customFields(Map.of("customfield_12345", List.of("1.0", "2.0", "3.0")))
                .build());
        jiraIssueService.bulkUpdateEpicStoryPoints(company, "1", ingestedAt);
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(1).name("sprint1").integrationId(Integer.parseInt("1")).state("active").startDate(1617290995L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(2).name("sprint2").integrationId(Integer.parseInt("1")).state("closed").startDate(1617290985L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(3).name("sprint3").integrationId(Integer.parseInt("1")).state("closed").startDate(1617290975L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());

        List<DbJiraIssue> moreIssues = List.of(
                DbJiraIssue.builder().key("UN-1").integrationId("1").project("TS").summary("Remove access for user").priority("MEDIUM").reporter("").status("To Do").issueType("Task")
                        .descSize(0).bounces(1).hops(1).numAttachments(1).issueCreatedAt(Long.valueOf(1613658414)).issueUpdatedAt(Long.valueOf(1613658414)).ingestedAt(ingestedAt).sprintIds(List.of(2, 3)).build(),
                DbJiraIssue.builder().key("UN-2").integrationId("1").project("LEV").summary("Summary").priority("MEDIUM").reporter("").status("DONE").issueType("Epic")
                        .descSize(0).bounces(0).hops(0).numAttachments(0).issueCreatedAt(Long.valueOf(1613658414)).issueUpdatedAt(Long.valueOf(1613658414)).ingestedAt(ingestedAt).sprintIds(List.of(2, 3)).build(),
                DbJiraIssue.builder().key("UN-3").integrationId("1").project("TS").summary("Remove access for user").priority("MEDIUM").reporter("").status("To Do").issueType("Task")
                        .descSize(0).bounces(1).hops(1).numAttachments(1).issueCreatedAt(Long.valueOf(1613658414)).issueUpdatedAt(Long.valueOf(1613658414)).ingestedAt(ingestedAt).sprintIds(List.of(1, 3)).build(),
                DbJiraIssue.builder().key("UN-4").integrationId("1").project("LEV").summary("Summary").priority("MEDIUM").reporter("").status("DONE").issueType("Epic")
                        .descSize(0).bounces(0).hops(0).numAttachments(0).issueCreatedAt(Long.valueOf(1613658414)).issueUpdatedAt(Long.valueOf(1613658414)).ingestedAt(ingestedAt).sprintIds(List.of(1, 3)).build(),
                DbJiraIssue.builder().key("UN-5").integrationId("1").project("TS").summary("Remove access for user").priority("MEDIUM").reporter("").status("To Do").issueType("Task")
                        .descSize(0).bounces(1).hops(1).numAttachments(1).issueCreatedAt(Long.valueOf(1613658414)).issueUpdatedAt(Long.valueOf(1613658414)).ingestedAt(ingestedAt).sprintIds(List.of(3)).build(),
                DbJiraIssue.builder().key("UN-6").integrationId("1").project("LEV").summary("Summary").priority("MEDIUM").reporter("").status("DONE").issueType("Epic")
                        .descSize(0).bounces(0).hops(0).numAttachments(0).issueCreatedAt(Long.valueOf(1613658414)).issueUpdatedAt(Long.valueOf(1613658414)).ingestedAt(ingestedAt).sprintIds(List.of(2)).build(),
                DbJiraIssue.builder().key("UN-7").integrationId("1").project("TS").summary("Remove access for user").priority("MEDIUM").reporter("").status("To Do").issueType("Task")
                        .descSize(0).bounces(1).hops(1).numAttachments(1).issueCreatedAt(Long.valueOf(1613658414)).issueUpdatedAt(Long.valueOf(1613658414)).ingestedAt(ingestedAt).sprintIds(List.of(1, 2, 3)).build(),
                DbJiraIssue.builder().key("UN-8").integrationId("1").project("LEV").summary("Summary").priority("MEDIUM").reporter("").status("DONE").issueType("Epic")
                        .descSize(0).bounces(0).hops(0).numAttachments(0).issueCreatedAt(Long.valueOf(1613658414)).issueUpdatedAt(Long.valueOf(1613658414)).ingestedAt(ingestedAt).sprintIds(List.of(1, 2, 3)).build()
        );
        moreIssues.forEach(issue -> {
            try {
                jiraIssueService.insert(company, issue);
            } catch (SQLException throwables) {
            }
        });
    }

    private Optional<String> userIdOf(String displayName) {
        return userIdentityService.getUserByDisplayName(company, "1", displayName);
    }

    @Test
    public void testWithLastSprintAndSprintIdFilter() throws SQLException {
        List<DbAggregationResult> aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1", "2"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("3"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1", "2"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("3"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("4"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(0);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("2"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1", "2"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("3", "1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("5", "3"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);

        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintIds(List.of("1", "2"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(27);
        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(24);
        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintIds(List.of("2"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(3);
        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintIds(List.of("3"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);
        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintIds(List.of("2", "3"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(4);
    }

    @Test
    public void testWithLastSprintAndSprintNameFilter() throws SQLException {
        List<DbAggregationResult> aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1", "sprint2"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint3"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1", "sprint2"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint3"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint4"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(0);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint2"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1", "sprint2"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint3", "sprint1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint5", "sprint3"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(true)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);

        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintNames(List.of("sprint1", "sprint2"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(27);
        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintNames(List.of("sprint1"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(24);
        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintNames(List.of("sprint2"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(3);
        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintNames(List.of("sprint3"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);
        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintNames(List.of("sprint2", "sprint3"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(4);
    }

    @Test
    public void testWithoutLastSprintAndWithSprintIdFilter() throws SQLException {
        List<DbAggregationResult> aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1", "2"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("3"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1", "2"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("3"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("4"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(0);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("2"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("1", "2"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("3", "1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintIds(List.of("5", "3"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);

        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintIds(List.of("1", "2"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(false)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(27);
        assertThat(jiraIssueService.list(
                        company,
                        JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(false)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(24);
        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintIds(List.of("2"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(false)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(25);
        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintIds(List.of("3"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(false)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(27);
        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintIds(List.of("2", "3"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(false)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(28);
    }

    @Test
    public void testWithoutLastSprintAndWithSprintNameFilter() throws SQLException {
        List<DbAggregationResult> aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1", "sprint2"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint3"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1", "sprint2"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint3"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint4"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(0);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint2"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1", "sprint2"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint3", "sprint1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint5", "sprint3"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterByLastSprint(false)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);

        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintNames(List.of("sprint1", "sprint2"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(false)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(27);
        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintNames(List.of("sprint1"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(false)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(24);
        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintNames(List.of("sprint2"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(false)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(25);
        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintNames(List.of("sprint3"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(false)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(27);
        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .sprintNames(List.of("sprint2", "sprint3"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(false)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(28);
    }

    //     @Test // commented out since values keep flipping by 1 randomnly... needs to be checked, potential issue
    public void testByLastSprintFilter() throws SQLException {
        DbListResponse<DbAggregationResult> actual = jiraIssueService.stackedGroupBy(company, JiraIssuesFilter.builder()
                .across(JiraIssuesFilter.DISTINCT.priority)
                .ingestedAt(ingestedAt)
                .filterByLastSprint(true)
                .build(), List.of(JiraIssuesFilter.DISTINCT.status), null, null, Map.of());
        assertThat(actual).isNotNull();
        DbListResponse<DbAggregationResult> expected = DbListResponse.of(List.of(
                DbAggregationResult.builder()
                        .key("MEDIUM").totalTickets(23L).stacks(List.of(
                                DbAggregationResult.builder().key("TO DO").totalTickets(7L).build(),
                                DbAggregationResult.builder().key("DONE").totalTickets(6L).build(),
                                DbAggregationResult.builder().key("IN PROGRESS").totalTickets(6L).build(),
                                DbAggregationResult.builder().key("To Do").totalTickets(4L).build()
                        )).build(),
                DbAggregationResult.builder()
                        .key("HIGH").totalTickets(2L).stacks(List.of(
                                DbAggregationResult.builder().key("DONE").totalTickets(1L).build(),
                                DbAggregationResult.builder().key("TO DO").totalTickets(1L).build()
                        )).build(),
                DbAggregationResult.builder()
                        .key("HIGHEST").totalTickets(3L).stacks(List.of(
                                DbAggregationResult.builder().key("DONE").totalTickets(2L).build(),
                                DbAggregationResult.builder().key("IN PROGRESS").totalTickets(1L).build()
                        )).build()
        ), 3);
        assertThat(actual.getTotalCount()).isEqualTo(expected.getTotalCount());
        assertThat(actual.getRecords()).containsExactlyInAnyOrderElementsOf(expected.getRecords());

        JiraIssuesFilter a = JiraIssuesFilter.builder()
                .hygieneCriteriaSpecs(Map.of())
                .partialMatch(Map.of("priority", Map.of("$begins", "HIG")))
                .ingestedAt(ingestedAt)
                .filterByLastSprint(true)
                .build();
        JiraIssuesFilter b = JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .customFields(Map.of(
                        "customfield_12641", List.of("Required"),
                        "customfield_12716", List.of("0.0"),
                        "customfield_12746", List.of("what this")
                ))
                .across(JiraIssuesFilter.DISTINCT.custom_field)
                .customAcross("customfield_12716")
                .filterByLastSprint(true)
                .build();
        assertThat(a.generateCacheHash()).isEqualTo(a.generateCacheHash());
        assertThat(b.generateCacheHash()).isEqualTo(b.generateCacheHash());
        assertThat(a.generateCacheHash()).isNotEqualTo(b.generateCacheHash());
        DbListResponse<DbJiraIssue> list = jiraIssueService.list(
                company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000);
        assertThat(list.getRecords().size()).isEqualTo(28);

        list = jiraIssueService.list(
                company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .parentKeys(List.of("LEV-902"))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000);
        assertThat(list.getRecords().size()).isEqualTo(0);

        list = jiraIssueService.list(
                company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        // .excludeParentKeys(List.of("LEV-652"))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000);
        assertThat(list.getRecords().size()).isEqualTo(28);

        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("priority", Map.of("$begins", "HIG")))
                                .excludeStages(List.of("DONE"))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(4);

        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("priority", Map.of("$begins", "SHY")))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(0);
        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("status", Map.of("$begins", "DO")))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(9);
        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("status", Map.of("$ends", "DO")))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(8);
        JiraAssigneeTime jat = jiraIssueService.listIssueAssigneesByTime(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null,
                        0,
                        10000)
                .getRecords().get(0);
        assertThat(jat.getTotalTime()).isLessThan(62408444);// this will start to fail eventually as the test gets older.

        DbListResponse<JiraAssigneeTime> dbListResponse = jiraIssueService.listIssueAssigneesByTime(company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("assignee", false))
                        .timeAssignees(List.of("_UNASSIGNED_"))
                        .filterByLastSprint(true)
                        .build(),
                null,
                0,
                10000);
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = jiraIssueService.listIssueAssigneesByTime(company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("assignee", false))
                        .filterByLastSprint(true)
                        .build(),
                null,
                0,
                10000);
        Assert.assertEquals(10, dbListResponse.getRecords().size());

        dbListResponse = jiraIssueService.listIssueAssigneesByTime(company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("assignee", false))
                        .excludeTimeAssignees(List.of("_UNASSIGNED_"))
                        .filterByLastSprint(true)
                        .build(),
                null,
                0,
                10000);
        Assert.assertEquals(10, dbListResponse.getRecords().size());

        JiraStatusTime jst = jiraIssueService.listIssueStatusesByTime(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .build(),
                        null,
                        0,
                        10000)
                .getRecords().get(0);
        assertThat(jst.getTotalTime()).isLessThan(62408444);// this will start to fail eventually as the test gets older.
        DbListResponse<JiraStatusTime> dbListResponse2 = jiraIssueService.listIssueStatusesByTime(company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .excludeStatuses(List.of("TO DO"))
                        .timeStatuses(List.of("TO DO"))
                        .build(),
                null,
                0,
                10000);
        Assert.assertEquals(10, dbListResponse2.getRecords().size());

        dbListResponse2 = jiraIssueService.listIssueStatusesByTime(company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .excludeStatuses(List.of("TO DO"))
                        .build(),
                null,
                0,
                10000);
        Assert.assertEquals(25, dbListResponse2.getRecords().size());

        integrationService.delete(company, "2");
        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .issueUpdatedRange(ImmutablePair.of(1584469718L, 1584469720L))
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);

        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("summary", Map.of("$begins", "Fix")))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);

        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("components", Map.of("$contains", "serverapi")))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);

        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("summary", Map.of("$contains", "designer")))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(2);

        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("customfield_12641", Map.of("$contains", "Not"),
                                        "summary", Map.of("$contains", "template")))
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);
        assertThat(jiraIssueService.list(
                        company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .issueUpdatedRange(ImmutablePair.of(1584469718L, 1584469720L))
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);
        assertThat(jiraIssueService.groupByAndCalculate(
                        company,
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .across(JiraIssuesFilter.DISTINCT.priority)
                                .ingestedAt(ingestedAt)
                                .issueCreatedRange(ImmutablePair.of(1584469711L, 1584469714L))
                                .filterByLastSprint(true)
                                .build(),
                        true,
                        null,
                        null, Map.of())
                .getTotalCount())
                .isEqualTo(1);

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .across(JiraIssuesFilter.DISTINCT.parent)
                        .ingestedAt(ingestedAt)
                        .filterByLastSprint(true)
                        .build(),
                false,
                null,
                null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getRecords().size()).isEqualTo(2);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("fix_version", true))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getTotalCount()).isEqualTo(28);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_10048")
                        .filterByLastSprint(true)
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(2);

        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_10149")
                        .filterByLastSprint(true)
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(1);
        assertThat(jiraIssueService.get(
                company,
                "LEV-996",
                "1",
                io.levelops.commons.dates.DateUtils.truncate(currentTime, Calendar.DATE)).isEmpty()).isEqualTo(false);
        try {
            jiraIssueService.insert(company,
                    randomIssue.toBuilder().key("TESTING-1123231").assigneeList(List.of(DbJiraAssignee.builder().build())).build());
            assertThat(true).isEqualTo(false);
        } catch (Exception ignored) {
        }
        assertThat(jiraIssueService.listIssueAssigneesByTime(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .filterByLastSprint(true)
                                .build(),
                        null,
                        0,
                        10000)
                .getTotalCount()).isEqualTo(20);
        assertThat(jiraIssueService.listIssueStatusesByTime(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .build(),
                        null,
                        0,
                        10000)
                .getTotalCount()).isEqualTo(33);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.trend)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .filterByLastSprint(true)
                                .build(),
                        true, null, null, Map.of())
                .getRecords().size()).isEqualTo(1);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1")).keys(List.of("LEV-1005"))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getRecords().get(0).getAssigneeList().size()).isEqualTo(1);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("customfield_12716", false))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getRecords()).hasSize(2);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("customfield_12716", true,
                                "priority", false, "project", false))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getRecords()).hasSize(26);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .fieldSize(Map.of("summary", Map.of("$lt", "30", "$gt", "20"), "assignee", Map.of("$lt", "12")))
                        .missingFields(Map.of("customfield_12716", true,
                                "priority", false, "project", false))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getRecords()).hasSize(1);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("customfield_12716", true, "customfield_0000", true,
                                "priority", false, "project", false))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getRecords()).hasSize(28);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("customfield_12716", true, "project", true))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getRecords()).hasSize(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.inactive_assignees))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getTotalCount()).isEqualTo(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.inactive_assignees,
                                JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getTotalCount()).isEqualTo(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .filterByLastSprint(true)
                        .integrationIds(List.of("1")).keys(List.of("LEV-1005")).build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getRecords().get(0).getAssigneeList().size()).isEqualTo(1);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .customFields(Map.of(
                                "customfield_12641", List.of("Required"),
                                "customfield_12716", List.of("0.0"),
                                "customfield_12746", List.of("what this")))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getTotalCount()).isEqualTo(2);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .fieldSize(Map.of("customfield_12641", Map.of("$gt", "25")))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getTotalCount()).isEqualTo(0);

        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.epic)
                        .filterByLastSprint(true)
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(20);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.priority)
                        .calculation(JiraIssuesFilter.CALCULATION.assign_to_resolve)
                        .filterByLastSprint(true)
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_12345")
                        .filterByLastSprint(true)
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .customFields(Map.of(
                                "customfield_12641", List.of("Required"),
                                "customfield_12716", List.of("0.0"),
                                "customfield_12746", List.of("what this")
                        ))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_12716")
                        .filterByLastSprint(true)
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(1);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .customFields(Map.of(
                                "customfield_12641", List.of("Required", " Required"),
                                "customfield_12716", List.of("0.0"),
                                "customfield_12746", List.of("what this")
                        ))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_12641")
                        .filterByLastSprint(true)
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(1);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .customFields(Map.of(
                                "customfield_12641", List.of("Required", "Not Required"),
                                "customfield_12716", List.of("0.0"),
                                "customfield_12746", List.of("what this")
                        ))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_12641")
                        .filterByLastSprint(true)
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .customFields(Map.of("customfield_12746", List.of("what this")))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_12641")
                        .filterByLastSprint(true)
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.epic)
                        .filterByLastSprint(true)
                        .build(),
                false, null, null, Map.of()).getTotalCount()).isEqualTo(20);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.priority)
                        .calculation(JiraIssuesFilter.CALCULATION.assign_to_resolve)
                        .filterByLastSprint(true)
                        .build(),
                false, null, null, Map.of()).getTotalCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .customFields(Map.of(
                                "customfield_12641", List.of("Required"),
                                "customfield_12716", List.of("0.0"),
                                "customfield_12746", List.of("what this")
                        ))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_12716")
                        .filterByLastSprint(true)
                        .build(),
                false, null, null, Map.of()).getTotalCount()).isEqualTo(1);
        assertThat(jiraIssueService.stackedGroupBy(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .integrationIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.custom_field)
                                .customStacks(List.of("customfield_12641"))
                                .customAcross("customfield_12746")
                                .filterByLastSprint(true)
                                .build(),
                        List.of(JiraIssuesFilter.DISTINCT.custom_field), null, null, Map.of())
                .getRecords().stream().findFirst().orElseThrow().getStacks()).hasSize(2);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .customFields(Map.of(
                                "customfield_12641", List.of("Required", " Required"),
                                "customfield_12716", List.of("0.0"),
                                "customfield_12746", List.of("what this")
                        ))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_12641")
                        .filterByLastSprint(true)
                        .build(),
                false, null, null, Map.of()).getTotalCount()).isEqualTo(1);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .customFields(Map.of(
                                "customfield_12641", List.of("Required", "Not Required"),
                                "customfield_12716", List.of("0.0"),
                                "customfield_12746", List.of("what this")
                        ))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_12641")
                        .filterByLastSprint(true)
                        .build(),
                false, null, null, Map.of()).getTotalCount()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .customFields(Map.of("customfield_12746", List.of("what this")))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_12641")
                        .filterByLastSprint(true)
                        .build(),
                false, null, null, Map.of()).getTotalCount()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .integrationIds(List.of("1"))
                                .customFields(Map.of("customfield_12641", List.of("Required", "Not Required")))
                                .across(JiraIssuesFilter.DISTINCT.custom_field)
                                .customAcross("customfield_12641")
                                .filterByLastSprint(true)
                                .build(),
                        false, null, null, Map.of())
                .getRecords().get(0).getTotalTickets())
                .isEqualTo(2);

        //test exclusions
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeProjects(List.of("LEV"))
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 100).getTotalCount())
                .isEqualTo(13);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludePriorities(List.of("MEDIUM"))
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 100).getTotalCount())
                .isEqualTo(18);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeCustomFields(Map.of("customfield_12641", List.of("Required")))
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 100).getTotalCount())
                .isEqualTo(506);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeCustomFields(Map.of("customfield_12641", List.of("Not Required")))
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 100).getTotalCount())
                .isEqualTo(507);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeAssignees(List.of(userIdOf("Harsh Jariwala").isPresent() ? userIdOf("Harsh Jariwala").get() : ""))
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 100).getTotalCount())
                .isEqualTo(464);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeIssueTypes(List.of("TASK", "STORY"))
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 100).getTotalCount())
                .isEqualTo(181);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeFixVersions(List.of("123"))
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 100).getTotalCount())
                .isEqualTo(506);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeVersions(List.of("456"))
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 100).getTotalCount())
                .isEqualTo(506);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .versions(List.of("456"))
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 100).getTotalCount())
                .isEqualTo(2);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeComponents(List.of("internalapi-levelops", "commons-levelops"))
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 100).getTotalCount())
                .isEqualTo(488);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeLabels(List.of("production", "plugins"))
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 100).getTotalCount())
                .isEqualTo(503);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeEpics(List.of("LEV-671", "LEV-26", "LEV-358"))
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 100).getTotalCount())
                .isEqualTo(260);
        //test sorting
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt).integrationIds(List.of("1")).filterByLastSprint(true).build(),
                        null, Map.of("hops", SortingOrder.DESC), 0, 100)
                .getRecords().get(0).getHops())
                .isEqualTo(29);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt).integrationIds(List.of("1")).filterByLastSprint(true).build(),
                        null, Map.of("bounces", SortingOrder.DESC), 0, 100)
                .getRecords().get(0).getBounces())
                .isEqualTo(24);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt).integrationIds(List.of("1")).filterByLastSprint(true).build(),
                        null, Map.of("num_attachments", SortingOrder.DESC), 0, 100)
                .getRecords().get(0).getNumAttachments())
                .isEqualTo(3);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt).integrationIds(List.of("1")).filterByLastSprint(true).build(),
                        null, Map.of("hops", SortingOrder.ASC), 0, 100)
                .getRecords().get(0).getHops())
                .isEqualTo(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt).integrationIds(List.of("1")).filterByLastSprint(true).build(),
                        null, Map.of("bounces", SortingOrder.ASC), 0, 100)
                .getRecords().get(0).getBounces())
                .isEqualTo(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt).integrationIds(List.of("1")).filterByLastSprint(true).build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getRecords().get(0).getNumAttachments())
                .isEqualTo(0);
        //test deletes
        jiraIssueService.cleanUpOldData(
                company, currentTime.toInstant().getEpochSecond(), 86400 * 2L);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt).keys(List.of("TESTING-1123231")).filterByLastSprint(true).build(),
                        null, Map.of("issue_resolved_at", SortingOrder.DESC), 0, 100)
                .getTotalCount())
                .isEqualTo(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .filterByLastSprint(true)
                        .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.poor_description,
                                JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time))
                        .build(), null, Map.of("issue_resolved_at", SortingOrder.DESC),
                0, 100).getTotalCount())
                .isEqualTo(217);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .filterByLastSprint(true)
                        .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.poor_description,
                                JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time))
                        .build(),
                null, Map.of("issue_resolved_at", SortingOrder.DESC),
                0, 100).getTotalCount())
                .isEqualTo(263);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .issueTypes(List.of("EPIC"))
                                .integrationIds(List.of("1"))
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("issue_resolved_at", SortingOrder.DESC),
                        0, 100)
                .getTotalCount())
                .isEqualTo(21);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .priorities(List.of("HIGHEST"))
                        .integrationIds(List.of("1"))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("issue_created_at", SortingOrder.DESC),
                0, 100).getTotalCount()).isEqualTo(6);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                        .integrationIds(List.of("1"))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("issue_updated_at", SortingOrder.DESC),
                0, 100).getTotalCount())
                .isEqualTo(508);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                        .integrationIds(List.of("1"))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("hops", SortingOrder.DESC),
                0, 100).getTotalCount())
                .isEqualTo(508);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                        .integrationIds(List.of("1"))
                        .projects(List.of("TS"))
                        .filterByLastSprint(true)
                        .build(),
                null, Map.of("bounces", SortingOrder.DESC),
                0, 100).getTotalCount())
                .isEqualTo(5);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .integrationIds(List.of("1"))
                                .components(List.of("internalapi-levelops", "serverapi-levelops"))
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("desc_size", SortingOrder.DESC), 0, 100)
                .getTotalCount())
                .isEqualTo(74);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .integrationIds(List.of("1"))
                                .components(List.of("internalapi-levelops"))
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of(), 0, 100)
                .getTotalCount()).isEqualTo(17);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .statuses(List.of("DONE"))
                                .assignees(List.of(userIdOf("Maxime Bellier").isPresent() ? userIdOf("Maxime Bellier").get() : ""))
                                .integrationIds(List.of("1"))
                                .components(List.of("serverapi-levelops"))
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of(), 0, 100)
                .getTotalCount()).isEqualTo(6);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .statuses(List.of("DONE"))
                                .assignees(List.of(userIdOf("Maxime Bellier").isPresent() ? userIdOf("Maxime Bellier").get() : ""))
                                .integrationIds(List.of("1"))
                                .components(List.of("serverapi-levelops"))
                                .summary("the dmg")
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of(), 0, 100)
                .getTotalCount()).isEqualTo(1);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .statuses(List.of("DONE"))
                                .assignees(List.of(userIdOf("Maxime Bellier").isPresent() ? userIdOf("Maxime Bellier").get() : ""))
                                .integrationIds(List.of("1"))
                                .components(List.of("serverapi-levelops"))
                                .summary("the dmg")
                                .keys(List.of("LEV-647"))
                                .issueTypes(List.of("BUG"))
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of("issue_resolved_at", SortingOrder.DESC), 0, 100)
                .getTotalCount()).isEqualTo(1);
        // test state transit time
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .integrationIds(List.of("1"))
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of(), 0, 100)
                .getRecords()
                .stream()
                .filter(i -> CollectionUtils.isNotEmpty(i.getStatuses()))
                .findAny()
                .orElseThrow()
                .getStatuses()
                .stream()
                .map(DbJiraStatus::getStartTime)
                .collect(Collectors.toList())).isNotEmpty();
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .fromState("TO DO")
                                .toState("DONE")
                                .integrationIds(List.of("1"))
                                .components(List.of("internalapi-levelops"))
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of(), 0, 100)
                .getTotalCount()).isEqualTo(14);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .fromState("TO DO")
                                .toState("DONE")
                                .integrationIds(List.of("1"))
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of(), 0, 100)
                .getTotalCount()).isEqualTo(374);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .fromState("TO DO")
                                .toState("DONE")
                                .integrationIds(List.of("1"))
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of(), 0, 100)
                .getRecords()
                .stream()
                .map(DbJiraIssue::getStateTransitionTime)
                .filter(Objects::isNull)
                .findAny()).isEmpty();
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .fromState("TO DO")
                                .toState("DONE")
                                .integrationIds(List.of("1"))
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of(), 0, 100)
                .getRecords()
                .stream()
                .map(DbJiraIssue::getStateTransitionTime)
                .filter(transitTime -> transitTime < 0)
                .collect(Collectors.toList())).isEmpty();
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .fromState("DONE")
                                .toState("TO DO")
                                .integrationIds(List.of("1"))
                                .components(List.of("internalapi-levelops"))
                                .filterByLastSprint(true)
                                .build(),
                        null, Map.of(), 0, 100)
                .getTotalCount()).isEqualTo(0);

        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .fromState("TO DO")
                        .toState("DONE")
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .filterByLastSprint(true)
                        .build(), false, null, null, Map.of())
                .getTotalCount()).isEqualTo(13);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .fromState("TO DO")
                        .toState("DONE")
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .filterByLastSprint(true)
                        .build(), false, null, null, Map.of())
                .getTotalCount()).isEqualTo(4);

        // test first assignee filter and across
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .firstAssignees(List.of(userIdOf("Meghana Dwarakanath").isPresent() ? userIdOf("Meghana Dwarakanath").get() : ""))
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 1000).getTotalCount()).isEqualTo(149);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .firstAssignees(List.of(userIdOf("Harsh Jariwala").isPresent() ? userIdOf("Harsh Jariwala").get() : ""))
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 1000).getTotalCount()).isEqualTo(43);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .filterByLastSprint(true)
                        .build(), null, Map.of(), 0, 1000)
                .getTotalCount()).isEqualTo(508);

        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .firstAssignees(List.of(userIdOf("Meghana Dwarakanath").isPresent() ? userIdOf("Meghana Dwarakanath").get() : ""))
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.first_assignee)
                .filterByLastSprint(true)
                .build(), false, null, null, Map.of()).getRecords().get(0).getTotalTickets()).isEqualTo(149);
        List<DbAggregationResult> aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.first_assignee)
                .filterByLastSprint(true)
                .build(), false, null, null, Map.of()).getRecords();
        assertThat(aggs.stream()
                .map(DbAggregationResult::getTotalTickets)
                .min(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(1);
        assertThat(aggs.stream()
                .map(DbAggregationResult::getTotalTickets)
                .max(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(149);

        // test first assignee filter and across
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .firstAssignees(List.of(userIdOf("Meghana Dwarakanath").isPresent() ? userIdOf("Meghana Dwarakanath").get() : ""))
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 1000).getTotalCount()).isEqualTo(149);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .firstAssignees(List.of(userIdOf("Harsh Jariwala").isPresent() ? userIdOf("Harsh Jariwala").get() : ""))
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .filterByLastSprint(true)
                .build(), null, Map.of(), 0, 1000).getTotalCount()).isEqualTo(43);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .filterByLastSprint(true)
                        .build(), null, Map.of(), 0, 1000)
                .getTotalCount()).isEqualTo(508);

        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .firstAssignees(List.of(userIdOf("Meghana Dwarakanath").isPresent() ? userIdOf("Meghana Dwarakanath").get() : ""))
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.first_assignee)
                .filterByLastSprint(true)
                .build(), false, null, null, Map.of()).getRecords().get(0).getTotalTickets()).isEqualTo(149);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.first_assignee)
                .filterByLastSprint(true)
                .build(), false, null, null, Map.of()).getRecords();
        assertThat(aggs.stream()
                .map(DbAggregationResult::getTotalTickets)
                .min(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(1);
        assertThat(aggs.stream()
                .map(DbAggregationResult::getTotalTickets)
                .max(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(149);
    }
}
