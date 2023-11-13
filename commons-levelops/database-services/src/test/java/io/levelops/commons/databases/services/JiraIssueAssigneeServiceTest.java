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
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
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
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueAssigneeServiceTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static JiraIssueService jiraIssueService;
    private static JiraIssueStoryPointsDatabaseService jiraIssueStoryPointsDatabaseService;
    private static DbJiraIssue randomIssue;
    private static Date currentTime;
    private static Long ingestedAt;
    private static IntegrationService integrationService;
    private static UserIdentityService userIdentityService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {

        if (dataSource != null) {
            return;
        }
        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);

        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();
        jiraIssueStoryPointsDatabaseService = jiraTestDbs.getJiraIssueStoryPointsDatabaseService();

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
        jiraIssueService.ensureTableExistence(company);
        jiraIssueStoryPointsDatabaseService.ensureTableExistence(company);
        jiraFieldService.ensureTableExistence(company);
        jiraProjectService.ensureTableExistence(company);
        userIdentityService = jiraTestDbs.getUserIdentityService();
        userIdentityService.ensureTableExistence(company);

        String input = ResourceUtils.getResourceAsString("json/databases/jirausers_assignees.json");
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

        input = ResourceUtils.getResourceAsString("json/databases/jiraissues_assignees.json");
        PaginatedResponse<JiraIssue> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = new Date();
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
                        .customFieldConfig(List.of())
                        .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3)).build();
                if (randomIssue == null) {
                    randomIssue = tmp;
                } else {
                    randomIssue = (new Random().nextInt(100)) > 50 ? tmp : randomIssue;
                }
                if ("LEV-273".equalsIgnoreCase(tmp.getKey())) {
                    DbJiraIssue issue1 = JiraIssueParser.parseJiraIssue(issue, "2", currentTime, JiraIssueParser.JiraParserConfig.builder()
                            .epicLinkField("customfield_10014")
                            .storyPointsField("customfield_10030")
                            .customFieldConfig(List.of())
                            .build());
                    issue1 = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, issue1);
                    jiraIssueService.insert(company, issue1);
                }
                jiraIssueService.insert(company, tmp);
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isEmpty()) {
                    throw new RuntimeException("This issue should exist.");
                }
                List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
                versions.forEach(dbJiraVersion -> jiraIssueService.insertJiraVersion(company, dbJiraVersion));
                if (CollectionUtils.isNotEmpty(issue.getFields().getIssueLinks())) {
                    issue.getFields().getIssueLinks().forEach(jiraIssueLink -> {
                        if (ObjectUtils.isNotEmpty(jiraIssueLink.getOutwardIssue())) {
                            jiraIssueService.insertJiraLinkedIssueRelation(company, "1", issue.getKey(), jiraIssueLink.getOutwardIssue().getKey(), jiraIssueLink.getType().getOutward());
                        } else if (ObjectUtils.isNotEmpty(jiraIssueLink.getInwardIssue())) {
                            jiraIssueService.insertJiraLinkedIssueRelation(company, "1", issue.getKey(), jiraIssueLink.getInwardIssue().getKey(), jiraIssueLink.getType().getInward());
                        }
                    });
                }
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
        jiraIssueService.bulkUpdateEpicStoryPoints(company, "1",
                DateUtils.truncate(currentTime, Calendar.DATE).toInstant().getEpochSecond());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(1).name("sprint1").integrationId(Integer.parseInt("1")).state("active").startDate(1617290995L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(2).name("sprint2").integrationId(Integer.parseInt("1")).state("closed").startDate(1617290985L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(3).name("sprint3").integrationId(Integer.parseInt("1")).state("closed").startDate(1617290975L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(4).name("sprint4").integrationId(Integer.parseInt("1")).state("active").startDate(1590302565l).endDate(1590302585l).completedDate(1590302588l).updatedAt(currentTime.toInstant().getEpochSecond()).build());

        DbJiraProject project1 = DbJiraProject.builder()
                .cloudId("100")
                .key("P1")
                .name("project 1")
                .integrationId("1")
                .components(List.of("c"))
                .isPrivate(false)
                .build();
        jiraProjectService.insert(company, project1);
    }

    @Test
    public void testStatusAcross() throws SQLException {
        List<DbAggregationResult> aggs;
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .excludeStages(List.of("WONT DO", "DONE"))
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.status.toString(), SortingOrder.DESC))
                        .calculation(JiraIssuesFilter.CALCULATION.stage_times_report)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .excludeStages(List.of("WONT DO", "DONE"))
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.status.toString(), SortingOrder.DESC))
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.status.toString(), SortingOrder.DESC))
                        .calculation(JiraIssuesFilter.CALCULATION.hops)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.status.toString(), SortingOrder.DESC))
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(2);
    }

    @Test
    public void testAssigneeAcross() throws SQLException {
        List<DbAggregationResult> aggs = null;
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .calculation(JiraIssuesFilter.CALCULATION.stage_times_report)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .calculation(JiraIssuesFilter.CALCULATION.age)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .excludeStages(List.of("WONT DO", "DONE"))
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .calculation(JiraIssuesFilter.CALCULATION.response_time)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .calculation(JiraIssuesFilter.CALCULATION.story_points)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .calculation(JiraIssuesFilter.CALCULATION.assign_to_resolve)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.first_assignee)
                        .calculation(JiraIssuesFilter.CALCULATION.stage_times_report)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(5);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .calculation(JiraIssuesFilter.CALCULATION.hops)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
    }
}