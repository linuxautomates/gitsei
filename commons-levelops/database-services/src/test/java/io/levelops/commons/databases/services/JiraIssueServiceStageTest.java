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
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraField;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraUser;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class JiraIssueServiceStageTest {
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

    private Optional<String> userIdOf(String displayName) {
        return userIdentityService.getUserByDisplayName(company, "1", displayName);
    }

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource =  DatabaseTestUtils.setUpDataSource(pg, company);

        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);

        final JiraFieldService jiraFieldService = new JiraFieldService(dataSource);
        final JiraProjectService jiraProjectService = new JiraProjectService(dataSource);
        integrationService = new IntegrationService(dataSource);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        new TagsService(dataSource).ensureTableExistence(company);
        new TagItemDBService(dataSource).ensureTableExistence(company);
        integrationService.ensureTableExistence(company);
        new JiraIssueSprintMappingDatabaseService(dataSource).ensureTableExistence(company);
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

        input = ResourceUtils.getResourceAsString("json/databases/jirausers_aug12.json");
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

        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                tmp = tmp.toBuilder().sprintIds(List.of(1,2,3)).build();
                if (randomIssue == null)
                    randomIssue = tmp;
                else
                    randomIssue = (new Random().nextInt(100)) > 50 ? tmp : randomIssue;
                if ("LEV-273".equalsIgnoreCase(tmp.getKey())) {
                    DbJiraIssue issue1 = JiraIssueParser.parseJiraIssue(issue, "2", currentTime,
                            JiraIssueParser.JiraParserConfig.builder()
                                    .epicLinkField("customfield_10014")
                                    .storyPointsField("customfield_10030")
                                    .customFieldConfig(entries)
                                    .build());
                    issue1 = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, issue1);
                    jiraIssueService.insert(company, issue1);
                }
                DbJiraIssue issue2 = JiraIssueParser.parseJiraIssue(issue, "1",
                        DateUtils.addSeconds(currentTime, 2 * -86400),
                        JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build());
                issue2 = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, issue2);
                jiraIssueService.insert(company, issue2);
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(),
                        tmp.getIngestedAt() - 86400L).isPresent())
                    throw new RuntimeException("This issue shouldnt exist.");
                DbJiraIssue issue3 = JiraIssueParser.parseJiraIssue(issue, "1",
                        DateUtils.addSeconds(currentTime, -86400),
                        JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build());
                issue3 = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, issue3);
                jiraIssueService.insert(company, issue3);
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
        jiraIssueService.insert(company,DbJiraIssue.builder().key("UN-35").integrationId("1").project("TS").summary("Remove access for user")
                .components(List.of()).labels(List.of()).versions(List.of()).fixVersions(List.of()).descSize(51).priority("MEDIUM")
                .reporter("").status("BLOCKED").issueType("Task").bounces(1).hops(1).numAttachments(1).issueCreatedAt(Long.valueOf(1613658414))
                .issueUpdatedAt(Long.valueOf(1613658414)).ingestedAt(ingestedAt).sprintIds(List.of(1)).customFields(Map.of()).salesforceFields(Map.of()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(1).name("sprint1").integrationId(Integer.parseInt("1")).state("active").startDate(1617290995L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(2).name("sprint2").integrationId(Integer.parseInt("1")).state("closed").startDate(1617290985L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(3).name("sprint3").integrationId(Integer.parseInt("1")).state("closed").startDate(1617290975L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());

    }

    @Test
    public void testStageFilter() throws SQLException {
        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.stage)
                        .build(),
                true,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(4);
        assertThat(dbAggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsAll(List.of("DONE","IN PROGRESS","TO DO","_UNKNOWN_"));

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse1 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .excludeStages(List.of("SELECTED FOR DEVELOPMENT","TO DO"))
                        .across(JiraIssuesFilter.DISTINCT.stage)
                        .build(),
                true,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse1.getTotalCount()).isEqualTo(3);
        assertThat(dbAggregationResultDbListResponse1.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsAll(List.of("DONE","IN PROGRESS","_UNKNOWN_"));

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse2 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.stage)
                        .build(),
                true,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse2.getTotalCount()).isEqualTo(4);
        assertThat(dbAggregationResultDbListResponse2.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .doesNotContain("BLOCKED");

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse3 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.stage)
                        .calculation(JiraIssuesFilter.CALCULATION.state_transition_time)
                        .toState("DONE")
                        .fromState("TO DO")
                        .build(),
                true,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse3.getTotalCount()).isEqualTo(1);
        assertThat(dbAggregationResultDbListResponse3.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .contains("DONE");

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse4 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.stage)
                        .calculation(JiraIssuesFilter.CALCULATION.stage_times_report)
                        .toState("DONE")
                        .fromState("TO DO")
                        .build(),
                true,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse4.getTotalCount()).isEqualTo(7);
        assertThat(dbAggregationResultDbListResponse4.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("DONE", "DONE", "DONE", "DONE", "IN PROGRESS", "IN PROGRESS", "TO DO"));

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse5 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.stage)
                        .calculation(JiraIssuesFilter.CALCULATION.state_transition_time)
                        .toState("DONE")
                        .fromState("TO DO")
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse5.getTotalCount()).isEqualTo(1);
        assertThat(dbAggregationResultDbListResponse5.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("DONE"));

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse6 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.stage)
                        .calculation(JiraIssuesFilter.CALCULATION.response_time)
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse6.getTotalCount()).isEqualTo(4);
        assertThat(dbAggregationResultDbListResponse6.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("IN PROGRESS", "TO DO", "DONE", "_UNKNOWN_"));
    }

    @Test
    public void testSort() throws SQLException {
        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.stage)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.stage.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(4);
        Assertions.assertThat(dbAggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(List.of("_UNKNOWN_", "DONE", "IN PROGRESS", "TO DO"));

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse1 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.stage)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.stage.toString(), SortingOrder.DESC))
                        .build(),
                false,
                null, null, Map.of());
        List<String> result = dbAggregationResultDbListResponse1.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList());
        Collections.reverse(result);
        assertThat(dbAggregationResultDbListResponse1.getTotalCount()).isEqualTo(4);
        assertThat(result).containsExactlyInAnyOrderElementsOf(List.of("_UNKNOWN_", "DONE", "IN PROGRESS", "TO DO"));

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse11 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_20001")
                        .calculation(JiraIssuesFilter.CALCULATION.hops)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.hops.toString(), SortingOrder.DESC))
                        .build(),
                false,
                null, null, Map.of());
        List<Long> result4 = dbAggregationResultDbListResponse11.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList());
        Collections.reverse(result4);
        assertThat(dbAggregationResultDbListResponse11.getTotalCount()).isEqualTo(6);
        assertThat(result4).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse2 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.bounces.toString(), SortingOrder.DESC))
                        .build(),
                false,
                null, null, Map.of());
        List<Long> result1 = dbAggregationResultDbListResponse2.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList());
        Collections.reverse(result1);
        assertThat(dbAggregationResultDbListResponse2.getTotalCount()).isEqualTo(5);
        assertThat(result1).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse3 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.assignee.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse3.getTotalCount()).isEqualTo(5);
        assertThat(dbAggregationResultDbListResponse3.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse4 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.epic)
                        .calculation(JiraIssuesFilter.CALCULATION.response_time)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.response_time.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse4.getTotalCount()).isEqualTo(1);
        assertThat(dbAggregationResultDbListResponse4.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse5 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.resolution_time.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse5.getTotalCount()).isEqualTo(3);
        assertThat(dbAggregationResultDbListResponse5.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse6 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.issue_created)
                        .calculation(JiraIssuesFilter.CALCULATION.stage_times_report)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.stage_times_report.toString(), SortingOrder.DESC))
                        .build(),
                false,
                null, null, Map.of());
        List<Long> result2 = dbAggregationResultDbListResponse6.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList());
        Collections.reverse(result2);
        assertThat(dbAggregationResultDbListResponse6.getTotalCount()).isEqualTo(11);
        assertThat(result2).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse7 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.issue_resolved)
                        .calculation(JiraIssuesFilter.CALCULATION.story_points)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.story_points.toString(), SortingOrder.DESC))
                        .build(),
                false,
                null, null, Map.of());
        List<Long> result3 = dbAggregationResultDbListResponse7.getRecords().stream().map(DbAggregationResult::getTotalStoryPoints).collect(Collectors.toList());
        Collections.reverse(result3);
        assertThat(dbAggregationResultDbListResponse7.getTotalCount()).isEqualTo(3);
        assertThat(result3).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse8 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.issue_updated)
                        .calculation(JiraIssuesFilter.CALCULATION.priority)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.priority.toString(), SortingOrder.DESC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse8.getTotalCount()).isEqualTo(6);
        assertThat(dbAggregationResultDbListResponse8.getRecords().stream().map(DbAggregationResult::getPriorityOrder).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse9 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.component)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.bounces.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse9.getTotalCount()).isEqualTo(5);
        assertThat(dbAggregationResultDbListResponse9.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse10 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_10149")
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.bounces.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse10.getTotalCount()).isEqualTo(1);
        assertThat(dbAggregationResultDbListResponse10.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse12 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.first_assignee)
                        .calculation(JiraIssuesFilter.CALCULATION.age)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.age.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse12.getTotalCount()).isEqualTo(4);
        assertThat(dbAggregationResultDbListResponse12.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse13 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.sprint_mapping)
                        .calculation(JiraIssuesFilter.CALCULATION.sprint_mapping)
                        .sort(Map.of("start_date", SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse13.getTotalCount()).isEqualTo(0);
        assertThat(dbAggregationResultDbListResponse13.getRecords().stream().map(DbAggregationResult::getSprintId).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse14 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.ticket_count.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse14.getTotalCount()).isEqualTo(4);
        assertThat(dbAggregationResultDbListResponse14.getRecords().stream().map(DbAggregationResult::getTotalTickets).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse15 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.fix_version)
                        .calculation(JiraIssuesFilter.CALCULATION.assign_to_resolve)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.assign_to_resolve.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse15.getTotalCount()).isEqualTo(0);
        assertThat(dbAggregationResultDbListResponse15.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse16 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.status.toString(), SortingOrder.DESC))
                        .build(),
                false,
                null, null, Map.of());
        List<String> result5 = dbAggregationResultDbListResponse16.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList());
        Collections.reverse(result5);
        assertThat(dbAggregationResultDbListResponse16.getTotalCount()).isEqualTo(4);
        assertThat(result5).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse17 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .calculation(JiraIssuesFilter.CALCULATION.assignees)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.status.toString(), SortingOrder.DESC))
                        .build(),
                false,
                null, null, Map.of());
        List<String> result6 = dbAggregationResultDbListResponse17.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList());
        Collections.reverse(result6);
        assertThat(dbAggregationResultDbListResponse17.getTotalCount()).isEqualTo(3);
        assertThat(result6).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse18 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.label)
                        .fromState("TO DO")
                        .toState("DONE")
                        .calculation(JiraIssuesFilter.CALCULATION.state_transition_time)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.state_transition_time.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse18.getTotalCount()).isEqualTo(0);
        assertThat(dbAggregationResultDbListResponse18.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse19 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.priority)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.bounces.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse19.getTotalCount()).isEqualTo(3);
        assertThat(dbAggregationResultDbListResponse19.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse20 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.resolution)
                        .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.resolution_time.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse20.getTotalCount()).isEqualTo(3);
        assertThat(dbAggregationResultDbListResponse20.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse21 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.ticket_category)
                        .calculation(JiraIssuesFilter.CALCULATION.hops)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.hops.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse21.getTotalCount()).isEqualTo(1);
        assertThat(dbAggregationResultDbListResponse21.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse22 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.project)
                        .calculation(JiraIssuesFilter.CALCULATION.age)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.age.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse22.getTotalCount()).isEqualTo(3);
        assertThat(dbAggregationResultDbListResponse22.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse23 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.reporter)
                        .calculation(JiraIssuesFilter.CALCULATION.response_time)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.response_time.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse23.getTotalCount()).isEqualTo(5);
        assertThat(dbAggregationResultDbListResponse23.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse24 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.trend.toString(), SortingOrder.DESC))
                        .build(),
                false,
                null, null, Map.of());
        List<String> result7 = dbAggregationResultDbListResponse24.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList());
        Collections.reverse(result7);
        assertThat(dbAggregationResultDbListResponse24.getTotalCount()).isEqualTo(3);
        assertThat(result7).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse25 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .calculation(JiraIssuesFilter.CALCULATION.priority)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.priority.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        List<Integer> result8 = dbAggregationResultDbListResponse25.getRecords().stream().map(DbAggregationResult::getPriorityOrder).collect(Collectors.toList());
        Collections.reverse(result8);
        assertThat(dbAggregationResultDbListResponse25.getTotalCount()).isEqualTo(3);
        assertThat(result8).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse26 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_20001")
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.custom_field.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse26.getTotalCount()).isEqualTo(6);
        assertThat(dbAggregationResultDbListResponse26.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse27 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.none)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.none.toString(), SortingOrder.ASC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse27.getTotalCount()).isEqualTo(1);
        assertThat(dbAggregationResultDbListResponse27.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse28 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.none)
                        .calculation(JiraIssuesFilter.CALCULATION.hops)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.hops.toString(), SortingOrder.DESC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse28.getTotalCount()).isEqualTo(1);
        assertThat(dbAggregationResultDbListResponse28.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse29 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.sprint)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.sprint.toString(), SortingOrder.ASC))
                        .build(),
                true,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse29.getTotalCount()).isEqualTo(3);
        assertThat(dbAggregationResultDbListResponse29.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse30 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.sprint)
                        .calculation(JiraIssuesFilter.CALCULATION.age)
                        .sort(Map.of(JiraIssuesFilter.CALCULATION.age.toString(), SortingOrder.DESC))
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse30.getTotalCount()).isEqualTo(3);
        assertThat(dbAggregationResultDbListResponse30.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isSorted();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse31 = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.component)
                        .calculation(JiraIssuesFilter.CALCULATION.age)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.component.toString(), SortingOrder.DESC))
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.issue_type),
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse31.getTotalCount()).isEqualTo(5);
        assertThat(dbAggregationResultDbListResponse31.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("ui-levelops", "serverapi-levelops", "serverapi", "internalapi-levelops", "customer-requests"));

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse32 = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .calculation(JiraIssuesFilter.CALCULATION.age)
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.status.toString(), SortingOrder.DESC))
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.parent),
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse32.getTotalCount()).isEqualTo(4);
        assertThat(dbAggregationResultDbListResponse32.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("TO DO", "IN PROGRESS", "DONE", "BLOCKED"));
    }

    @Test
    public void testCalculationAge() throws SQLException {
        DbListResponse<DbAggregationResult> response = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter
                        .builder()
                        .calculation(JiraIssuesFilter.CALCULATION.age)
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .build(), false, null, null, Map.of());
        assertThat(response.getTotalCount()).isEqualTo(5);

        List<DbAggregationResult> records = response.getRecords().stream()
                .filter(record -> Objects.nonNull(record.getAdditionalKey()))
                .sorted(Comparator.comparing(DbAggregationResult::getAdditionalKey))
                .collect(Collectors.toList());

        assertThat(records.get(0).getAdditionalKey()).isEqualTo("Maxime Bellier");
        assertThat(records.get(0).getTotalStoryPoints()).isEqualTo(0L);
        assertThat(records.get(1).getAdditionalKey()).isEqualTo("Meghana");
        assertThat(records.get(1).getTotalStoryPoints()).isEqualTo(0L);
        assertThat(records.get(2).getAdditionalKey()).isEqualTo("Meghana Dwarakanath");
        assertThat(records.get(2).getTotalStoryPoints()).isEqualTo(243);
        assertThat(records.get(3).getAdditionalKey()).isEqualTo("_UNASSIGNED_");
        assertThat(records.get(3).getTotalStoryPoints()).isEqualTo(0);

        assertThat(records.get(0).getKey()).isEqualTo(userIdOf("Maxime Bellier").isPresent() ? userIdOf("Maxime Bellier").get() : "");
        assertThat(records.get(0).getTotalStoryPoints()).isEqualTo(0L);
        assertThat(records.get(1).getKey()).isEqualTo(userIdOf("Meghana").isPresent() ? userIdOf("Meghana").get() : "");
        assertThat(records.get(1).getTotalStoryPoints()).isEqualTo(0L);
        assertThat(records.get(2).getKey()).isEqualTo(userIdOf("Meghana Dwarakanath").isPresent() ? userIdOf("Meghana Dwarakanath").get() : "");
        assertThat(records.get(2).getTotalStoryPoints()).isEqualTo(243);
    }

    @Test
    public void testCalculateTicketReport() throws SQLException {
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter
                        .builder()
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .build(), false, null, null, Map.of()).getRecords().get(2).getTotalStoryPoints()).isEqualTo(243);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter
                        .builder()
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .build(), false, null, null, Map.of()).getRecords().get(2).getTotalStoryPoints()).isEqualTo(81);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter
                        .builder()
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .across(JiraIssuesFilter.DISTINCT.resolution)
                        .build(), false, null, null, Map.of()).getRecords().get(1).getTotalStoryPoints()).isEqualTo(243);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter
                        .builder()
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .across(JiraIssuesFilter.DISTINCT.priority)
                        .build(), false, null, null, Map.of()).getRecords().get(0).getMeanStoryPoints()).isEqualTo(5.282608695652174);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter
                        .builder()
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .build(), false, null, null, Map.of()).getRecords().get(0).getMeanStoryPoints()).isEqualTo(0.0);
    }
}
