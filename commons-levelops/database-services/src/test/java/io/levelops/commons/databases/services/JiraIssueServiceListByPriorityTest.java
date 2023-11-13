package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.*;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.*;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class JiraIssueServiceListByPriorityTest {
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
    private static JiraProjectService jiraProjectService;

    @Before
    public void setup() throws Exception {
        if (dataSource != null)
            return;
        dataSource =  DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        final JiraFieldService jiraFieldService = new JiraFieldService(dataSource);
        jiraProjectService = new JiraProjectService(dataSource);
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
        String input = ResourceUtils.getResourceAsString("json/databases/jira_issues_april.json");
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
        List<DbJiraProject> dbJiraProjects = getDbJiraProjects();
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
                if (randomIssue == null)
                    randomIssue = tmp;
                else
                    randomIssue = (new Random().nextInt(100)) > 50 ? tmp : randomIssue;
                if ("LEV-273".equalsIgnoreCase(tmp.getKey()))
                    jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, "2", currentTime, JiraIssueParser.JiraParserConfig.builder()
                            .epicLinkField("customfield_10014")
                            .storyPointsField("customfield_10030")
                            .customFieldConfig(entries)
                            .build()));
                jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, "1",
                        DateUtils.addSeconds(currentTime, 2 * -86400), JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build()));
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(),
                        tmp.getIngestedAt() - 86400L).isPresent())
                    throw new RuntimeException("This issue shouldnt exist.");
                jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, "1",
                        DateUtils.addSeconds(currentTime, -86400), JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build()));
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isPresent())
                    throw new RuntimeException("This issue shouldnt exist.");
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
        jiraIssueService.insert(company, randomIssue.toBuilder()
                .customFields(Map.of("customfield_12345", List.of("1.0", "2.0", "3.0")))
                .build());
        jiraIssueService.bulkUpdateEpicStoryPoints(company, "1",
                DateUtils.truncate(currentTime, Calendar.DATE).toInstant().getEpochSecond());
        Date currentTime = new Date();
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(1).name("sprint1")
                .integrationId(Integer.parseInt("1")).state("active").startDate(1617290995L)
                .endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(2).name("sprint2")
                .integrationId(Integer.parseInt("1")).state("closed").startDate(1617290985L)
                .endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(3).name("sprint3")
                .integrationId(Integer.parseInt("1")).state("closed").startDate(1617290975L)
                .endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
    }

    @Test
    public void testListByPriorityOrder() throws SQLException {
        DbListResponse<DbJiraIssue> descendingOrderIssuesList = jiraIssueService.list(company,
                JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.priority)
                        .ingestedAt(ingestedAt)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), null, Map.of("priority", SortingOrder.DESC), 0, 10000);
        assertThat(descendingOrderIssuesList.getRecords()).isNotEmpty();
        DefaultObjectMapper.prettyPrint(descendingOrderIssuesList.getRecords().stream().map(DbJiraIssue::getPriority).collect(Collectors.toList()));
        assertThat(descendingOrderIssuesList.getRecords().stream().map(DbJiraIssue::getPriority).collect(Collectors.toList()))
                .containsExactly("NOW", "NOW", "NOW", "HIGHEST", "HIGH", "HIGH", "BETWEEN MEDIUM AND HIGH", "BETWEEN MEDIUM AND HIGH", "BETWEEN MEDIUM AND HIGH", "BETWEEN MEDIUM AND HIGH", "BETWEEN MEDIUM AND HIGH", "BETWEEN MEDIUM AND HIGH", "BETWEEN MEDIUM AND HIGH", "MEDIUM", "MEDIUM", "LOW", "LOW", "LOWEST", "LOWEST", "LOWEST", "_UNPRIORITIZED_", "_UNPRIORITIZED_");

        DbListResponse<DbJiraIssue> ascendingOrderIssuesList = jiraIssueService.list(company,
                JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.project)
                        .ingestedAt(ingestedAt)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), null, Map.of("priority", SortingOrder.ASC), 0, 10000);
        assertThat(ascendingOrderIssuesList.getRecords()).isNotEmpty();
        assertThat(ascendingOrderIssuesList.getRecords().stream().map(DbJiraIssue::getPriority).collect(Collectors.toList()))
                .containsExactly("LOWEST", "LOWEST", "LOWEST", "LOW", "LOW", "MEDIUM", "MEDIUM", "BETWEEN MEDIUM AND HIGH", "BETWEEN MEDIUM AND HIGH", "BETWEEN MEDIUM AND HIGH", "BETWEEN MEDIUM AND HIGH", "BETWEEN MEDIUM AND HIGH", "BETWEEN MEDIUM AND HIGH", "BETWEEN MEDIUM AND HIGH", "HIGH", "HIGH", "HIGHEST", "NOW", "NOW", "NOW", "_UNPRIORITIZED_", "_UNPRIORITIZED_");
        final DbListResponse<DbJiraIssue> unsortedIssues = jiraIssueService.list(company,
                JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.priority)
                        .ingestedAt(ingestedAt)
                        .build(), null, Map.of(), 0, 10000);
        assertThat(unsortedIssues.getRecords()).isNotEmpty();
        assertThat(unsortedIssues.getRecords().size()).isEqualTo(descendingOrderIssuesList.getRecords().size());
        assertThat(unsortedIssues.getRecords()).isNotEqualTo(descendingOrderIssuesList.getRecords());
        Map<String, Integer> priorityOrderMap = getPriorityOrderMap();
        checkOrderForDbListResponse(descendingOrderIssuesList, ascendingOrderIssuesList, priorityOrderMap);
    }

    @Test
    public void testGroupByAndCalculateWithPriorityOrder() throws SQLException {
        List<DbAggregationResult> aggregationResultsWithAcrossPriority = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .across(JiraIssuesFilter.DISTINCT.priority)
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .build(),
                true,
                null, null, Map.of()).getRecords();
        checkOrderForAggregationResults(aggregationResultsWithAcrossPriority);
        List<DbAggregationResult> aggregationResults = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                        .across(JiraIssuesFilter.DISTINCT.priority)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .build(),
                true,
                null, null, Map.of()).getRecords();
        checkOrderForAggregationResults(aggregationResults);
        assertThat(jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .across(JiraIssuesFilter.DISTINCT.priority)
                        .calculation(JiraIssuesFilter.CALCULATION.response_time)
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .build(),
                true,
                null, null, Map.of()).getRecords()).isNotEmpty();
        checkOrderForAggregationResults(jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .across(JiraIssuesFilter.DISTINCT.priority)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .build(), List.of(JiraIssuesFilter.DISTINCT.priority),
                null, null, Map.of()).getRecords());
        checkOrderForAggregationResults(jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .across(JiraIssuesFilter.DISTINCT.project)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .ingestedAt(ingestedAt)
                        .build(), List.of(JiraIssuesFilter.DISTINCT.priority),
                null, null, Map.of()).getRecords());

    }

    private void checkOrderForAggregationResults(List<DbAggregationResult> aggregationResults) {
        Map<String, Integer> priorityOrderMap = getPriorityOrderMap();
        int previousOrder;
        for (DbAggregationResult aggregationResult : aggregationResults) {
            if (priorityOrderMap.containsKey(aggregationResult.getKey())) {
                Integer priorityOrder = priorityOrderMap.get(aggregationResult.getKey());
                previousOrder = priorityOrder;
                assertThat(priorityOrder).isGreaterThanOrEqualTo(previousOrder);
            }
        }
    }

    private void checkOrderForDbListResponse(DbListResponse<DbJiraIssue> descendingOrderIssuesList,
                                             DbListResponse<DbJiraIssue> ascendingOrderIssuesList,
                                             Map<String, Integer> priorityOrderMap) {
        int previousOrder;
        for (DbJiraIssue dbJiraIssue : descendingOrderIssuesList.getRecords()) {
            if (priorityOrderMap.containsKey(dbJiraIssue.getPriority())) {
                Integer priorityOrder = priorityOrderMap.get(dbJiraIssue.getPriority());
                previousOrder = priorityOrder;
                assertThat(priorityOrder).isGreaterThanOrEqualTo(previousOrder);
            }
        }
        for (DbJiraIssue dbJiraIssue : ascendingOrderIssuesList.getRecords()) {
            if (priorityOrderMap.containsKey(dbJiraIssue.getPriority())) {
                Integer priorityOrder = priorityOrderMap.get(dbJiraIssue.getPriority());
                previousOrder = priorityOrder;
                assertThat(priorityOrder).isLessThanOrEqualTo(previousOrder);
            }
        }
    }

    private Map<String, Integer> getPriorityOrderMap() {
        return jiraProjectService.getPriorities(company, List.of("1"), 0, 100).stream()
                .collect(Collectors.toMap(
                        DbJiraPriority::getName,
                        DbJiraPriority::getOrder,
                        (existing, replacement) -> existing));
    }

    @NotNull
    private List<DbJiraProject> getDbJiraProjects() throws IOException {
        String inputProjects = ResourceUtils.getResourceAsString("json/databases/jira_projects_april.json");
        PaginatedResponse<JiraProject> projects = m.readValue(inputProjects,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraProject.class));
        List<DbJiraProject> dbJiraProjects = projects.getResponse().getRecords().stream()
                .map(project -> DbJiraProject.fromJiraProject(project, "1"))
                .collect(Collectors.toList());
        return dbJiraProjects;
    }

}
