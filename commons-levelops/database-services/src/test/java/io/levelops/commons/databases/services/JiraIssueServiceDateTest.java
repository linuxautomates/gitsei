package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraProject;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
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
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueServiceDateTest {
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
    private static List<DbJiraIssue> jiraIssues;
    private static UserIdentityService userIdentityService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null) {
            return;
        }
        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        JiraFieldService jiraFieldService = jiraTestDbs.getJiraFieldService();
        JiraProjectService jiraProjectService = jiraTestDbs.getJiraProjectService();
        integrationService = jiraTestDbs.getIntegrationService();

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
        userIdentityService = jiraTestDbs.getUserIdentityService();
        userIdentityService.ensureTableExistence(company);

        jiraIssues = new ArrayList<>();
        String input = ResourceUtils.getResourceAsString("json/databases/jirausers_aug12.json");
        PaginatedResponse<JiraUser> jiraUsers = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraUser.class));
        jiraUsers.getResponse().getRecords().forEach(user -> {
            DbJiraUser tmp = DbJiraUser.fromJiraUser(user, "1");
            jiraIssueService.insertJiraUser(company, tmp);
            if (user.getDisplayName() != null) {
                try {
                    userIdentityService.batchUpsert(company,
                            List.of(DbScmUser.builder()
                                    .integrationId("1")
                                    .cloudId(user.getAccountId())
                                    .displayName(user.getDisplayName())
                                    .originalDisplayName(user.getDisplayName())
                                    .build()));
                } catch (SQLException throwables) {
                    System.out.println("Failed to insert into integration_users with display name: " + user.getDisplayName() + " , company: " + company + ", integration id:" + "1");
                }
            }
        });

        jiraIssues = new ArrayList<>();
        input = ResourceUtils.getResourceAsString("json/databases/jiraissues_jun62020.json");
        PaginatedResponse<JiraIssue> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = new Date();
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
                jiraIssues.add(tmp);
                if (randomIssue == null) {
                    randomIssue = tmp;
                } else {
                    randomIssue = (new Random().nextInt(100)) > 50 ? tmp : randomIssue;
                }
                if ("LEV-273".equalsIgnoreCase(tmp.getKey())) {
                    jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, "2", currentTime, JiraIssueParser.JiraParserConfig.builder()
                            .epicLinkField("customfield_10014")
                            .storyPointsField("customfield_10030")
                            .customFieldConfig(entries)
                            .build()));
                }
                jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, "1",
                        currentTime, JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build()));
                jiraIssueService.insert(company, tmp);
                List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
                versions.forEach(dbJiraVersion -> jiraIssueService.insertJiraVersion(company, dbJiraVersion));
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
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
                        .build(), "1"))));
        jiraIssueService.insert(company, randomIssue.toBuilder()
                .customFields(Map.of("customfield_12345", List.of("1.0", "2.0", "3.0")))
                .build());
        jiraIssueService.bulkUpdateEpicStoryPoints(company, "1",
                DateUtils.truncate(currentTime, Calendar.DATE).toInstant().getEpochSecond());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(1).name("sprint1").integrationId(Integer.parseInt("1")).state("active").startDate(1617290995L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(2).name("sprint2").integrationId(Integer.parseInt("1")).state("closed").startDate(1617290985L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(3).name("sprint3").integrationId(Integer.parseInt("1")).state("closed").startDate(1617290975L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
    }

    private Optional<String> userIdOf(String displayName) {
        return userIdentityService.getUserByDisplayName(company, "1", displayName);//todo. displayname.
    }

    @Test
    public void test() throws SQLException {
        List<DbAggregationResult> records = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .across(JiraIssuesFilter.DISTINCT.issue_created)
                        .aggInterval("day")
                        .ingestedAt(ingestedAt)
                        .build(),
                false,
                null, null, Map.of()).getRecords();
        assertThat(records.size()).isEqualTo(5);
        records = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .across(JiraIssuesFilter.DISTINCT.issue_created)
                        .acrossLimit(90)
                        .aggInterval("day")
                        .ingestedAt(ingestedAt)
                        .build(),
                false,
                null, null, Map.of()).getRecords();
        assertThat(records.size()).isEqualTo(5);

        records = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .across(JiraIssuesFilter.DISTINCT.issue_created)
                        .acrossLimit(1)
                        .aggInterval("day")
                        .ingestedAt(ingestedAt)
                        .build(),
                false,
                null, null, Map.of()).getRecords();
        assertThat(records.size()).isEqualTo(1);

        records = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .across(JiraIssuesFilter.DISTINCT.issue_created)
                        .aggInterval("month")
                        .ingestedAt(ingestedAt)
                        .build(),
                false,
                null, null, Map.of()).getRecords();

        assertThat(records.size()).isEqualTo(3);

        assertThat(records.get(0).getAdditionalKey()).isEqualTo("5-2020");
        assertThat(records.get(0).getTotalTickets()).isEqualTo(11);
        assertThat(records.get(2).getAdditionalKey()).isEqualTo("2-2020");
        assertThat(records.get(2).getTotalTickets()).isEqualTo(1);

        records = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .across(JiraIssuesFilter.DISTINCT.issue_created)
                        .aggInterval("quarter")
                        .ingestedAt(ingestedAt)
                        .build(),
                false,
                null, null, Map.of()).getRecords();

        assertThat(records.size()).isEqualTo(2);

        assertThat(records.get(0).getAdditionalKey()).isEqualTo("Q2-2020");
        assertThat(records.get(0).getTotalTickets()).isEqualTo(11);
        assertThat(records.get(1).getAdditionalKey()).isEqualTo("Q1-2020");
        assertThat(records.get(1).getTotalTickets()).isEqualTo(9);

        records = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .across(JiraIssuesFilter.DISTINCT.issue_created)
                        .aggInterval("year")
                        .ingestedAt(ingestedAt)
                        .build(),
                false,
                null, null, Map.of()).getRecords();

        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).getAdditionalKey()).isEqualTo("2020");
        assertThat(records.get(0).getTotalTickets()).isEqualTo(20);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.trend)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .build(),
                        true, null, null, Map.of())
                .getRecords().size()).isEqualTo(1);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.trend)
                                .calculation(JiraIssuesFilter.CALCULATION.hops)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(1);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.trend)
                                .calculation(JiraIssuesFilter.CALCULATION.bounces)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(1);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.trend)
                                .calculation(JiraIssuesFilter.CALCULATION.bounces)
                                .components(List.of("internalapi-levelops", "serverapi-levelops"))
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(1);
    }

    @Test
    public void testFilterByResolvedAtYear() throws SQLException {
        List<Long> listOfResolvedAtTimes = jiraIssues.stream().map(DbJiraIssue::getIssueResolvedAt).collect(Collectors.toList());
        List<String> actualList;
        List<String> expectedList;
        expectedList = listOfResolvedAtTimes.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(TimeUnit.MILLISECONDS.toMicros(instant), Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_resolved)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .aggInterval(String.valueOf(AGG_INTERVAL.year))
                        .build(),
                true, null, null, Map.of());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(instant -> extractDataComponentForDbResults(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(instant)), Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        DbListResponse<DbAggregationResult> stackedGroupByResult = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .ingestedAt(ingestedAt)
                        .hygieneCriteriaSpecs(Map.of())
                        .aggInterval(String.valueOf(AGG_INTERVAL.year))
                        .build(), List.of(JiraIssuesFilter.DISTINCT.issue_resolved), null, null, Map.of());
        actualList = stackedGroupByResult.getRecords()
                .stream()
                .filter(record -> !record.getStacks().isEmpty())
                .findFirst()
                .orElseThrow()
                .getStacks().stream().filter(Objects::nonNull).map(DbAggregationResult::getKey)
                .map(instant -> extractDataComponentForDbResults(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(instant)), Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
    }

    @Test
    public void testFilterByResolvedAtQuarter() throws SQLException {
        List<Long> listOfResolvedAtTimes = jiraIssues.stream().map(DbJiraIssue::getIssueResolvedAt)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result;
        DbListResponse<DbAggregationResult> stackedGroupByResult;
        List<String> actualList;
        List<String> expectedList;
        expectedList = listOfResolvedAtTimes.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(TimeUnit.MILLISECONDS.toMicros(instant), Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_resolved)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .aggInterval(String.valueOf(AGG_INTERVAL.quarter))
                        .build(),
                true, null, null, Map.of());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(instant -> extractDataComponentForDbResults(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(instant)), Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        stackedGroupByResult = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .ingestedAt(ingestedAt)
                        .hygieneCriteriaSpecs(Map.of())
                        .aggInterval(String.valueOf(AGG_INTERVAL.quarter))
                        .build(), List.of(JiraIssuesFilter.DISTINCT.issue_resolved), null, null, Map.of());

        actualList = stackedGroupByResult.getRecords()
                .stream()
                .filter(record -> !record.getStacks().isEmpty())
                .findFirst()
                .orElseThrow()
                .getStacks().stream().filter(Objects::nonNull).map(DbAggregationResult::getKey)
                .map(instant -> extractDataComponentForDbResults(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(instant)), Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
    }

    @Test
    public void testFilterByResolvedAtDay() throws SQLException {
        List<Long> listOfResolvedAtTimes = jiraIssues.stream().map(DbJiraIssue::getIssueResolvedAt)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result;
        DbListResponse<DbAggregationResult> stackedGroupByResult;
        List<String> actualList;
        List<String> expectedList;
        expectedList = listOfResolvedAtTimes.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(TimeUnit.MILLISECONDS.toMicros(instant), Calendar.DAY_OF_WEEK, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_resolved)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .aggInterval(String.valueOf(AGG_INTERVAL.day))
                        .build(),
                true, null, null, Map.of());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(instant -> extractDataComponentForDbResults(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(instant)), Calendar.DAY_OF_WEEK, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

        stackedGroupByResult = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .ingestedAt(ingestedAt)
                        .hygieneCriteriaSpecs(Map.of())
                        .aggInterval(String.valueOf(AGG_INTERVAL.day))
                        .build(), List.of(JiraIssuesFilter.DISTINCT.issue_resolved), null, null, Map.of());
        actualList = stackedGroupByResult.getRecords()
                .stream()
                .filter(record -> !record.getStacks().isEmpty())
                .findFirst()
                .orElseThrow()
                .getStacks().stream().filter(Objects::nonNull).map(DbAggregationResult::getKey)
                .map(instant -> extractDataComponentForDbResults(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(instant)), Calendar.DAY_OF_WEEK, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
    }

    @Test
    public void testFilterByResolvedAtWeek() throws SQLException {
        List<Long> listOfResolvedAtTimes = jiraIssues.stream().map(DbJiraIssue::getIssueResolvedAt)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result;
        DbListResponse<DbAggregationResult> stackedGroupByResult;
        List<String> actualList;
        List<String> expectedList;
        expectedList = listOfResolvedAtTimes.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(TimeUnit.MILLISECONDS.toMicros(instant), Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_resolved)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .aggInterval(String.valueOf(AGG_INTERVAL.week))
                        .build(),
                true, null, null, Map.of());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(instant -> extractDataComponentForDbResults(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(instant)), Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

        stackedGroupByResult = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .ingestedAt(ingestedAt)
                        .hygieneCriteriaSpecs(Map.of())
                        .aggInterval(String.valueOf(AGG_INTERVAL.week))
                        .build(), List.of(JiraIssuesFilter.DISTINCT.issue_resolved), null, null, Map.of());
        actualList = stackedGroupByResult.getRecords()
                .stream()
                .filter(record -> !record.getStacks().isEmpty())
                .findFirst()
                .orElseThrow()
                .getStacks().stream().filter(Objects::nonNull).map(DbAggregationResult::getKey)
                .map(instant -> extractDataComponentForDbResults(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(instant)), Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
    }

    @Test
    public void testListFilterByResolved() throws SQLException {
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .issueResolutionRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                .build(), null, Collections.emptyMap(), 1, 10000).getTotalCount()).isEqualTo(4);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .assignees(List.of(userIdOf("Meghana Dwarakanath").isPresent() ? userIdOf("Meghana Dwarakanath").get() : ""))
                .issueResolutionRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                .build(), null, Collections.emptyMap(), 1, 10000).getTotalCount()).isEqualTo(4);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .assignees(List.of(userIdOf("Meghana Dwarakanath").isPresent() ? userIdOf("Meghana Dwarakanath").get() : ""))
                .issueResolutionRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                .build(), null, Collections.emptyMap(), 1, 10000).getTotalCount()).isEqualTo(4);
    }

    private int extractDataComponentForDbResults(Long instant, int dateComponent, boolean isIntervalQuarter) {
        Calendar calendar = getPGCompatibleCalendar();
        calendar.setTimeInMillis(instant);
        if (isIntervalQuarter) {
            return (calendar.get(dateComponent) / 3) + 1;
        }
        return calendar.get(dateComponent);
    }

    /**
     * By definition, ISO weeks start on Mondays and the first week of a year contains January 4 of that year.
     * In other words, the first Thursday of a year is in week 1 of that year.
     * {@see https://tapoueh.org/blog/2017/06/postgresql-and-the-calendar/}
     */
    @NotNull
    private Calendar getPGCompatibleCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        return calendar;
    }

}
