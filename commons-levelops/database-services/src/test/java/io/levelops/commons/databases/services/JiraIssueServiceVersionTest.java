package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraProject;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
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
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueServiceVersionTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static JiraIssueService jiraIssueService;
    private static Date currentTime;
    private static Long ingestedAt;
    private static IntegrationService integrationService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;

        dataSource =  DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        final JiraFieldService jiraFieldService = jiraTestDbs.getJiraFieldService();
        final JiraProjectService jiraProjectService = jiraTestDbs.getJiraProjectService();
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
        integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test 3")
                .status("enabled")
                .build());
        jiraIssueService.ensureTableExistence(company);
        jiraFieldService.ensureTableExistence(company);
        jiraProjectService.ensureTableExistence(company);
        String input = ResourceUtils.getResourceAsString("json/databases/jira_issues2.json");
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
                tmp = tmp.toBuilder().sprintIds(List.of(1,2,3)).build();
                jiraIssueService.insert(company, tmp);
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isEmpty())
                    throw new RuntimeException("This issue should exist.");
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
        jiraIssueService.bulkUpdateEpicStoryPoints(company, "1",
                DateUtils.truncate(currentTime, Calendar.DATE).toInstant().getEpochSecond());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(1).name("sprint1").integrationId(Integer.parseInt("1")).state("active").startDate(1617290995L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(2).name("sprint2").integrationId(Integer.parseInt("1")).state("closed").startDate(1617290985L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(3).name("sprint3").integrationId(Integer.parseInt("1")).state("closed").startDate(1617290975L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
    }

    @Test
    public void testVersionsInsert() throws IOException, ParseException {
        String input = ResourceUtils.getResourceAsString("json/databases/jira_versions_issue.json");
        JiraIssue issue = m.readValue(input, m.getTypeFactory().constructType(JiraIssue.class));
        List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
        versions.forEach(version -> jiraIssueService.insertJiraVersion(company, version));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        DbJiraVersion version1 = DbJiraVersion.builder()
                .versionId(10005)
                .projectId(10004)
                .name("AugRelease")
                .description("test-5")
                .integrationId(1)
                .released(false)
                .archived(false)
                .overdue(false)
                .startDate(simpleDateFormat.parse("2021-08-01").toInstant())
                .endDate(simpleDateFormat.parse("2021-08-31").toInstant())
                .build();
        Optional<DbJiraVersion> dbVersion1 = jiraIssueService.getJiraVersion(company, 1, 10005);
        Assertions.assertThat(dbVersion1.isPresent()).isTrue();
        validateVersionsEqual(version1, dbVersion1.get());
        DbJiraVersion version2 = DbJiraVersion.builder()
                .versionId(10006)
                .projectId(10004)
                .name("JuneRelease")
                .description("test-6")
                .integrationId(1)
                .released(false)
                .archived(false)
                .overdue(false)
                .endDate(simpleDateFormat.parse("2021-07-31").toInstant())
                .build();
        Optional<DbJiraVersion> dbVersion2 = jiraIssueService.getJiraVersion(company, 1, 10006);
        Assertions.assertThat(dbVersion2.isPresent()).isTrue();
        validateVersionsEqual(version2, dbVersion2.get());
        DbJiraVersion version3 = DbJiraVersion.builder()
                .versionId(10007)
                .projectId(10004)
                .name("SepRelease")
                .description("test-7")
                .integrationId(1)
                .released(false)
                .archived(false)
                .overdue(false)
                .startDate(simpleDateFormat.parse("2021-09-01").toInstant())
                .endDate(simpleDateFormat.parse("2021-09-31").toInstant())
                .build();
        Optional<DbJiraVersion> dbVersion3 = jiraIssueService.getJiraVersion(company, 1, 10007);
        Assertions.assertThat(dbVersion3.isPresent()).isTrue();
        validateVersionsEqual(version3, dbVersion3.get());
    }

    @Test
    public void testIssuesList() throws SQLException {
        DbListResponse<DbJiraIssue> response = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .build(), null, Map.of(), 0, 100);
        assertThat(response.getTotalCount()).isEqualTo(17);
        response = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .versions(List.of("New Version 3"))
                        .build(), null, Map.of("version", SortingOrder.ASC), 0, 100);
        assertThat(response.getTotalCount()).isEqualTo(3);
        validateListResponse(response, SortingOrder.ASC, false);
        response = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .fixVersions(List.of("New Version 3"))
                        .build(), null, Map.of("fix_version", SortingOrder.ASC), 0, 100);
        assertThat(response.getTotalCount()).isEqualTo(3);
        validateListResponse(response, SortingOrder.ASC, true);
        response = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .build(), null, Map.of("version", SortingOrder.ASC), 0, 100);
        assertThat(response.getTotalCount()).isEqualTo(19);
        validateListResponse(response, SortingOrder.ASC, false);
        response = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .versions(List.of("New Version 3", "New Version 1"))
                        .build(), null, Map.of("version", SortingOrder.DESC), 0, 100);
        assertThat(response.getTotalCount()).isEqualTo(11);
        validateListResponse(response, SortingOrder.DESC, false);
        response = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .build(), null, Map.of("fix_version", SortingOrder.ASC), 0, 100);
        assertThat(response.getTotalCount()).isEqualTo(20);
        validateListResponse(response, SortingOrder.ASC, true);
        response = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .fixVersions(List.of("New Version 3", "New Version 1"))
                        .build(), null, Map.of("fix_version", SortingOrder.DESC), 0, 100);
        assertThat(response.getTotalCount()).isEqualTo(11);
        validateListResponse(response, SortingOrder.DESC, true);
    }

    @Test
    public void testIssuesGroupByAndCalculate() throws SQLException {
        List<DbAggregationResult> results = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .versions(List.of("New Version 3"))
                        .across(JiraIssuesFilter.DISTINCT.version)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(results.size()).isEqualTo(1);
        validateGroupByResults(results, SortingOrder.DESC);
        results = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.version)
                        .sort(Map.of("version", SortingOrder.ASC))
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(results.size()).isEqualTo(4);
        validateGroupByResults(results, SortingOrder.ASC);
        results = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.version)
                        .sort(Map.of("version", SortingOrder.DESC))
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(results.size()).isEqualTo(4);
        validateGroupByResults(results, SortingOrder.DESC);
        results = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.version)
                        .sort(Map.of("version", SortingOrder.ASC))
                        .versions(List.of("New Version 1", "New Version 2"))
                        .filterAcrossValues(true)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(results.size()).isEqualTo(2);
        validateGroupByResults(results, SortingOrder.ASC);
        results = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.version)
                        .sort(Map.of("version", SortingOrder.DESC))
                        .versions(List.of("New Version 2", "JulyRelease"))
                        .filterAcrossValues(true)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(results.size()).isEqualTo(2);
        validateGroupByResults(results, SortingOrder.DESC);

        results = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.fix_version)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(results.size()).isEqualTo(4);
        validateGroupByResults(results, SortingOrder.ASC);
        results = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .sort(Map.of("fix_version", SortingOrder.DESC))
                        .across(JiraIssuesFilter.DISTINCT.fix_version)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(results.size()).isEqualTo(4);
        validateGroupByResults(results, SortingOrder.DESC);
        results = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.fix_version)
                        .sort(Map.of("fix_version", SortingOrder.ASC))
                        .versions(List.of("New Version 1", "New Version 2"))
                        .filterAcrossValues(true)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(results.size()).isEqualTo(2);
        validateGroupByResults(results, SortingOrder.ASC);
        results = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.fix_version)
                        .sort(Map.of("fix_version", SortingOrder.DESC))
                        .versions(List.of("New Version 2", "JulyRelease"))
                        .filterAcrossValues(true)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(results.size()).isEqualTo(2);
        validateGroupByResults(results, SortingOrder.DESC);
        results = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .sort(Map.of("fix_version", SortingOrder.ASC))
                        .versions(List.of("New Version 2", "JulyRelease"))
                        .across(JiraIssuesFilter.DISTINCT.fix_version)
                        .filterAcrossValues(true)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(results.size()).isEqualTo(2);
        validateGroupByResults(results, SortingOrder.ASC);
        results = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .sort(Map.of("fix_version", SortingOrder.DESC))
                        .versions(List.of("New Version 2", "JulyRelease"))
                        .filterAcrossValues(true)
                        .across(JiraIssuesFilter.DISTINCT.fix_version)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(results.size()).isEqualTo(2);
        validateGroupByResults(results, SortingOrder.DESC);
        results = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.fix_version)
                        .sort(Map.of("fix_version", SortingOrder.ASC))
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(results.size()).isEqualTo(4);
        validateGroupByResults(results, SortingOrder.ASC);
        results = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.fix_version)
                        .sort(Map.of("fix_version", SortingOrder.DESC))
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(results.size()).isEqualTo(4);
        validateGroupByResults(results, SortingOrder.DESC);
    }

    private static final Map<String, Integer> endDateMap = Map.of(
            "New Version 1", 1, "New Version 2", 2, "New Version 3", 3, "JulyRelease", 4
    );

    private static void validateListResponse(DbListResponse<DbJiraIssue> response,
                                             SortingOrder sortingOrder,
                                             boolean validateFixVersions) {
        List<Integer> list = response.getRecords().stream()
                .filter(dbJiraIssue -> CollectionUtils.isNotEmpty(
                        validateFixVersions ? dbJiraIssue.getFixVersions() : dbJiraIssue.getVersions()))
                .map(dbJiraIssue -> (validateFixVersions ? dbJiraIssue.getFixVersions() : dbJiraIssue.getVersions())
                        .stream()
                        .filter(endDateMap::containsKey)
                        .map(endDateMap::get)
                        .reduce(Integer::min)
                        .orElse(-1))
                .collect(Collectors.toList());
        if (sortingOrder.equals(SortingOrder.ASC)) {
            assertThat(list).isSorted();
        } else {
            assertThat(list).isSortedAccordingTo(Comparator.reverseOrder());
        }
    }

    private static void validateGroupByResults(List<DbAggregationResult> results, SortingOrder sortingOrder) {
        List<Integer> startDateOrder = results.stream()
                .map(DbAggregationResult::getKey)
                .filter(Objects::nonNull)
                .filter(endDateMap::containsKey)
                .map(endDateMap::get)
                .collect(Collectors.toList());
        if (sortingOrder.equals(SortingOrder.ASC)) {
            assertThat(startDateOrder).isSorted();
        } else {
            assertThat(startDateOrder).isSortedAccordingTo(Comparator.reverseOrder());
        }
    }

    private static void validateVersionsEqual(DbJiraVersion e, DbJiraVersion a) {
        Assert.assertEquals(e.getVersionId(), a.getVersionId());
        Assert.assertEquals(e.getProjectId(), a.getProjectId());
        Assert.assertEquals(e.getName(), a.getName());
        Assert.assertEquals(e.getDescription(), a.getDescription());
        Assert.assertEquals(e.getIntegrationId(), a.getIntegrationId());
        Assert.assertEquals(e.getReleased(), a.getReleased());
        Assert.assertEquals(e.getArchived(), a.getArchived());
        Assert.assertEquals(e.getOverdue(), a.getOverdue());
        Assert.assertEquals(e.getStartDate(), a.getStartDate());
        Assert.assertEquals(e.getEndDate(), a.getEndDate());
    }

    @Test
    public void testFixVersionUpdatedAtField() throws IOException {
        JiraProject jiraProject = ResourceUtils.getResourceAsObject("json/databases/jira/project.json", JiraProject.class);
        DbJiraProject dbJiraProject = DbJiraProject.fromJiraProject(jiraProject, "3");
        List<DbJiraVersion> versions = DbJiraVersion.fromJiraProject(dbJiraProject, "3");
        versions.forEach(version -> jiraIssueService.insertJiraVersion(company, version));
        Optional<DbJiraVersion> jiraVersion = jiraIssueService.getJiraVersion(company, 3, 12303);
        Assert.assertEquals(Optional.of(1698760149L), Optional.of(jiraVersion.get().getFixVersionUpdatedAt()));
    }
}
