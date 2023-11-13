package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraLink;
import io.levelops.commons.databases.models.database.jira.DbJiraProject;
import io.levelops.commons.databases.models.database.jira.DbJiraSalesforceCase;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraStoryPoints;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
import io.levelops.commons.databases.models.database.jira.JiraAssigneeTime;
import io.levelops.commons.databases.models.database.jira.JiraStatusTime;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraOrFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraUser;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@Log4j2
public class JiraIssueServiceTest {
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

    private Optional<String> userIdOf(String displayName) {
        return userIdentityService.getUserByDisplayName(company, "1", displayName);
    }

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null) {
            return;
        }

        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
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

        input = ResourceUtils.getResourceAsString("json/databases/jiraissues_duedate.json");
        PaginatedResponse<JiraIssue> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = new Date();
        jiraFieldService.batchUpsert(company,
                List.of(DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("array").fieldKey("customfield_10048").fieldItems("user").build(),
                        DbJiraField.builder().custom(true).name("project").integrationId("1").fieldType("array").fieldKey("customfield_13264").fieldItems("user").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("string").fieldKey("customfield_12641").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("number").fieldKey("customfield_10105").build(),
                        DbJiraField.builder().custom(true).name("hello").integrationId("1").fieldType("string").fieldKey("customfield_20001").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("string").fieldKey("customfield_12746").build()));
        List<IntegrationConfig.ConfigEntry> entries = List.of(
                IntegrationConfig.ConfigEntry.builder().key("customfield_12641").name("something").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_12746").name("something 1").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_10048").name("USER ARRAY").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_10149").name("USER SINGLE").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_20001").name("hello").delimiter(",").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_12716").name("something 2").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_10020").name("sprint").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_10105").name("sp").build());
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
                        tmp.getIngestedAt() - 86400L).isPresent()) {
                    throw new RuntimeException("This issue shouldnt exist.");
                }
                DbJiraIssue issue3 = JiraIssueParser.parseJiraIssue(issue, "1",
                        DateUtils.addSeconds(currentTime, -86400),
                        JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build());
                issue3 = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, issue3);
                jiraIssueService.insert(company, issue3);
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isPresent()) {
                    throw new RuntimeException("This issue shouldnt exist.");
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
//       Commenting this code as it's creating flaky test result.
//       test() method was already commented where this data was being referenced.
//        jiraFieldService.batchUpsert(company, List.of(Objects.requireNonNull(DbJiraField.fromJiraField(
//                JiraField.builder()
//                        .key("customfield_12345")
//                        .custom(true)
//                        .name("test_field")
//                        .schema(JiraField.Schema.builder()
//                                .type("array")
//                                .items("string")
//                                .build())
//                        .build(), "1"))));
//        randomIssue.toBuilder().key("PROP-1221").build();
//        jiraIssueService.insert(company, randomIssue.toBuilder()
//                .customFields(Map.of("customfield_12345", List.of("1.0", "2.0", "3.0")))
//                .build());
        jiraIssueService.bulkUpdateEpicStoryPoints(company, "1", DateUtils.truncate(currentTime, Calendar.DATE).toInstant().getEpochSecond());
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

        DatabaseTestUtils.setupStatusMetadata(company, "1", jiraTestDbs.getJiraStatusMetadataDatabaseService());
    }

    @Test
    public void customFieldAcrossStackTest() throws SQLException {
        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_10020")
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .build(),
                null, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(2);
        assertThat(dbAggregationResultDbListResponse.getRecords()
                .stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).containsExactlyInAnyOrder("[\"May 11\"]", "[\"May \\n11\"]");

        dbAggregationResultDbListResponse = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                        .customAcross("customfield_10105")
                        .integrationIds(List.of("1"))
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.custom_field.toString(), SortingOrder.ASC))
                        .ingestedAt(ingestedAt)
                        .build(),
                null, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(4);
        assertThat(dbAggregationResultDbListResponse.getRecords()
                .stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).containsExactly("0.9", "1.5", "2.0", "2.5");

        dbAggregationResultDbListResponse = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                        .customAcross("customfield_10105")
                        .integrationIds(List.of("1"))
                        .sort(Map.of(JiraIssuesFilter.DISTINCT.custom_field.toString(), SortingOrder.DESC))
                        .ingestedAt(ingestedAt)
                        .build(),
                null, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(4);
        assertThat(dbAggregationResultDbListResponse.getRecords()
                .stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).containsExactly("2.5", "2.0", "1.5", "0.9");

        dbAggregationResultDbListResponse = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_10149")
                        .customFields(Map.of("customfield_10048", List.of("maxime@levelops.io")))
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.priority), null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(dbAggregationResultDbListResponse.getRecords()
                .stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).containsExactlyInAnyOrder("Harsh Jariwala");
        assertThat(dbAggregationResultDbListResponse.getRecords()
                .stream().findFirst().orElseThrow().getStacks()
                .stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("HIGH");

        dbAggregationResultDbListResponse = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_10149")
                        .customFields(Map.of("customfield_10048", List.of("maxime@levelops.io"), "customfield_20001", List.of("Magic2")))
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.status), null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(dbAggregationResultDbListResponse.getRecords()
                .stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).containsExactlyInAnyOrder("Harsh Jariwala");
        assertThat(dbAggregationResultDbListResponse.getRecords()
                .stream().findFirst().orElseThrow().getStacks()
                .stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("DONE");
    }

    @Test
    @Ignore
    public void testGroupByCalculateAndStackGroupByWithIssueDue() throws SQLException {
        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.issue_due)
                        .build(),
                false, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getRecords().size()).isEqualTo(2);

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse1 = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .issueCreatedRange(ImmutablePair.of(1624352100L, 1624352599L))
                        .across(JiraIssuesFilter.DISTINCT.issue_due)
                        .aggInterval("week")
                        .build(),
                false, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse1.getRecords().size()).isEqualTo(1);

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse2 = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .issueCreatedRange(ImmutablePair.of(1624352100L, 1624352599L))
                        .across(JiraIssuesFilter.DISTINCT.issue_due)
                        .aggInterval("month")
                        .build(),
                false, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse2.getRecords().size()).isEqualTo(1);

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse3 = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .issueCreatedRange(ImmutablePair.of(1624352100L, 1624352599L))
                        .across(JiraIssuesFilter.DISTINCT.issue_due)
                        .aggInterval("year")
                        .build(),
                false, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse3.getRecords().size()).isEqualTo(1);

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse4 = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .issueCreatedRange(ImmutablePair.of(1624352100L, 1624352599L))
                        .across(JiraIssuesFilter.DISTINCT.issue_due)
                        .aggInterval("quarter")
                        .build(),
                false, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse4.getRecords().size()).isEqualTo(1);

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse5 = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .issueDueRange(ImmutablePair.of(1625145387L, 1627650987L))
                        .across(JiraIssuesFilter.DISTINCT.issue_due)
                        .aggInterval("day")
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.issue_due), null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse5.getRecords().size()).isEqualTo(1);
        Assert.assertEquals("1626805800.000000", dbAggregationResultDbListResponse5.getRecords().get(0).getKey());

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse6 = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.issue_due)
                        .aggInterval("week")
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.issue_due), null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse6.getRecords().size()).isEqualTo(2);
        Assert.assertEquals("1626633000.000000", dbAggregationResultDbListResponse6.getRecords().get(0).getKey());

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse7 = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.issue_due)
                        .aggInterval("month")
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.issue_due), null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse7.getRecords().size()).isEqualTo(2);
        Assert.assertEquals("1625077800.000000", dbAggregationResultDbListResponse7.getRecords().get(0).getKey());

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse8 = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.issue_due)
                        .aggInterval("year")
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.issue_due), null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse8.getRecords().size()).isEqualTo(2);
        Assert.assertEquals("1609439400.000000", dbAggregationResultDbListResponse8.getRecords().get(0).getKey());

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse9 = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.issue_due)
                        .aggInterval("quarter")
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.issue_due), null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse9.getRecords().size()).isEqualTo(2);
        Assert.assertEquals("1625077800.000000", dbAggregationResultDbListResponse9.getRecords().get(0).getKey());
    }

    @Test
    @Ignore
    public void testGroupByCalculateAndStackGroupByWithIssueDueRelative() throws SQLException {
        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .issueCreatedRange(ImmutablePair.of(1624352100L, 1624352599L))
                        .across(JiraIssuesFilter.DISTINCT.issue_due_relative)
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.issue_due_relative), null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getRecords().size()).isEqualTo(1);
        Assert.assertEquals("1626805800.000000", dbAggregationResultDbListResponse.getRecords().get(0).getKey());

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse1 = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .issueDueRange(ImmutablePair.of(1625145387L, 1627650987L))
                        .aggInterval("week")
                        .across(JiraIssuesFilter.DISTINCT.issue_due_relative)
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.issue_due_relative), null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse1.getRecords().size()).isEqualTo(1);
        Assert.assertEquals("1626633000.000000", dbAggregationResultDbListResponse1.getRecords().get(0).getKey());

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse2 = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .issueDueRange(ImmutablePair.of(1625145387L, 1627650987L))
                        .aggInterval("month")
                        .across(JiraIssuesFilter.DISTINCT.issue_due_relative)
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.issue_due_relative), null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse2.getRecords().size()).isEqualTo(1);
        Assert.assertEquals("1625077800.000000", dbAggregationResultDbListResponse2.getRecords().get(0).getKey());

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse3 = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .issueDueRange(ImmutablePair.of(1625145387L, 1627650987L))
                        .aggInterval("year")
                        .across(JiraIssuesFilter.DISTINCT.issue_due_relative)
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.issue_due_relative), null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse3.getRecords().size()).isEqualTo(1);
        Assert.assertEquals("1609439400.000000", dbAggregationResultDbListResponse3.getRecords().get(0).getKey());

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse4 = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .issueDueRange(ImmutablePair.of(1625145387L, 1627650987L))
                        .aggInterval("quarter")
                        .across(JiraIssuesFilter.DISTINCT.issue_due_relative)
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.issue_due_relative), null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse4.getRecords().size()).isEqualTo(1);
        Assert.assertEquals("1625077800.000000", dbAggregationResultDbListResponse4.getRecords().get(0).getKey());

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse5 = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.issue_due_relative)
                        .aggInterval("day")
                        .build(),
                false, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse5.getRecords().size()).isEqualTo(2);

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse6 = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.issue_due_relative)
                        .aggInterval("week")
                        .build(),
                false, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse6.getRecords().size()).isEqualTo(2);

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse7 = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.issue_due_relative)
                        .aggInterval("month")
                        .build(),
                false, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse7.getRecords().size()).isEqualTo(2);

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse8 = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.issue_due_relative)
                        .aggInterval("year")
                        .build(),
                false, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse8.getRecords().size()).isEqualTo(2);

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse9 = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.issue_due_relative)
                        .aggInterval("quarter")
                        .build(),
                false, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse9.getRecords().size()).isEqualTo(2);
    }

    @Test
    public void testFieldSizeValidation() throws SQLException {
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .fieldSize(Map.of("summary", Map.of("$lt", "30", "$gt", "20")))
                        .missingFields(Map.of("customfield_12716", true,
                                "priority", false, "project", false))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getRecords()).hasSize(7);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                        .issueDueRange(ImmutablePair.of(1625145387L, 1627650987L))
                        .build(),
                null, Map.of(), 0, 10000).getRecords()).hasSize(3);

        assertThrows(NumberFormatException.class, () -> {
            jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                            .ingestedAt(ingestedAt)
                            .fieldSize(Map.of("summary", Map.of("$lt", "test")))
                            .missingFields(Map.of("customfield_12716", true,
                                    "priority", false, "project", false))
                            .build(),
                    null, Map.of("num_attachments", SortingOrder.ASC),
                    0, 10000);
        });
    }

    @Test
    public void testPartialMatchCondition() throws SQLException {
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("priority", Map.of("$begins", "HIG")))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(10);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("priority", Map.of("$begins", "HIG' OR priority is not NULL OR priority = '")))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(0);
    }

    @Test
    public void testOrConditions() throws SQLException {
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("priority", Map.of("$begins", "HIG")))
                                .excludeStages(List.of("DONE"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(9);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("priority", Map.of("$begins", "HIG")))
                                .excludeStatuses(List.of("DONE"))
                                .orFilter(JiraOrFilter.builder().statuses(List.of("DONE")).build())
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("priority", Map.of("$begins", "HIG")))
                                .orFilter(JiraOrFilter.builder().statuses(List.of("DONE")).build())
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(7);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("priority", Map.of("$begins", "HIG")))
                                .orFilter(JiraOrFilter.builder()
                                        .statuses(List.of("DONE"))
                                        .assignees(List.of(userIdOf("Maxime Bellier").isPresent() ? userIdOf("Maxime Bellier").get() : ""))
                                        .build())
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(8);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .orFilter(JiraOrFilter.builder()
                                        .statuses(List.of("DONE"))
                                        .assignees(List.of(userIdOf("Maxime Bellier").isPresent() ? userIdOf("Maxime Bellier").get() : ""))
                                        .partialMatch(Map.of("priority", Map.of("$begins", "HIG"),
                                                "customfield_12641", Map.of("$contains", "Not")))
                                        .build())
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(13);
    }

    @Test
    public void testPartialMatchArrayCondition() throws SQLException {
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("customfield_12641", Map.of("$contains", "Not")))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("customfield_12641", Map.of("$contains", "%' or true AND 'dummy' = '")))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(0);
    }

    //     @Test
    public void test() throws SQLException, JsonProcessingException {
        DbListResponse<DbAggregationResult> actual = jiraIssueService.stackedGroupBy(company, JiraIssuesFilter.builder()
                .across(JiraIssuesFilter.DISTINCT.priority)
                .ingestedAt(ingestedAt)
                .build(), List.of(JiraIssuesFilter.DISTINCT.status), null, null, Map.of());
        assertThat(actual).isNotNull();
        DbListResponse<DbAggregationResult> expected = DbListResponse.of(List.of(
                DbAggregationResult.builder()
                        .key("MEDIUM").totalTickets(483L).stacks(List.of(
                                DbAggregationResult.builder().key("DONE").totalTickets(357L).build(),
                                DbAggregationResult.builder().key("TO DO").totalTickets(90L).build(),
                                DbAggregationResult.builder().key("IN PROGRESS").totalTickets(27L).build(),
                                DbAggregationResult.builder().key("WONT DO").totalTickets(8L).build(),
                                DbAggregationResult.builder().key("BACKLOG").totalTickets(1L).build()
                        )).build(),
                DbAggregationResult.builder()
                        .key("HIGH").totalTickets(12L).stacks(List.of(
                                DbAggregationResult.builder().key("DONE").totalTickets(9L).build(),
                                DbAggregationResult.builder().key("TO DO").totalTickets(2L).build(),
                                DbAggregationResult.builder().key("IN PROGRESS").totalTickets(1L).build()
                        )).build(),
                DbAggregationResult.builder()
                        .key("HIGHEST").totalTickets(6L).stacks(List.of(
                                DbAggregationResult.builder().key("DONE").totalTickets(5L).build(),
                                DbAggregationResult.builder().key("TO DO").totalTickets(1L).build()
                        )).build()
        ), 3);
        // assertThat(actual).isEqualTo(expected);
        assertThat(actual.getRecords()).containsExactlyInAnyOrderElementsOf(expected.getRecords());

        JiraIssuesFilter a = JiraIssuesFilter.builder()
                .hygieneCriteriaSpecs(Map.of())
                .partialMatch(Map.of("priority", Map.of("$begins", "HIG")))
                .ingestedAt(ingestedAt)
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
                .build();
        log.info("a = " + a.generateCacheRawString());
        log.info("b = " + b.generateCacheRawString());
        assertThat(a.generateCacheHash()).isEqualTo(a.generateCacheHash());
        assertThat(b.generateCacheHash()).isEqualTo(b.generateCacheHash());
        assertThat(a.generateCacheHash()).isNotEqualTo(b.generateCacheHash());
        DbListResponse<DbJiraIssue> list = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000);
        assertThat(list.getRecords().size()).isEqualTo(501);

        list = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .parentKeys(List.of("LEV-652", "LEV-724", "LEV-902"))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000);
        assertThat(list.getRecords().size()).isEqualTo(26);

        list = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .excludeParentKeys(List.of("LEV-652", "LEV-724", "LEV-902"))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000);
        assertThat(list.getRecords().size()).isEqualTo(10);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("priority", Map.of("$begins", "HIG")))
                                .excludeStages(List.of("DONE"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(18);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("priority", Map.of("$begins", "SHY")))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("status", Map.of("$begins", "DO")))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(371);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("status", Map.of("$ends", "DO")))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(101);
        JiraAssigneeTime jat = jiraIssueService.listIssueAssigneesByTime(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
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
                        .build(),
                null,
                0,
                10000);
        Assert.assertEquals(252, dbListResponse.getRecords().size());

        dbListResponse = jiraIssueService.listIssueAssigneesByTime(company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("assignee", false))
                        .build(),
                null,
                0,
                10000);
        Assert.assertEquals(756, dbListResponse.getRecords().size());

        dbListResponse = jiraIssueService.listIssueAssigneesByTime(company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("assignee", false))
                        .excludeTimeAssignees(List.of("_UNASSIGNED_"))
                        .build(),
                null,
                0,
                10000);
        Assert.assertEquals(504, dbListResponse.getRecords().size());

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
        Assert.assertEquals(407, dbListResponse2.getRecords().size());

        dbListResponse2 = jiraIssueService.listIssueStatusesByTime(company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .excludeStatuses(List.of("TO DO"))
                        .build(),
                null,
                0,
                10000);
        Assert.assertEquals(1017, dbListResponse2.getRecords().size());

        integrationService.delete(company, "2");
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .issueUpdatedRange(ImmutablePair.of(1584469718L, 1584469720L))
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("summary", Map.of("$begins", "Fix")))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(7);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("components", Map.of("$contains", "serverapi")))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(62);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("summary", Map.of("$contains", "single")))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(2);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .partialMatch(Map.of("customfield_12641", Map.of("$contains", "Not"),
                                        "summary", Map.of("$contains", "template")))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .issueUpdatedRange(ImmutablePair.of(1584469718L, 1584469720L))
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
                                .build(),
                        true,
                        null, null, Map.of())
                .getTotalCount())
                .isEqualTo(1);

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .across(JiraIssuesFilter.DISTINCT.parent)
                        .ingestedAt(ingestedAt)
                        .build(),
                false,
                null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getRecords().size()).isEqualTo(10);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("fix_version", true))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getTotalCount()).isEqualTo(496);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_10048")
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(2);

        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_10149")
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(1);
        assertThat(jiraIssueService.get(
                company,
                "LEV-647",
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
                                .build(),
                        null,
                        0,
                        10000)
                .getTotalCount()).isEqualTo(829);
        assertThat(jiraIssueService.listIssueStatusesByTime(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .build(),
                        null,
                        0,
                        10000)
                .getTotalCount()).isEqualTo(1116);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.trend)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .build(),
                        true, null, null, Map.of())
                .getRecords().size()).isEqualTo(3);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1")).keys(List.of("LEV-523"))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getRecords().get(0).getAssigneeList().size()).isEqualTo(4);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("customfield_12716", false))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getRecords()).hasSize(3);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("customfield_12716", true,
                                "priority", false, "project", false))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getRecords()).hasSize(497);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .fieldSize(Map.of("summary", Map.of("$lt", "30", "$gt", "20"), "assignee", Map.of("$lt", "12")))
                        .missingFields(Map.of("customfield_12716", true,
                                "priority", false, "project", false))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getRecords()).hasSize(9);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("customfield_12716", true, "customfield_0000", true,
                                "priority", false, "project", false))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getRecords()).hasSize(500);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("customfield_12716", true, "project", true))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getRecords()).hasSize(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.inactive_assignees))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getTotalCount()).isEqualTo(57);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.inactive_assignees,
                                JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getTotalCount()).isEqualTo(54);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1")).keys(List.of("LEV-647")).build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getRecords().get(0).getAssigneeList().size()).isEqualTo(29);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .customFields(Map.of(
                                "customfield_12641", List.of("Required"),
                                "customfield_12716", List.of("0.0"),
                                "customfield_12746", List.of("what this")
                        )).build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getTotalCount()).isEqualTo(2);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .fieldSize(Map.of("customfield_12641", Map.of("$gt", "10"), "customfield_12746", Map.of("$lt", "10")))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getTotalCount()).isEqualTo(1);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .fieldSize(Map.of("customfield_12641", Map.of("$gt", "25")))
                        .build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getTotalCount()).isEqualTo(0);

        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.epic)
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(20);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.priority)
                        .calculation(JiraIssuesFilter.CALCULATION.assign_to_resolve)
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_12345")
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
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .customFields(Map.of("customfield_12746", List.of("what this")))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_12641")
                        .build(),
                true, null, null, Map.of()).getTotalCount()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.epic)
                        .build(),
                false, null, null, Map.of()).getTotalCount()).isEqualTo(20);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.priority)
                        .calculation(JiraIssuesFilter.CALCULATION.assign_to_resolve)
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
                        .build(),
                false, null, null, Map.of()).getTotalCount()).isEqualTo(1);
        assertThat(jiraIssueService.stackedGroupBy(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .integrationIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.custom_field)
                                .customStacks(List.of("customfield_12641"))
                                .customAcross("customfield_12746")
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
                        .build(),
                false, null, null, Map.of()).getTotalCount()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .customFields(Map.of("customfield_12746", List.of("what this")))
                        .across(JiraIssuesFilter.DISTINCT.custom_field)
                        .customAcross("customfield_12641")
                        .build(),
                false, null, null, Map.of()).getTotalCount()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .integrationIds(List.of("1"))
                                .customFields(Map.of("customfield_12641", List.of("Required", "Not Required")))
                                .across(JiraIssuesFilter.DISTINCT.custom_field)
                                .customAcross("customfield_12641")
                                .build(),
                        false, null, null, Map.of())
                .getRecords().get(0).getTotalTickets())
                .isEqualTo(2);

        //test exclusions
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeProjects(List.of("LEV"))
                .build(), null, Map.of(), 0, 100).getTotalCount()).isEqualTo(9);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludePriorities(List.of("MEDIUM"))
                .build(), null, Map.of(), 0, 100).getTotalCount()).isEqualTo(18);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeCustomFields(Map.of("customfield_12641", List.of("Required")))
                .build(), null, Map.of(), 0, 100).getTotalCount()).isEqualTo(498);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeCustomFields(Map.of("customfield_12641", List.of("Not Required")))
                .build(), null, Map.of(), 0, 100).getTotalCount()).isEqualTo(499);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeAssignees(List.of(userIdOf("Harsh Jariwala").isPresent() ? userIdOf("Harsh Jariwala").get() : ""))
                .build(), null, Map.of(), 0, 100).getTotalCount()).isEqualTo(464);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeIssueTypes(List.of("TASK", "STORY"))
                .build(), null, Map.of(), 0, 100).getTotalCount()).isEqualTo(173);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeFixVersions(List.of("123"))
                .build(), null, Map.of(), 0, 100).getTotalCount()).isEqualTo(498);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeVersions(List.of("456"))
                .build(), null, Map.of(), 0, 100).getTotalCount()).isEqualTo(498);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .versions(List.of("456"))
                .build(), null, Map.of(), 0, 100).getTotalCount()).isEqualTo(2);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeComponents(List.of("internalapi-levelops", "commons-levelops"))
                .build(), null, Map.of(), 0, 100).getTotalCount()).isEqualTo(480);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeLabels(List.of("production", "plugins"))
                .build(), null, Map.of(), 0, 100).getTotalCount()).isEqualTo(495);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .excludeEpics(List.of("LEV-671", "LEV-26", "LEV-358"))
                .build(), null, Map.of(), 0, 100).getTotalCount()).isEqualTo(260);
        //test sorting
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt).integrationIds(List.of("1")).build(),
                        null, Map.of("hops", SortingOrder.DESC), 0, 100)
                .getRecords().get(0).getHops()).isEqualTo(29);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt).integrationIds(List.of("1")).build(),
                        null, Map.of("bounces", SortingOrder.DESC), 0, 100)
                .getRecords().get(0).getBounces()).isEqualTo(24);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt).integrationIds(List.of("1")).build(),
                        null, Map.of("num_attachments", SortingOrder.DESC), 0, 100)
                .getRecords().get(0).getNumAttachments()).isEqualTo(3);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt).integrationIds(List.of("1")).build(),
                        null, Map.of("hops", SortingOrder.ASC), 0, 100)
                .getRecords().get(0).getHops()).isEqualTo(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt).integrationIds(List.of("1")).build(),
                        null, Map.of("bounces", SortingOrder.ASC), 0, 100)
                .getRecords().get(0).getBounces()).isEqualTo(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt).integrationIds(List.of("1")).build(),
                null, Map.of("num_attachments", SortingOrder.ASC),
                0, 100).getRecords().get(0).getNumAttachments()).isEqualTo(0);
        //test deletes
        jiraIssueService.cleanUpOldData(
                company, currentTime.toInstant().getEpochSecond(), 86400 * 2L);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt).keys(List.of("TESTING-1123231")).build(),
                        null, Map.of("issue_resolved_at", SortingOrder.DESC), 0, 100)
                .getTotalCount()).isEqualTo(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.poor_description,
                                JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time))
                        .build(), null, Map.of("issue_resolved_at", SortingOrder.DESC),
                0, 100).getTotalCount()).isEqualTo(209);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.poor_description,
                                JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time))
                        .build(),
                null, Map.of("issue_resolved_at", SortingOrder.DESC),
                0, 100).getTotalCount()).isEqualTo(255);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .issueTypes(List.of("EPIC"))
                                .integrationIds(List.of("1"))
                                .build(),
                        null, Map.of("issue_resolved_at", SortingOrder.DESC),
                        0, 100)
                .getTotalCount()).isEqualTo(21);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .priorities(List.of("HIGHEST"))
                        .integrationIds(List.of("1"))
                        .build(),
                null, Map.of("issue_created_at", SortingOrder.DESC),
                0, 100).getTotalCount()).isEqualTo(6);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                        .integrationIds(List.of("1"))
                        .build(),
                null, Map.of("issue_updated_at", SortingOrder.DESC),
                0, 100).getTotalCount()).isEqualTo(500);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                        .integrationIds(List.of("1"))
                        .build(),
                null, Map.of("hops", SortingOrder.DESC),
                0, 100).getTotalCount()).isEqualTo(500);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                        .integrationIds(List.of("1"))
                        .projects(List.of("TS"))
                        .build(),
                null, Map.of("bounces", SortingOrder.DESC),
                0, 100).getTotalCount()).isEqualTo(1);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .integrationIds(List.of("1"))
                                .components(List.of("internalapi-levelops", "serverapi-levelops")).build(),
                        null, Map.of("desc_size", SortingOrder.DESC), 0, 100)
                .getTotalCount()).isEqualTo(74);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .integrationIds(List.of("1"))
                                .components(List.of("internalapi-levelops"))
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
                        .build(),
                null, Map.of(), 0, 100).getTotalCount()).isEqualTo(6);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .statuses(List.of("DONE"))
                                .assignees(List.of(userIdOf("Maxime Bellier").isPresent() ? userIdOf("Maxime Bellier").get() : ""))
                                .integrationIds(List.of("1"))
                                .components(List.of("serverapi-levelops"))
                                .summary("the dmg")
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
                                .build(),
                        null, Map.of("issue_resolved_at", SortingOrder.DESC), 0, 100)
                .getTotalCount()).isEqualTo(1);
        // test state transit time
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .integrationIds(List.of("1"))
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
                                .build(),
                        null, Map.of(), 0, 100)
                .getTotalCount()).isEqualTo(14);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .fromState("TO DO")
                                .toState("DONE")
                                .integrationIds(List.of("1"))
                                .build(),
                        null, Map.of(), 0, 100)
                .getTotalCount()).isEqualTo(374);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .fromState("TO DO")
                                .toState("DONE")
                                .integrationIds(List.of("1"))
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
                                .build(),
                        null, Map.of(), 0, 100)
                .getTotalCount()).isEqualTo(0);

        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .fromState("TO DO")
                        .toState("DONE")
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .build(), false, null, null, Map.of())
                .getTotalCount()).isEqualTo(13);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .fromState("TO DO")
                        .toState("DONE")
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .build(), false, null, null, Map.of())
                .getTotalCount()).isEqualTo(4);

        // test first assignee filter and across
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .firstAssignees(List.of(userIdOf("Meghana Dwarakanath").isPresent() ? userIdOf("Meghana Dwarakanath").get() : ""))
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .build(), null, Map.of(), 0, 1000).getTotalCount()).isEqualTo(149);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .firstAssignees(List.of(userIdOf("Harsh Jariwala").isPresent() ? userIdOf("Harsh Jariwala").get() : ""))
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .build(), null, Map.of(), 0, 1000).getTotalCount()).isEqualTo(43);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .build(), null, Map.of(), 0, 1000).getTotalCount()).isEqualTo(500);

        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .firstAssignees(List.of(userIdOf("Meghana Dwarakanath").isPresent() ? userIdOf("Meghana Dwarakanath").get() : ""))
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.first_assignee)
                .build(), false, null, null, Map.of()).getRecords().get(0).getTotalTickets()).isEqualTo(149);
        List<DbAggregationResult> aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.first_assignee)
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
                .build(), null, Map.of(), 0, 1000).getTotalCount()).isEqualTo(149);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .firstAssignees(List.of(userIdOf("Harsh Jariwala").isPresent() ? userIdOf("Harsh Jariwala").get() : ""))
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .build(), null, Map.of(), 0, 1000).getTotalCount()).isEqualTo(43);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .build(), null, Map.of(), 0, 1000).getTotalCount()).isEqualTo(500);

        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .firstAssignees(List.of(userIdOf("Meghana Dwarakanath").isPresent() ? userIdOf("Meghana Dwarakanath").get() : ""))
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.first_assignee)
                .build(), false, null, null, Map.of()).getRecords().get(0).getTotalTickets()).isEqualTo(149);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.first_assignee)
                .build(), false, null, null, Map.of()).getRecords();
        assertThat(aggs.stream()
                .map(DbAggregationResult::getTotalTickets)
                .min(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(1);
        assertThat(aggs.stream()
                .map(DbAggregationResult::getTotalTickets)
                .max(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(149);

        testPrioritySla();
        testDistinct();
        testGroupByMinMedMax();
        testHygieneCalculations();
        testConfigFilter();
        testAgeReports();
    }

    @Test
    public void testJiraSprints() throws SQLException {
        DbJiraSprint sprint1 = DbJiraSprint.builder()
                .name("name")
                .integrationId(1)
                .sprintId(4)
                .state("Done")
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build();
        DbJiraSprint sprint2 = DbJiraSprint.builder()
                .name("name2")
                .integrationId(1)
                .sprintId(5)
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build();
        String id1 = jiraIssueService.insertJiraSprint(company, sprint1).orElseThrow();
        String id2 = jiraIssueService.insertJiraSprint(company, sprint2).orElseThrow();
        assertThat(jiraIssueService.getSprint(company, 1, 4).orElseThrow().getId()).isEqualTo(id1);
        assertThat(jiraIssueService.getSprint(company, 1, 5).orElseThrow().getId()).isEqualTo(id2);
        assertThat(jiraIssueService.streamSprints(company, JiraSprintFilter.builder()
                .integrationIds(List.of("1"))
                .build())).hasSize(5);
        assertThat(jiraIssueService.streamSprints(company, JiraSprintFilter.builder()
                .integrationIds(List.of("1", "2"))
                .build())).hasSize(5);
        String id = jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder()
                .name("name_change")
                .integrationId(1)
                .sprintId(5)
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build()).orElseThrow();
        assertThat(id).isEqualTo(id2);
        List<DbJiraSprint> dbSprints = jiraIssueService.streamSprints(company, JiraSprintFilter.builder()
                .integrationIds(List.of("1"))
                .build()).collect(Collectors.toList());
        assertThat(dbSprints.size()).isEqualTo(5);
        assertThat(dbSprints.get(4).getName()).isEqualTo("name_change");

        assertThat(jiraIssueService.streamSprints(company, JiraSprintFilter.builder()
                .state("dOnE")
                .build())).hasSize(1);


    }

    public void testAgeReports() throws SQLException {
        List<DbAggregationResult> ageRecords = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .hygieneCriteriaSpecs(Map.of())
                        .age(ImmutablePair.of(295L, 424L))
                        .calculation(JiraIssuesFilter.CALCULATION.age)
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .aggInterval("day")
                        .ingestedAt(ingestedAt)
                        .build(),
                false,
                null, null, Map.of()).getRecords();
        if (ageRecords.size() != 0) {
            assertThat(ageRecords.get(0).getMin()).isGreaterThan(295);
            assertThat(ageRecords.get(0).getMax()).isLessThan(424);
        }
    }

    public void testDistinct() throws SQLException {
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .across(JiraIssuesFilter.DISTINCT.priority)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .across(JiraIssuesFilter.DISTINCT.issue_created)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .build(),
                        false, null, null, Map.of())
                .getCount()).isEqualTo(93);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .across(JiraIssuesFilter.DISTINCT.issue_created)
                                .acrossLimit(90)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .build(),
                        false, null, null, Map.of())
                .getCount()).isEqualTo(90);
        // Temporarily commented out until timezone issues in unit tests are resolved.
//        assertThat(jiraIssueService.groupByAndCalculate(company,
//                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
//                        .ingestedAt(ingestedAt)
//                        .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
//                        .across(JiraIssuesFilter.DISTINCT.issue_created)
//                        .integrationIds(List.of("1"))
//                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
//                        .build(),
//                true)
//                .getCount()).isEqualTo(99); //should be 93 but we cap it to 90 data points in agg
//        assertThat(jiraIssueService.groupByAndCalculate(company,
//                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
//                        .ingestedAt(ingestedAt)
//                        .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
//                        .across(JiraIssuesFilter.DISTINCT.issue_updated)
//                        .integrationIds(List.of("1"))
//                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
//                        .build(),
//                true)
//                .getCount()).isEqualTo(77);
//        assertThat(jiraIssueService.groupByAndCalculate(company,
//                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
//                        .ingestedAt(ingestedAt)
//                        .across(JiraIssuesFilter.DISTINCT.issue_updated)
//                        .integrationIds(List.of("1"))
//                        .calculation(JiraIssuesFilter.CALCULATION.hops)
//                        .build(),
//                true)
//                .getCount()).isEqualTo(77);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "LOWEST"))
                                .across(JiraIssuesFilter.DISTINCT.issue_updated)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.bounces)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(6);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .priorities(List.of("HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.priority)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .across(JiraIssuesFilter.DISTINCT.assignee)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(13);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .across(JiraIssuesFilter.DISTINCT.assignee)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .components(List.of("internalapi-levelops", "serverapi-levelops"))
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(6);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .integrationIds(List.of("1"))
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(),
                true, null, null, Map.of()).getCount()).isEqualTo(5);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .integrationIds(List.of("1"))
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(),
                true, null, null, Map.of()).getCount()).isEqualTo(7);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.label)
                        .integrationIds(List.of("1"))
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(),
                true, null, null, Map.of()).getCount()).isEqualTo(9);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.version)
                        .integrationIds(List.of("1"))
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(),
                true, null, null, Map.of()).getCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.fix_version)
                        .integrationIds(List.of("1"))
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(),
                true, null, null, Map.of()).getCount()).isEqualTo(4);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.component)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(7);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.component)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .components(List.of("internalapi-levelops", "serverapi-levelops"))
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(6);
    }

    public void testGroupByMinMedMax() throws SQLException {
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .across(JiraIssuesFilter.DISTINCT.priority)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.bounces)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .across(JiraIssuesFilter.DISTINCT.priority)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.hops)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .across(JiraIssuesFilter.DISTINCT.assignee)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.hops)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(13);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .across(JiraIssuesFilter.DISTINCT.status)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.bounces)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(5);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.issue_type)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(7);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.response_time)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.component)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.hops)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(7);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.component)
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.hops)
                                .components(List.of("internalapi-levelops", "serverapi-levelops"))
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(6);
    }

    public void testHygieneCalculations() throws SQLException {
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.priority)
                                .integrationIds(List.of("1"))
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.idle))
                                .calculation(JiraIssuesFilter.CALCULATION.bounces)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of(JiraIssuesFilter.EXTRA_CRITERIA.idle, "100000"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.priority)
                                .integrationIds(List.of("1"))
                                .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                                .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.idle))
                                .calculation(JiraIssuesFilter.CALCULATION.bounces)
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(0);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .integrationIds(List.of("1"))
                        .priorities(List.of("HIGHEST", "HIGH", "LOW", "MEDIUM", "LOWEST"))
                        .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.no_assignee))
                        .calculation(JiraIssuesFilter.CALCULATION.hops)
                        .build(),
                true, null, null, Map.of()).getCount()).isEqualTo(1);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.priority)
                                .integrationIds(List.of("1"))
                                .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.no_components))
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.issue_type)
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .integrationIds(List.of("1"))
                                .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.no_due_date))
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(7);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.response_time)
                                .integrationIds(List.of("1"))
                                .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.poor_description))
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of(JiraIssuesFilter.EXTRA_CRITERIA.poor_description, "-1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.response_time)
                                .integrationIds(List.of("1"))
                                .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.poor_description))
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(0);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.response_time)
                                .integrationIds(List.of("1"))
                                .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.poor_description,
                                        JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time))
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.assignee)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .integrationIds(List.of("1"))
                                .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.inactive_assignees))
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.assignee)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .integrationIds(List.of("1"))
                                .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.inactive_assignees,
                                        JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time))
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .calculation(JiraIssuesFilter.CALCULATION.response_time)
                                .integrationIds(List.of("1"))
                                .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.poor_description,
                                        JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time))
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.component)
                                .calculation(JiraIssuesFilter.CALCULATION.hops)
                                .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.no_due_date))
                                .build(),
                        true, null, null, Map.of())
                .getCount()).isEqualTo(7);

    }

    void testPrioritySla() {
        assertThat(jiraIssueService.listPrioritiesSla(company, List.of("1"), null, null,
                null, 0, 1000).getTotalCount()).isEqualTo(16);
        assertThat(jiraIssueService.listPrioritiesSla(company, List.of("1"), List.of("LEV"), null,
                null, 0, 1000).getTotalCount()).isEqualTo(14);
        assertThat(jiraIssueService.listPrioritiesSla(company, List.of("1"), null, List.of("BUG"),
                null, 0, 1000).getTotalCount()).isEqualTo(3);
        assertThat(jiraIssueService.listPrioritiesSla(company, List.of("1"), null, null,
                List.of("HIGH", "HIGHEST"), 0, 1000).getTotalCount()).isEqualTo(7);
    }

    void testConfigFilter() throws SQLException {
        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.none)
                        .build(),
                false, "Custom Row Value", null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(1);

        assertThat(dbAggregationResultDbListResponse.getRecords().get(0).getKey()).isEqualTo("Custom Row Value");
    }

    @Test
    public void testStoryPointFeature() throws SQLException {
        // testStoryPoints();
        // testStoryPointsAggregations();
    }

    public void testStoryPoints() throws SQLException {
        List<DbJiraIssue> issues = jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .parentStoryPoints(Map.of("$gt", "1"))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                null, Map.of("parent_story_points", SortingOrder.DESC),
                0, 10000).getRecords();
        assertThat(issues.size()).isEqualTo(0);
//        assertThat(issues.get(0).getKey()).isEqualTo("LEV-570");
        assertThat(jiraIssueService.get(company, issues.get(0).getEpic(), "1", ingestedAt)
                .get().getStoryPoints()).isEqualTo(400);
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
        assertThat(aggs.size()).isEqualTo(4);
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
        assertThat(aggs.size()).isEqualTo(4);
    }

    @Test
    public void testTimeAcrossStages() throws SQLException {
        List<DbAggregationResult> aggs = null;
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.project)
                        .calculation(JiraIssuesFilter.CALCULATION.stage_times_report)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(10);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .excludeStages(List.of("WONT DO", "DONE"))
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .calculation(JiraIssuesFilter.CALCULATION.stage_times_report)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(4);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.assignee)
                        .calculation(JiraIssuesFilter.CALCULATION.stage_times_report)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(11);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.reporter)
                        .calculation(JiraIssuesFilter.CALCULATION.stage_times_report)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(11);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.first_assignee)
                        .calculation(JiraIssuesFilter.CALCULATION.stage_times_report)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(11);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .calculation(JiraIssuesFilter.CALCULATION.stage_times_report)
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(15);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .calculation(JiraIssuesFilter.CALCULATION.stage_times_report)
                        .links(List.of("is blocked by"))
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
    }

    @Test
    public void testExcludeSprints() throws SQLException {
        List<DbAggregationResult> excludeSprintId1 = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .excludeSprintIds(List.of("1"))
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(excludeSprintId1.size()).isEqualTo(2);
        assertThat(excludeSprintId1.stream().map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("sprint2", "sprint3"));

        List<DbAggregationResult> excludeSprintId2 = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .excludeSprintIds(List.of("1", "2"))
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(excludeSprintId2.size()).isEqualTo(1);
        assertThat(excludeSprintId2.get(0).getKey()).isEqualTo("sprint3");

        List<DbAggregationResult> excludeSprintNames1 = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .excludeSprintNames(List.of("sprint3"))
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(excludeSprintNames1.size()).isEqualTo(2);
        assertThat(excludeSprintNames1.stream().map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("sprint1", "sprint2"));

        List<DbAggregationResult> excludeSprintNames2 = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .excludeSprintNames(List.of("sprint3", "sprint2"))
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(excludeSprintNames2.size()).isEqualTo(1);
        assertThat(excludeSprintNames2.get(0).getKey()).isEqualTo("sprint1");

        List<DbAggregationResult> excludeSprintStates1 = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .excludeSprintStates(List.of("closed"))
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(excludeSprintStates1.size()).isEqualTo(1);
        assertThat(excludeSprintStates1.get(0).getKey()).isEqualTo("sprint1");

        List<DbAggregationResult> excludeSprintStates2 = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .excludeSprintStates(List.of("active"))
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(excludeSprintStates2.size()).isEqualTo(2);
        assertThat(excludeSprintStates2.stream().map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("sprint2", "sprint3"));
    }

    @Test
    public void testSprintsFilterAndGroupBy() throws SQLException, JsonProcessingException {
        List<DbAggregationResult> aggs;
        Map<String, Object> customFields = new HashMap<>();
        Map<String, List<String>> salesforceFields = new HashMap<>();
        List<DbJiraIssue> issues = List.of(
                DbJiraIssue.builder()
                        .key("UN-35")
                        .integrationId("1")
                        .project("TS")
                        .summary("Remove access for user")
                        .components(List.of())
                        .labels(List.of())
                        .versions(List.of())
                        .fixVersions(List.of())
                        .descSize(51)
                        .priority("MEDIUM")
                        .reporter("")
                        .status("To Do")
                        .issueType("Task")
                        .bounces(1)
                        .hops(1)
                        .numAttachments(1)
                        .issueCreatedAt(Long.valueOf(1613658414))
                        .issueUpdatedAt(Long.valueOf(1613658414))
                        .ingestedAt(ingestedAt)
                        .sprintIds(List.of(1))
                        .customFields(customFields)
                        .salesforceFields(salesforceFields)
                        .build(),
                DbJiraIssue.builder()
                        .key("UN-13")
                        .integrationId("1")
                        .project("LEV")
                        .summary("Summary")
                        .components(List.of())
                        .labels(List.of())
                        .versions(List.of())
                        .fixVersions(List.of())
                        .descSize(51)
                        .priority("MEDIUM")
                        .reporter("")
                        .status("DONE")
                        .issueType("Epic")
                        .bounces(0)
                        .hops(0)
                        .numAttachments(0)
                        .issueCreatedAt(Long.valueOf(1613658414))
                        .issueUpdatedAt(Long.valueOf(1613658414))
                        .ingestedAt(ingestedAt)
                        .sprintIds(List.of(2, 3))
                        .customFields(customFields)
                        .salesforceFields(salesforceFields)
                        .build()
        );
        issues.forEach(issue -> {
            try {
                jiraIssueService.insert(company, issue);
            } catch (SQLException throwables) {
            }
        });
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .sprintIds(List.of("1", "2"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.project)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(5);
    }

    @Test
    public void testResolutionFilterAndGroupBy() throws SQLException {
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .resolutions(List.of("DONE"))
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getTotalCount())
                .isEqualTo(8);
        List<DbAggregationResult> aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.resolution)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), List.of(JiraIssuesFilter.DISTINCT.resolution), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(5);
    }

    @Test
    public void testResolutionExcludeStages() throws SQLException {
        // region Resolution_time_report - with one exclude stage
        List<DbAggregationResult> excludeStageTest1 = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.assignee)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .keys(List.of("LEV-999"))
                                .excludeStages(List.of("TO DO"))
                                .build(), false, null, null, Map.of())
                .getRecords();

        // region Resolution_time_report - without exclude stage
        List<DbAggregationResult> excludeStageTest2 = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.assignee)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .keys(List.of("LEV-999"))
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat((excludeStageTest2.get(0)).getMax()).isGreaterThan(excludeStageTest1.get(0).getMax());
        assertThat((excludeStageTest2.get(0)).getMin()).isGreaterThan(excludeStageTest1.get(0).getMin());
        assertThat((excludeStageTest2.get(0)).getMean()).isGreaterThan(excludeStageTest1.get(0).getMean());
        assertThat((excludeStageTest2.get(0)).getMedian()).isGreaterThan(excludeStageTest1.get(0).getMedian());

        // region Resolution_time_report - with all exclude stages
        List<DbAggregationResult> excludeStageTest3 = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.assignee)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .keys(List.of("LEV-999"))
                                .excludeStages(List.of("TO DO", "IN REVIEW", "IN PROGRESS", "BLOCKED", "DONE"))
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(excludeStageTest3.get(0).getMax()).isEqualTo(0);
        assertThat(excludeStageTest3.get(0).getMin()).isEqualTo(0);
        assertThat(excludeStageTest3.get(0).getMean()).isEqualTo(0);
        assertThat(excludeStageTest3.get(0).getMedian()).isEqualTo(0);

        // region Resolution_time_report - with multiple exclude stages
        List<DbAggregationResult> excludeStageTest4 = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.assignee)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .keys(List.of("LEV-999"))
                                .excludeStages(List.of("TO DO", "IN REVIEW"))
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(excludeStageTest1.get(0).getMax()).isGreaterThanOrEqualTo(excludeStageTest4.get(0).getMax());
        assertThat(excludeStageTest1.get(0).getMin()).isGreaterThanOrEqualTo(excludeStageTest4.get(0).getMin());
        assertThat(excludeStageTest1.get(0).getMean()).isGreaterThanOrEqualTo(excludeStageTest4.get(0).getMean());
        assertThat(excludeStageTest1.get(0).getMedian()).isGreaterThanOrEqualTo(excludeStageTest4.get(0).getMedian());

        // region Resolution_time_report - without keys
        List<DbAggregationResult> excludeStageTest5 = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.assignee)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .excludeStages(List.of("TO DO"))
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(excludeStageTest5.size()).isEqualTo(4);
        // endregion

        // region Resolution_time_report - with key - without any exclude stage
        List<DbAggregationResult> resolutionTimeReport = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.assignee)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .keys(List.of("LEV-999"))
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(resolutionTimeReport.get(0).getMedian()).isEqualTo(7868);
        // end region

        // region Resolution_time_report - with key - with invalid exclude stage
        resolutionTimeReport = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.assignee)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .keys(List.of("LEV-999"))
                                .excludeStages(List.of("LOREM IPSUM"))
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(resolutionTimeReport.get(0).getMedian()).isEqualTo(7868);
        // end region

        // region Resolution_time_report - with key - with valid exclude stages
        resolutionTimeReport = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.assignee)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .keys(List.of("LEV-999"))
                                .excludeStages(List.of("TO DO", "IN REVIEW"))
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(resolutionTimeReport.get(0).getMedian()).isEqualTo(6452);
        // end region

        // region Resolution_time_report - with key - with valid and invalid exclude stages
        resolutionTimeReport = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                                .across(JiraIssuesFilter.DISTINCT.assignee)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .keys(List.of("LEV-999"))
                                .excludeStages(List.of("TO DO", "IN REVIEW", "LOREM IPSUM"))
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(resolutionTimeReport.get(0).getMedian()).isEqualTo(6452);
        // end region

    }

    @Test
    public void testListByIngestedAt() throws SQLException {
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getTotalCount())
                .isEqualTo(29);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(1582675200L)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getTotalCount())
                .isEqualTo(0);
    }

    @Test
    public void testStatusCategoryFilterAndGroupBy() throws SQLException {
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .statusCategories(List.of("In Progress"))
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), null, Map.of("num_attachments", SortingOrder.ASC),
                0, 10000).getTotalCount())
                .isEqualTo(7);
        List<DbAggregationResult> aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.status_category)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), List.of(JiraIssuesFilter.DISTINCT.status_category), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(5);
    }

    @Test
    public void testSprintNameFilter() throws SQLException {
        List<DbAggregationResult> aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .partialMatch(Map.of("sprint_name", Map.of("$begins", "sprint")))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .partialMatch(Map.of("sprint_name", Map.of("$ends", "1")))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .partialMatch(Map.of("sprint_name", Map.of("$contains", "sprint1")))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("invalid-sprint"))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(0);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .partialMatch(Map.of("sprint_name", Map.of("$begins", "invalid-sprint")))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(0);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .partialMatch(Map.of("sprint_name", Map.of("$ends", "invalid-1")))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(0);
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .partialMatch(Map.of("sprint_name", Map.of("$contains", "invalid-sprint")))
                                .across(JiraIssuesFilter.DISTINCT.sprint)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(0);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("sprint1"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(5);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .partialMatch(Map.of("sprint_name", Map.of("$begins", "sprint")))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(5);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .partialMatch(Map.of("sprint_name", Map.of("$ends", "1")))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(5);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .partialMatch(Map.of("sprint_name", Map.of("$contains", "sprint1")))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(5);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .sprintNames(List.of("invalid-sprint"))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(0);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .partialMatch(Map.of("sprint_name", Map.of("$begins", "invalid-sprint")))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(0);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .partialMatch(Map.of("sprint_name", Map.of("$ends", "invalid-1")))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(0);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .partialMatch(Map.of("sprint_name", Map.of("$contains", "invalid-sprint")))
                                .across(JiraIssuesFilter.DISTINCT.project)
                                .ingestedAt(ingestedAt)
                                .hygieneCriteriaSpecs(Map.of())
                                .build(), List.of(JiraIssuesFilter.DISTINCT.sprint), null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(0);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .across(JiraIssuesFilter.DISTINCT.trend)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.epic), null, null, Map.of())
                .getRecords();
        DefaultObjectMapper.prettyPrint(aggs);
        assertThat(aggs.size()).isEqualTo(3);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .calculation(JiraIssuesFilter.CALCULATION.age)
                                .across(JiraIssuesFilter.DISTINCT.reporter)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.trend), null, null, Map.of())
                .getRecords();
        DefaultObjectMapper.prettyPrint(aggs);
        assertThat(aggs.size()).isEqualTo(6);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .calculation(JiraIssuesFilter.CALCULATION.bounces)
                                .across(JiraIssuesFilter.DISTINCT.epic)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.trend), null, null, Map.of())
                .getRecords();
        DefaultObjectMapper.prettyPrint(aggs);
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .calculation(JiraIssuesFilter.CALCULATION.hops)
                                .across(JiraIssuesFilter.DISTINCT.component)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.trend), null, null, Map.of())
                .getRecords();
        DefaultObjectMapper.prettyPrint(aggs);
        assertThat(aggs.size()).isEqualTo(5);

    }


    @Test
    public void testStatusAsAcross() throws SQLException {
        List<DbAggregationResult> aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .calculation(null)
                        .build(), true, null, null, Map.of())
                .getRecords();
        DefaultObjectMapper.prettyPrint(aggs);
        assertThat(aggs.size()).isEqualTo(5);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .statusCategories(List.of("STATUS-CATEGORY-1"))
                        .build(), true, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .excludeStatusCategories(List.of("STATUS-CATEGORY-1"))
                        .build(), true, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(4);
    }

    public void testStoryPointsAggregations() throws SQLException {
        List<DbAggregationResult> aggs = null;
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .parentStoryPoints(Map.of(
                                "$gt", "1",
                                "$lt", "500"
                        ))
                        .across(JiraIssuesFilter.DISTINCT.priority)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        assertThat(aggs.get(0).getTotalTickets()).isEqualTo(136);
        assertThat(aggs.get(0).getKey()).isEqualTo("MEDIUM");
        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .storyPoints(Map.of(
                                "$gt", "400",
                                "$lt", "500"
                        ))
                        .across(JiraIssuesFilter.DISTINCT.priority)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(), false, null, null, Map.of())
                .getRecords();
        assertThat(aggs.size()).isEqualTo(0);
//        assertThat(aggs.get(0).getTotalTickets()).isEqualTo(97);
//        assertThat(aggs.get(1).getTotalTickets()).isEqualTo(1);
//        assertThat(aggs.get(2).getTotalTickets()).isEqualTo(1);
//        aggs = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
//                .integrationIds(List.of("1"))
//                .ingestedAt(ingestedAt)
//                .storyPoints(Map.of(
//                        "$gt", "300",
//                        "$lt", "500"
//                ))
//                .across(JiraIssuesFilter.DISTINCT.priority)
//                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
//                .hygieneCriteriaSpecs(Map.of())
//                .build(), false, null, null)
//                .getRecords();
//        assertThat(aggs.size()).isEqualTo(3);
//        assertThat(aggs.get(0).getTotalTickets()).isEqualTo(193);
//        assertThat(aggs.get(1).getTotalTickets()).isEqualTo(3);
//        assertThat(aggs.get(2).getTotalTickets()).isEqualTo(3);
//        aggs = jiraIssueService.stackedGroupBy(company, JiraIssuesFilter.builder()
//                        .integrationIds(List.of("1"))
//                        .ingestedAt(ingestedAt)
//                        .storyPoints(Map.of(
//                                "$gt", "400",
//                                "$lt", "500"
//                        ))
//                        .across(JiraIssuesFilter.DISTINCT.priority)
//                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
//                        .hygieneCriteriaSpecs(Map.of())
//                        .build(),
//                List.of(JiraIssuesFilter.DISTINCT.assignee),
//                null).getRecords();
//        assertThat(aggs.get(0).getStacks().stream().map(DbAggregationResult::getTotalTickets).min(Comparator.naturalOrder()).get()).isEqualTo(7);
//        assertThat(aggs.get(0).getStacks().stream().map(DbAggregationResult::getTotalTickets).max(Comparator.naturalOrder()).get()).isEqualTo(23);
//        assertThat(aggs.get(1).getStacks().stream().map(DbAggregationResult::getTotalTickets).min(Comparator.naturalOrder()).get()).isEqualTo(1);
//        assertThat(aggs.get(1).getStacks().stream().map(DbAggregationResult::getTotalTickets).max(Comparator.naturalOrder()).get()).isEqualTo(1);
//        assertThat(aggs.get(2).getStacks().stream().map(DbAggregationResult::getTotalTickets).min(Comparator.naturalOrder()).get()).isEqualTo(1);
//        assertThat(aggs.get(2).getStacks().stream().map(DbAggregationResult::getTotalTickets).max(Comparator.naturalOrder()).get()).isEqualTo(1);
    }

    /*
    Setup
    MT-7 -> customfield_20001(Magic2, Magic3, Magic7)
    MT-8 -> customfield_20001(Magic3, Magic4, Magic5)
    TS-3 -> customfield_20001(Magic1, Magic2, Magic3)
     */
    @Test
    public void testCustomFieldsWithDelimiter() throws SQLException {
        List<DbAggregationResult> records = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .hygieneCriteriaSpecs(Map.of())
                .across(JiraIssuesFilter.DISTINCT.custom_field)
                .customAcross("customfield_20001")
                .ingestedAt(ingestedAt)
//                .customFields(Map.of("customfield_20001", List.of("Magic1")))
                .build(), false, null, null, Map.of()).getRecords();
        Map<String, Long> customFieldKeyToTickets = records.stream()
                .collect(Collectors.toMap(DbAggregationResult::getKey, DbAggregationResult::getTotalTickets));
        Map<String, Long> expectedReport = Map.of(
                "Magic1", 1L,
                "Magic2", 2L,
                "Magic3", 3L,
                "Magic4", 1L,
                "Magic5", 1L,
                "Magic7", 1L);
        assertThat(customFieldKeyToTickets).isEqualTo(expectedReport);

        records = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .hygieneCriteriaSpecs(Map.of())
                .across(JiraIssuesFilter.DISTINCT.custom_field)
                .customAcross("customfield_20001")
                .ingestedAt(ingestedAt)
                .partialMatch(Map.of("customfield_20001", Map.of("$begins", "Magic2")))
                .build(), false, null, null, Map.of()).getRecords();
        customFieldKeyToTickets = records.stream()
                .collect(Collectors.toMap(DbAggregationResult::getKey, DbAggregationResult::getTotalTickets));
        expectedReport = Map.of(
                "Magic1", 1L,
                "Magic2", 2L,
                "Magic3", 2L,
                "Magic7", 1L);
        assertThat(customFieldKeyToTickets).isEqualTo(expectedReport);

        records = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .hygieneCriteriaSpecs(Map.of())
                .across(JiraIssuesFilter.DISTINCT.custom_field)
                .customAcross("customfield_20001")
                .ingestedAt(ingestedAt)
                .partialMatch(Map.of("customfield_20001", Map.of("$contains", "agic")))
                .build(), false, null, null, Map.of()).getRecords();
        customFieldKeyToTickets = records.stream()
                .collect(Collectors.toMap(DbAggregationResult::getKey, DbAggregationResult::getTotalTickets));
        expectedReport = Map.of(
                "Magic1", 1L,
                "Magic2", 2L,
                "Magic3", 3L,
                "Magic4", 1L,
                "Magic5", 1L,
                "Magic7", 1L);
        assertThat(customFieldKeyToTickets).isEqualTo(expectedReport);

        records = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .hygieneCriteriaSpecs(Map.of())
                .across(JiraIssuesFilter.DISTINCT.custom_field)
                .customAcross("customfield_20001")
                .ingestedAt(ingestedAt)
                .partialMatch(Map.of("customfield_20001", Map.of("$ends", "ic4")))
                .build(), false, null, null, Map.of()).getRecords();
        customFieldKeyToTickets = records.stream()
                .collect(Collectors.toMap(DbAggregationResult::getKey, DbAggregationResult::getTotalTickets));
        expectedReport = Map.of(
                "Magic3", 1L,
                "Magic4", 1L,
                "Magic5", 1L);
        assertThat(customFieldKeyToTickets).isEqualTo(expectedReport);

        records = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .hygieneCriteriaSpecs(Map.of())
                .across(JiraIssuesFilter.DISTINCT.custom_field)
                .customAcross("customfield_20001")
                .ingestedAt(ingestedAt)
                .excludeCustomFields(Map.of("customfield_20001", List.of("Magic2")))
                .build(), false, null, null, Map.of()).getRecords();
        customFieldKeyToTickets = records.stream()
                .collect(Collectors.toMap(DbAggregationResult::getKey, DbAggregationResult::getTotalTickets));
        expectedReport = Map.of(
                "Magic3", 1L,
                "Magic4", 1L,
                "Magic5", 1L);
        assertThat(customFieldKeyToTickets).isEqualTo(expectedReport);

        records = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .hygieneCriteriaSpecs(Map.of())
                .across(JiraIssuesFilter.DISTINCT.custom_field)
                .customAcross("customfield_20001")
                .ingestedAt(ingestedAt)
                .excludeCustomFields(Map.of("customfield_20001", List.of("Magic3")))
                .build(), false, null, null, Map.of()).getRecords();
        customFieldKeyToTickets = records.stream()
                .collect(Collectors.toMap(DbAggregationResult::getKey, DbAggregationResult::getTotalTickets));
        expectedReport = Map.of();
        assertThat(customFieldKeyToTickets).isEqualTo(expectedReport);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .customFields(Map.of("customfield_20001", List.of("Magic1")))
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .excludeCustomFields(Map.of("customfield_20001", List.of("Magic7", "Magic5")))
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(26);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .customFields(Map.of("customfield_20001", List.of("Magic3", "Magic2")))
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(3);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .customFields(Map.of("customfield_20001", List.of("Magic2")))
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(2);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .customFields(Map.of("customfield_20001", List.of("Magic3")))
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(3);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .hygieneCriteriaSpecs(Map.of())
                                .ingestedAt(ingestedAt)
                                .customFields(Map.of("customfield_20001", List.of("Magic7", "Magic3", "Magic5")))
                                .build(),
                        null, Map.of("num_attachments", SortingOrder.ASC), 0, 10000)
                .getTotalCount())
                .isEqualTo(3);
    }

    @Test
    public void testInsertLinkedIssueRelation() {
        assertThat(jiraIssueService.insertJiraLinkedIssueRelation(company, "1", "LEV-1", "LEV-2", "blocks"))
                .isEqualTo(true);
        assertThat(jiraIssueService.insertJiraLinkedIssueRelation(company, "1", "LEV-1", "LEV-2", "is blocked by"))
                .isEqualTo(true);
    }

    @Test
//    @Ignore
    public void testVelocityConfig() throws SQLException {
        DbListResponse<DbJiraIssue> list = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .velocityStages(List.of("Stage-1-pre"))
                        .build(),
                Optional.empty(),
                null, Optional.of(VelocityConfigDTO.builder()
                        .name("WorkflowProfile-1")
                        .postDevelopmentCustomStages(List.of(VelocityConfigDTO.Stage.builder()
                                        .order(0)
                                        .name("Stage-1-post")
                                        .event(VelocityConfigDTO.Event.builder()
                                                .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                                .values(List.of("READY FOR QA"))
                                                .build())
                                        .lowerLimitValue(2L).lowerLimitUnit(TimeUnit.DAYS)
                                        .upperLimitValue(4L).upperLimitUnit(TimeUnit.DAYS)
                                        .build(),
                                VelocityConfigDTO.Stage.builder()
                                        .order(1)
                                        .name("Stage-2-post")
                                        .event(VelocityConfigDTO.Event.builder()
                                                .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                                .values(List.of("DONE"))
                                                .build())
                                        .lowerLimitValue(2L).lowerLimitUnit(TimeUnit.DAYS)
                                        .upperLimitValue(4L).upperLimitUnit(TimeUnit.DAYS)
                                        .build()
                        ))
                        .preDevelopmentCustomStages(List.of(VelocityConfigDTO.Stage.builder()
                                        .order(0)
                                        .name("Stage-1-pre")
                                        .event(VelocityConfigDTO.Event.builder()
                                                .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                                .values(List.of("BACKLOG"))
                                                .build())
                                        .lowerLimitValue(2L).lowerLimitUnit(TimeUnit.DAYS)
                                        .upperLimitValue(4L).upperLimitUnit(TimeUnit.DAYS)
                                        .build(),
                                VelocityConfigDTO.Stage.builder()
                                        .order(1)
                                        .name("Stage-2-pre")
                                        .event(VelocityConfigDTO.Event.builder()
                                                .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                                .values(List.of("TO DO", "TODO"))
                                                .build())
                                        .lowerLimitValue(2L).lowerLimitUnit(TimeUnit.DAYS)
                                        .upperLimitValue(4L).upperLimitUnit(TimeUnit.DAYS)
                                        .build()
                        ))
                        .build()), Map.of(), 0, 10000);
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(3);

        list = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .velocityStages(List.of("Stage-2-pre"))
                        .build(),
                Optional.empty(),
                null, Optional.of(VelocityConfigDTO.builder()
                        .name("WorkflowProfile-1")
                        .postDevelopmentCustomStages(List.of(VelocityConfigDTO.Stage.builder()
                                        .order(0)
                                        .name("Stage-1-post")
                                        .event(VelocityConfigDTO.Event.builder()
                                                .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                                .values(List.of("READY FOR QA"))
                                                .build())
                                        .lowerLimitValue(2L).lowerLimitUnit(TimeUnit.DAYS)
                                        .upperLimitValue(4L).upperLimitUnit(TimeUnit.DAYS)
                                        .build(),
                                VelocityConfigDTO.Stage.builder()
                                        .order(1)
                                        .name("Stage-2-post")
                                        .event(VelocityConfigDTO.Event.builder()
                                                .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                                .values(List.of("DONE"))
                                                .build())
                                        .lowerLimitValue(2L).lowerLimitUnit(TimeUnit.DAYS)
                                        .upperLimitValue(4L).upperLimitUnit(TimeUnit.DAYS)
                                        .build()
                        ))
                        .preDevelopmentCustomStages(List.of(VelocityConfigDTO.Stage.builder()
                                        .order(0)
                                        .name("Stage-1-pre")
                                        .event(VelocityConfigDTO.Event.builder()
                                                .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                                .values(List.of("BACKLOG"))
                                                .build())
                                        .lowerLimitValue(2L).lowerLimitUnit(TimeUnit.DAYS)
                                        .upperLimitValue(4L).upperLimitUnit(TimeUnit.DAYS)
                                        .build(),
                                VelocityConfigDTO.Stage.builder()
                                        .order(1)
                                        .name("Stage-2-pre")
                                        .event(VelocityConfigDTO.Event.builder()
                                                .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                                .values(List.of("TO DO", "TODO", "WON'T DO"))
                                                .build())
                                        .lowerLimitValue(2L).lowerLimitUnit(TimeUnit.DAYS)
                                        .upperLimitValue(4L).upperLimitUnit(TimeUnit.DAYS)
                                        .build()))
                        .build()), Map.of(), 0, 10000);
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(22);

        Optional<VelocityConfigDTO> optVelocityConfigDTO = Optional.of(VelocityConfigDTO.builder()
                .name("WorkflowProfile-1")
                .postDevelopmentCustomStages(List.of(VelocityConfigDTO.Stage.builder()
                                .order(0)
                                .name("Stage-1-post")
                                .event(VelocityConfigDTO.Event.builder()
                                        .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                        .values(List.of("READY FOR QA"))
                                        .build())
                                .lowerLimitValue(2L).lowerLimitUnit(TimeUnit.DAYS)
                                .upperLimitValue(4L).upperLimitUnit(TimeUnit.DAYS)
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .order(1)
                                .name("Stage-2-post")
                                .event(VelocityConfigDTO.Event.builder()
                                        .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                        .values(List.of("DONE"))
                                        .build())
                                .lowerLimitValue(2L).lowerLimitUnit(TimeUnit.DAYS)
                                .upperLimitValue(4L).upperLimitUnit(TimeUnit.DAYS)
                                .build()
                ))
                .preDevelopmentCustomStages(List.of(VelocityConfigDTO.Stage.builder()
                                .order(0)
                                .name("Stage-1-pre")
                                .event(VelocityConfigDTO.Event.builder()
                                        .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                        .values(List.of("BACKLOG"))
                                        .build())
                                .lowerLimitValue(2L).lowerLimitUnit(TimeUnit.DAYS)
                                .upperLimitValue(4L).upperLimitUnit(TimeUnit.DAYS)
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .order(1)
                                .name("Stage-2-pre")
                                .event(VelocityConfigDTO.Event.builder()
                                        .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                        .values(List.of("TO DO", "TODO", "WON'T DO"))
                                        .build())
                                .lowerLimitValue(2L).lowerLimitUnit(TimeUnit.DAYS)
                                .upperLimitValue(4L).upperLimitUnit(TimeUnit.DAYS)
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .order(1)
                                .name("Stage-3-pre")
                                .event(VelocityConfigDTO.Event.builder()
                                        .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                        .values(List.of("WON'T DO", "CAN'T DO"))
                                        .build())
                                .lowerLimitValue(2L).lowerLimitUnit(TimeUnit.DAYS)
                                .upperLimitValue(4L).upperLimitUnit(TimeUnit.DAYS)
                                .build()
                ))
                .build());
        list = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .velocityStages(List.of("Stage-3-pre"))
                        .build(),
                Optional.empty(),
                null, optVelocityConfigDTO, Map.of(), 0, 10000);
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(0);

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.velocity_stage)
                        .calculation(JiraIssuesFilter.CALCULATION.velocity_stage_times_report)
                        .build(), false,
                null,
                null,
                Map.of("Stage-1-post", List.of("READY FOR QA"),
                        "Stage-2-post", List.of("DONE"),
                        "Stage-1-pre", List.of("BACKLOG"),
                        "Stage-2-pre", List.of("TO DO", "TODO", "WON'T DO"),
                        "Stage-3-pre", List.of("WON'T DO", "CAN'T DO")));
        Assertions.assertThat(dbAggregationResultDbListResponse).isNotNull();
        Assertions.assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(4);
        /*
              Asserting report and drilldown average values for different stages
         */
        list = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .velocityStages(List.of("Stage-2-pre"))
                        .build(),
                Optional.empty(),
                null, optVelocityConfigDTO, Map.of(), 0, 10000);
        Map<String, Double> stageMeanMap = dbAggregationResultDbListResponse.getRecords().stream()
                .collect(Collectors.toMap(DbAggregationResult::getKey, DbAggregationResult::getMean));
        Double averageOfStageFromList = list.getRecords().stream().map(DbJiraIssue::getVelocityStageTime).mapToDouble(Long::doubleValue).average().orElseThrow();
        Double reportAverage = stageMeanMap.getOrDefault("Stage-2-pre", 0.0);
        Assertions.assertThat(reportAverage).isEqualTo(averageOfStageFromList);

        list = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .velocityStages(List.of("Stage-1-pre"))
                        .build(),
                Optional.empty(),
                null, optVelocityConfigDTO, Map.of(), 0, 10000);
        stageMeanMap = dbAggregationResultDbListResponse.getRecords().stream()
                .collect(Collectors.toMap(DbAggregationResult::getKey, DbAggregationResult::getMean));
        averageOfStageFromList = list.getRecords().stream().map(DbJiraIssue::getVelocityStageTime).mapToDouble(Long::doubleValue).average().orElseThrow();
        reportAverage = stageMeanMap.getOrDefault("Stage-1-pre", 0.0);
        Assertions.assertThat(reportAverage).isEqualTo(averageOfStageFromList);

        list = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .velocityStages(List.of("Stage-2-post"))
                        .build(),
                Optional.empty(),
                null, optVelocityConfigDTO, Map.of(), 0, 10000);
        stageMeanMap = dbAggregationResultDbListResponse.getRecords().stream()
                .collect(Collectors.toMap(DbAggregationResult::getKey, DbAggregationResult::getMean));
        averageOfStageFromList = list.getRecords().stream().map(DbJiraIssue::getVelocityStageTime).mapToDouble(Long::doubleValue).average().orElseThrow();
        reportAverage = stageMeanMap.getOrDefault("Stage-2-post", 0.0);
        Assertions.assertThat(reportAverage).isEqualTo(averageOfStageFromList);

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse1 = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.velocity_stage)
                        .calculation(JiraIssuesFilter.CALCULATION.velocity_stage_times_report)
                        .calculateSingleState(true)
                        .build(), false,
                null,
                null,
                Map.of("Stage-1-post", List.of("READY FOR QA"),
                        "Stage-2-post", List.of("DONE"),
                        "Stage-1-pre", List.of("BACKLOG"),
                        "Stage-2-pre", List.of("TO DO", "TODO", "WON'T DO"),
                        "Stage-3-pre", List.of("WON'T DO", "CAN'T DO")));
        Assertions.assertThat(dbAggregationResultDbListResponse1).isNotNull();
        Assertions.assertThat(dbAggregationResultDbListResponse1.getTotalCount()).isEqualTo(5);

        DbListResponse<DbJiraIssue> list4 = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .velocityStages(List.of())
                        .build(),
                Optional.empty(),
                null, optVelocityConfigDTO, Map.of(), 0, 10000);

        Set<DbJiraIssue> objectSet = list4.getRecords().stream()
                .collect(Collectors.toMap(DbJiraIssue::getId, Function.identity(), (o1, o2) -> o1, HashMap::new))
                .values()
                .stream()
                .collect(Collectors.toSet());

        DecimalFormat df = new DecimalFormat("0.00");

        Double avg = objectSet.stream().mapToDouble(DbJiraIssue::getVelocityStageTotalTime).average().orElse(0.0);
        Optional<DbAggregationResult> aggregationResult = dbAggregationResultDbListResponse1.getRecords().stream().filter(x -> x.getKey().equals("SingleState")).findFirst();
        String drillDownMean = df.format(avg);
        String singleStateMean = df.format(aggregationResult.get().getMean());
        Assert.assertEquals(drillDownMean, singleStateMean);

        Double max = objectSet.stream().mapToDouble(DbJiraIssue::getVelocityStageTotalTime).max().orElse(0.0);
        String drillDownMax = df.format(max);
        String singleStateMax = df.format(aggregationResult.get().getMax());
        Assert.assertEquals(drillDownMax, singleStateMax);

        Double min = objectSet.stream().mapToDouble(DbJiraIssue::getVelocityStageTotalTime).min().orElse(0.0);
        String drillDownMin = df.format(min);
        String singleStateMin = df.format(aggregationResult.get().getMin());
        Assert.assertEquals(drillDownMin, singleStateMin);
    }

    @Test
    public void testLinkedIssue() throws SQLException {
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .links(List.of("is blocked by"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(2);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .links(List.of("is blocked by", "blocks"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(2);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .excludeLinks(List.of("is blocked by"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(3);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .excludeLinks(List.of("is blocked by", "blocks"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .links(List.of("invalid"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .keys(List.of("TEST-881", "TEST-872", "TEST-96"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(3);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .keys(List.of("TEST-881", "TEST-872", "TEST-96"))
                                .links(List.of("blocks", "is blocked by"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(2);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .keys(List.of("TEST-881", "TEST-872", "TEST-96"))
                                .excludeLinks(List.of("blocks", "is blocked by"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        null, Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .links(List.of("blocks", "is blocked by"))
                                .ingestedAt(ingestedAt)
                                .sprintNames(List.of("sprint1", "sprint2", "sprint3", "sprint4"))
                                .sprintIds(List.of("1", "2", "3", "4"))
                                .versions(List.of("New Version 1", "New Version 2", "New Version 3"))
                                .build(),
                        null, Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .links(List.of("blocks", "is blocked by"))
                                .ingestedAt(ingestedAt)
                                .sprintNames(List.of("sprint1", "sprint2", "sprint3", "sprint4"))
                                .sprintIds(List.of("1", "2", "3", "4"))
                                .versions(List.of("New Version 1", "New Version 2", "New Version 3"))
                                .sort(Map.of(JiraIssuesFilter.DISTINCT.priority.toString(), SortingOrder.ASC))
                                .build(),
                        null, Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .links(List.of("is blocked by"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        Optional.of(JiraIssuesFilter.builder().issueTypes(List.of("STORY")).build()),
                        null, Optional.empty(), Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .links(List.of("is blocked by"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        Optional.of(JiraIssuesFilter.builder().issueTypes(List.of("STORY")).build()),
                        null, Optional.empty(), Map.of(), 0, 10000)
                .getRecords().stream().map(DbJiraIssue::getIssueType)
                .collect(Collectors.toList()))
                .isEqualTo(List.of("STORY"));
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .links(List.of("is blocked by", "blocks"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        Optional.of(JiraIssuesFilter.builder().versions(List.of("New Version 3")).build()),
                        null, Optional.empty(), Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .excludeLinks(List.of("is blocked by"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        Optional.of(JiraIssuesFilter.builder()
                                .assignees(List.of(userIdOf("Meghana").isPresent() ? userIdOf("Meghana").get() : ""))
                                .build()),
                        null, Optional.empty(), Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .excludeLinks(List.of("is blocked by", "blocks"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        Optional.of(JiraIssuesFilter.builder().statuses(List.of("TO DO")).build()),
                        null, Optional.empty(), Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .links(List.of("invalid"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        Optional.of(JiraIssuesFilter.builder().statuses(List.of("In Progress")).build()),
                        null, Optional.empty(), Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(0);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .keys(List.of("TEST-881", "TEST-872", "TEST-96"))
                                .ingestedAt(ingestedAt)
                                .build(),
                        Optional.of(JiraIssuesFilter.builder().priorities(List.of("HIGHEST")).build()),
                        null, Optional.empty(), Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(3);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .links(List.of("blocks", "is blocked by"))
                                .ingestedAt(ingestedAt)
                                .sprintNames(List.of("sprint1", "sprint2", "sprint3", "sprint4"))
                                .sprintIds(List.of("1", "2", "3", "4"))
                                .versions(List.of("New Version 1", "New Version 2", "New Version 3"))
                                .build(),
                        Optional.of(JiraIssuesFilter.builder().projects(List.of("TEST")).build()),
                        null, Optional.empty(), Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(1);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.trend)
                                .aggInterval("week")
                                .build(),
                        Optional.of(JiraIssuesFilter.builder().projects(List.of("TEST")).build()),
                        null, Optional.empty(), Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(27);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .links(List.of("blocks", "is blocked by"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.trend)
                                .aggInterval("week")
                                .build(),
                        Optional.of(JiraIssuesFilter.builder().projects(List.of("TEST")).build()),
                        null, Optional.empty(), Map.of(), 0, 10000)
                .getTotalCount())
                .isEqualTo(2);

        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("is blocked by"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .statuses(List.of("UNKNOWN"))
                        .links(List.of("is blocked by"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(0);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("is blocked by"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_created)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("is blocked by"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .statuses(List.of("BACKLOG"))
                        .links(List.of("is blocked by"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .statuses(List.of("BACKLOG"))
                        .parentStoryPoints(Map.of("$gt", "0", "$lt", "0"))
                        .links(List.of("is blocked by"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(0);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("is blocked by"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .calculation(JiraIssuesFilter.CALCULATION.age)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("is blocked by"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .calculation(JiraIssuesFilter.CALCULATION.age)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("is blocked by"))
                        .statuses(List.of("BACKLOG"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .calculation(JiraIssuesFilter.CALCULATION.age)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("is blocked by"))
                        .statuses(List.of("BACKLOG"))
                        .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .calculation(JiraIssuesFilter.CALCULATION.age)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("is blocked by"))
                        .statuses(List.of("BACKLOG"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("is blocked by"))
                        .statuses(List.of("BACKLOG"))
                        .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .calculation(JiraIssuesFilter.CALCULATION.assign_to_resolve)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(0);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("is blocked by", "blocks"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .excludeLinks(List.of("is blocked by"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .excludeLinks(List.of("is blocked by", "blocks"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(1);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("invalid"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(0);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .keys(List.of("TEST-881", "TEST-872", "TEST-96"))
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .keys(List.of("TEST-881", "TEST-872", "TEST-96"))
                        .links(List.of("blocks", "is blocked by", "is cloned by", "clones"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .keys(List.of("TEST-881", "TEST-872", "TEST-96"))
                        .links(List.of("blocks", "is blocked by", "is cloned by", "clones"))
                        .sprintIds(List.of("1", "2", "3", "4"))
                        .sprintNames(List.of("sprint1", "sprint2", "sprint3", "sprint4"))
                        .ingestedAt(ingestedAt)
                        .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time))
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(3);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .keys(List.of("TEST-881", "TEST-872", "TEST-96"))
                        .links(List.of("blocks", "is blocked by", "is cloned by", "clones"))
                        .sprintIds(List.of("1", "2", "3", "4"))
                        .sprintNames(List.of("sprint1", "sprint2", "sprint3", "sprint4"))
                        .ingestedAt(ingestedAt)
                        .parentStoryPoints(Map.of("$gt", "0", "$lt", "0"))
                        .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time))
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(0);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .keys(List.of("TEST-881", "TEST-872", "TEST-96", "LEV-995"))
                        .excludeLinks(List.of("blocks", "is blocked by", "is cloned by", "clones"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(0);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .keys(List.of("TEST-881", "TEST-872", "TEST-96"))
                        .links(List.of("blocks", "is blocked by", "is cloned by", "clones"))
                        .sprintIds(List.of("1", "2", "3", "4"))
                        .sprintNames(List.of("sprint1", "sprint2", "sprint3", "sprint4"))
                        .versions(List.of("New Version 1", "New Version 2", "New Version 3"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(1);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .keys(List.of("TEST-881", "TEST-872", "TEST-96", "LEV-995"))
                        .links(List.of("blocks", "is blocked by", "is cloned by", "clones"))
                        .versions(List.of("New Version 1", "New Version 2", "New Version 3"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.issue_type)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(1);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("blocks", "is blocked by", "is cloned by", "clones"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .calculation(JiraIssuesFilter.CALCULATION.hops)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("blocks", "is blocked by", "is cloned by", "clones"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .calculation(JiraIssuesFilter.CALCULATION.bounces)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("blocks", "is blocked by", "is cloned by", "clones"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .calculation(JiraIssuesFilter.CALCULATION.story_points)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("blocks", "is blocked by", "is cloned by", "clones"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("blocks", "is blocked by", "is cloned by", "clones"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .calculation(JiraIssuesFilter.CALCULATION.response_time)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(2);
        assertThat(jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .links(List.of("blocks", "is blocked by", "is cloned by", "clones"))
                        .ingestedAt(ingestedAt)
                        .across(JiraIssuesFilter.DISTINCT.status)
                        .calculation(JiraIssuesFilter.CALCULATION.stage_times_report)
                        .build(), false, null, null, Map.of())
                .getRecords().size()).isEqualTo(2);
    }

    @Test
    public void testAsOfStatus() throws SQLException {

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().sprintIds(List.of("4")).names(List.of("sprint4")).build(),
                        JiraIssuesFilter.builder()
                                .keys(List.of("LEV-998"))
                                .build(),
                        null, Map.of(), 0, 10000)
                .getRecords().get(0).getStatuses())
                .hasSize(2);

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().sprintIds(List.of("4")).names(List.of("sprint4")).build(),
                        JiraIssuesFilter.builder()
                                .keys(List.of("LEV-998"))
                                .build(),
                        null, Map.of(), 0, 10000)
                .getRecords().get(0).getStatus())
                .isEqualTo("DONE");

        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().sprintIds(List.of("4")).names(List.of("sprint4")).build(),
                        JiraIssuesFilter.builder()
                                .keys(List.of("LEV-998"))
                                .build(),
                        null, Map.of(), 0, 10000)
                .getRecords().get(0).getAsOfStatus())
                .isEqualTo("_UNKNOWN_");
    }

    @Test
    @Ignore
    public void testStageBounce() throws SQLException {
        DbListResponse<DbAggregationResult> aggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .ingestedAt(ingestedAt)
                .stages(List.of("to do"))
                .across(JiraIssuesFilter.DISTINCT.priority)
                .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                .build(), false, null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(3);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("HIGHEST", "HIGH", "MEDIUM");
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();

        aggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .ingestedAt(ingestedAt)
                .stages(List.of("to do"))
                .across(JiraIssuesFilter.DISTINCT.project)
                .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                .build(), false, null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(3);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("LEV", "MT", "PROP");
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();

        aggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .ingestedAt(ingestedAt)
                .stages(List.of("done"))
                .across(JiraIssuesFilter.DISTINCT.project)
                .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                .build(), false, null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(3);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("LEV", "TS", "PROP");
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();

        aggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .ingestedAt(ingestedAt)
                .stages(List.of("BACKLOG"))
                .across(JiraIssuesFilter.DISTINCT.project)
                .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                .build(), false, null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("TEST");
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();

        aggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .ingestedAt(ingestedAt)
                .stages(List.of("BACKLOG"))
                .across(JiraIssuesFilter.DISTINCT.stage)
                .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                .build(), false, null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("BACKLOG");
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();

        aggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .ingestedAt(ingestedAt)
                .stages(List.of("BACKLOG", "done"))
                .across(JiraIssuesFilter.DISTINCT.stage)
                .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                .build(), false, null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(2);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("BACKLOG", "DONE");
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();

        aggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .ingestedAt(ingestedAt)
                .stages(List.of("BACKLOG", "done"))
                .across(JiraIssuesFilter.DISTINCT.issue_created)
                .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                .build(), false, null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(6);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();

        aggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .ingestedAt(ingestedAt)
                .stages(List.of("BACKLOG", "done"))
                .components(List.of("ui-levelops"))
                .across(JiraIssuesFilter.DISTINCT.component)
                .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                .build(), false, null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("ui-levelops");
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        aggregationResultDbListResponse = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.project)
                        .ingestedAt(ingestedAt)
                        .stages(List.of("DONE"))
                        .hygieneCriteriaSpecs(Map.of())
                        .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                        .build(), List.of(JiraIssuesFilter.DISTINCT.assignee), null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(3);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("LEV", "TS", "PROP");
        assertThat(aggregationResultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        String userId1 = userIdOf("Meghana Dwarakanath").isPresent() ? userIdOf("Meghana Dwarakanath").get() : "";
        assertThat(aggregationResultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())//key is null in stacks.
        ).containsAnyOf(userId1);

        aggregationResultDbListResponse = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.project)
                        .ingestedAt(ingestedAt)
                        .stages(List.of("DONE"))
                        .hygieneCriteriaSpecs(Map.of())
                        .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                        .build(), List.of(JiraIssuesFilter.DISTINCT.stage), null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(3);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("LEV", "TS", "PROP");

        aggregationResultDbListResponse = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.project)
                        .ingestedAt(ingestedAt)
                        .stages(List.of("DONE"))
                        .hygieneCriteriaSpecs(Map.of())
                        .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                        .build(), List.of(JiraIssuesFilter.DISTINCT.issue_created), null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(3);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("LEV", "TS", "PROP");

        aggregationResultDbListResponse = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.project)
                        .ingestedAt(ingestedAt)
                        .stages(List.of("DONE"))
                        .hygieneCriteriaSpecs(Map.of())
                        .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                        .build(), List.of(JiraIssuesFilter.DISTINCT.status_category), null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(3);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("LEV", "TS", "PROP");
        assertThat(aggregationResultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);

        assertThat(aggregationResultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsAnyOf("Done", "To do");

        aggregationResultDbListResponse = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.reporter)
                        .ingestedAt(ingestedAt)
                        .stages(List.of("DONE"))
                        .hygieneCriteriaSpecs(Map.of())
                        .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                        .build(), List.of(JiraIssuesFilter.DISTINCT.issue_updated), null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(1);
        userId1 = userIdOf("Meghana Dwarakanath").isPresent() ? userIdOf("Meghana Dwarakanath").get() : "";
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(userId1);
        assertThat(aggregationResultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(4);

        aggregationResultDbListResponse = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.component)
                        .ingestedAt(ingestedAt)
                        .stages(List.of("DONE"))
                        .hygieneCriteriaSpecs(Map.of())
                        .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                        .build(), List.of(JiraIssuesFilter.DISTINCT.first_assignee), null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("ui-levelops");
        assertThat(aggregationResultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        userId1 = userIdOf("Meghana Dwarakanath").isPresent() ? userIdOf("Meghana Dwarakanath").get() : "";
        assertThat(aggregationResultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsAnyOf(userId1);

        aggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .ingestedAt(ingestedAt)
                .excludeStages(List.of("BACKLOG"))
                .across(JiraIssuesFilter.DISTINCT.project)
                .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                .build(), false, null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(9);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("LEV", "MT", "MT", "LEV", "PROP", "PROP", "LEV", "LEV", "TS");
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getStage).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("TO DO", "TO DO", "IN PROGRESS", "DONE", "DONE", "TO DO", "IN PROGRESS", "_UNKNOWN_", "DONE");
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();

        aggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .ingestedAt(ingestedAt)
                .excludeStages(List.of("Done", "backlog"))
                .across(JiraIssuesFilter.DISTINCT.priority)
                .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                .build(), false, null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(6);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("HIGHEST", "HIGHEST", "HIGHEST", "HIGH", "MEDIUM", "MEDIUM");
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getStage).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("TO DO", "IN PROGRESS", "_UNKNOWN_", "TO DO", "TO DO", "IN PROGRESS");
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();

        aggregationResultDbListResponse = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.project)
                        .ingestedAt(ingestedAt)
                        .excludeStages(List.of("BACKLOG", "DONE", "TO DO"))
                        .hygieneCriteriaSpecs(Map.of())
                        .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                        .build(), List.of(JiraIssuesFilter.DISTINCT.first_assignee), null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(3);
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("MT", "LEV", "LEV");
        assertThat(aggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getStage).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("IN PROGRESS", "IN PROGRESS", "_UNKNOWN_");

        aggregationResultDbListResponse = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(JiraIssuesFilter.DISTINCT.none)
                        .ingestedAt(ingestedAt)
                        .stages(List.of("BACKLOG", "DONE", "TO DO", "IN PROGRESS"))
                        .hygieneCriteriaSpecs(Map.of())
                        .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                        .build(), null, null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(4);

        aggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .stages(List.of("BACKLOG", "TO DO"))
                .across(JiraIssuesFilter.DISTINCT.trend)
                .calculation(JiraIssuesFilter.CALCULATION.stage_bounce_report)
                .build(), false, null, null, Map.of());
        assertThat(aggregationResultDbListResponse.getTotalCount()).isEqualTo(6);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCustomFieldQuery() throws IOException, SQLException {
        var request = ResourceUtils.getResourceAsObject("json/databases/jira/custom_field_array_request.json", DefaultListRequest.class);
        var issueFilter = JiraIssuesFilter.builder()
                .across(JiraIssuesFilter.DISTINCT.trend)
                .calculation(JiraIssuesFilter.CALCULATION.age)
                .aggInterval("week")
                .customStacks(List.of("customfield_13264"))
                .snapshotRange(ImmutablePair.of(Instant.parse("2021-08-02T08:34:23-08:00").getEpochSecond(), Instant.parse("2021-11-02T07:23:34-08:00").getEpochSecond()))
                .integrationIds(request.<String>getFilterValueAsList("integration_ids").get())
                .excludeCustomFields((Map<String, Object>) request.<String, Object>getFilterValueAsMap("exclude").get().get("custom_fields"))
                .build();
        // var issueFilter = jiraFilterParser.createFilter(company, request, null, null, null, null, false, false);
        var jiraSprintFilter = JiraSprintFilter.fromDefaultListRequest(request);
        jiraIssueService.list(company, jiraSprintFilter, issueFilter, Optional.empty(), null, Optional.empty(), SortingConverter.fromFilter(List.of()), 0, 10);
        jiraIssueService.stackedGroupBy(company, issueFilter, List.of(JiraIssuesFilter.DISTINCT.custom_field), null, null, Map.of());
    }

    @Test
    public void testAgeReportByYear() {
        // years
        var filter = JiraIssuesFilter.builder()
                .aggInterval("year")
                .snapshotRange(ImmutablePair.of(Instant.parse("2020-01-02T12:34:23-08:00").getEpochSecond(), Instant.parse("2021-11-02T07:23:34-08:00").getEpochSecond()))
                .build();
        Set<Long> intervals = null;

        // get age intervals
        var rangeFrom = filter.getSnapshotRange().getLeft();
        var rangeTo = filter.getSnapshotRange().getRight();
        intervals = AggTimeQueryHelper.getIngestedAtSetForInterval(rangeFrom, rangeTo, filter.getAggInterval());

        Assertions.assertThat(intervals).isNotNull();
        Assertions.assertThat(intervals).containsExactlyInAnyOrder(Instant.parse("2021-01-01T00:00:00-00:00").getEpochSecond());

        filter = JiraIssuesFilter.builder()
                .aggInterval("year")
                .snapshotRange(ImmutablePair.of(Instant.parse("2019-01-01T08:34:23-08:00").getEpochSecond(), Instant.parse("2021-11-02T07:23:34-08:00").getEpochSecond()))
                .build();

        rangeFrom = filter.getSnapshotRange().getLeft();
        rangeTo = filter.getSnapshotRange().getRight();
        intervals = AggTimeQueryHelper.getIngestedAtSetForInterval(rangeFrom, rangeTo, filter.getAggInterval());

        Assertions.assertThat(intervals).isNotNull();
        Assertions.assertThat(intervals).containsExactlyInAnyOrder(
                Instant.parse("2019-01-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2020-01-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-01-01T00:00:00-00:00").getEpochSecond());
    }

    @Test
    public void testAgeReportByQuarter() {
        // quarters
        var filter = JiraIssuesFilter.builder()
                .aggInterval("quarter")
                .snapshotRange(ImmutablePair.of(Instant.parse("2021-01-07T12:34:23-08:00").getEpochSecond(), Instant.parse("2021-11-02T07:23:34-08:00").getEpochSecond()))
                .build();
        var rangeFrom = filter.getSnapshotRange().getLeft();
        var rangeTo = filter.getSnapshotRange().getRight();
        var intervals = AggTimeQueryHelper.getIngestedAtSetForInterval(rangeFrom, rangeTo, filter.getAggInterval());

        Assertions.assertThat(intervals).isNotNull();
        Assertions.assertThat(intervals).containsExactlyInAnyOrder(
                Instant.parse("2021-04-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-07-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-10-01T00:00:00-00:00").getEpochSecond());

        filter = JiraIssuesFilter.builder()
                .aggInterval("quarter")
                .snapshotRange(ImmutablePair.of(Instant.parse("2020-07-01T08:34:23-08:00").getEpochSecond(), Instant.parse("2021-11-02T07:23:34-08:00").getEpochSecond()))
                .build();

        rangeFrom = filter.getSnapshotRange().getLeft();
        rangeTo = filter.getSnapshotRange().getRight();
        intervals = AggTimeQueryHelper.getIngestedAtSetForInterval(rangeFrom, rangeTo, filter.getAggInterval());

        Assertions.assertThat(intervals).isNotNull();
        Assertions.assertThat(intervals).containsExactlyInAnyOrder(
                Instant.parse("2020-07-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2020-10-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-01-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-04-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-07-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-10-01T00:00:00-00:00").getEpochSecond());
    }

    @Test
    public void testAgeReportByMonth() {
        // months
        var filter = JiraIssuesFilter.builder()
                .aggInterval("month")
                .snapshotRange(ImmutablePair.of(Instant.parse("2021-10-07T12:34:23-08:00").getEpochSecond(), Instant.parse("2021-11-02T07:23:34-08:00").getEpochSecond()))
                .build();
        var rangeFrom = filter.getSnapshotRange().getLeft();
        var rangeTo = filter.getSnapshotRange().getRight();
        var intervals = AggTimeQueryHelper.getIngestedAtSetForInterval(rangeFrom, rangeTo, filter.getAggInterval());

        Assertions.assertThat(intervals).isNotNull();
        Assertions.assertThat(intervals).containsExactlyInAnyOrder(Instant.parse("2021-11-01T00:00:00-00:00").getEpochSecond());

        filter = JiraIssuesFilter.builder()
                .aggInterval("month")
                .snapshotRange(ImmutablePair.of(Instant.parse("2021-08-02T08:34:23-08:00").getEpochSecond(), Instant.parse("2021-11-02T07:23:34-08:00").getEpochSecond()))
                .build();

        rangeFrom = filter.getSnapshotRange().getLeft();
        rangeTo = filter.getSnapshotRange().getRight();
        intervals = AggTimeQueryHelper.getIngestedAtSetForInterval(rangeFrom, rangeTo, filter.getAggInterval());

        Assertions.assertThat(intervals).isNotNull();
        Assertions.assertThat(intervals).containsExactlyInAnyOrder(
                Instant.parse("2021-09-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-10-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-11-01T00:00:00-00:00").getEpochSecond());


        filter = JiraIssuesFilter.builder()
                .aggInterval("month")
                .snapshotRange(ImmutablePair.of(Instant.parse("2020-08-02T08:34:23-08:00").getEpochSecond(), Instant.parse("2021-11-02T07:23:34-08:00").getEpochSecond()))
                .build();

        rangeFrom = filter.getSnapshotRange().getLeft();
        rangeTo = filter.getSnapshotRange().getRight();
        intervals = AggTimeQueryHelper.getIngestedAtSetForInterval(rangeFrom, rangeTo, filter.getAggInterval());

        Assertions.assertThat(intervals).isNotNull();
        Assertions.assertThat(intervals).containsExactlyInAnyOrder(
                Instant.parse("2020-09-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2020-10-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2020-11-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2020-12-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-01-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-02-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-03-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-04-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-05-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-06-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-07-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-08-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-09-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-10-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-11-01T00:00:00-00:00").getEpochSecond());
    }

    @Test
    public void testAgeReportByWeek() {
        // weeks
        var filter = JiraIssuesFilter.builder()
                .aggInterval("week")
                .snapshotRange(ImmutablePair.of(Instant.parse("2021-08-02T08:34:23-08:00").getEpochSecond(), Instant.parse("2021-11-02T07:23:34-08:00").getEpochSecond()))
                .build();

        var rangeFrom = filter.getSnapshotRange().getLeft();
        var rangeTo = filter.getSnapshotRange().getRight();
        var intervals = AggTimeQueryHelper.getIngestedAtSetForInterval(rangeFrom, rangeTo, filter.getAggInterval());

        Assertions.assertThat(intervals).isNotNull();
        Assertions.assertThat(intervals).containsExactlyInAnyOrder(
                Instant.parse("2021-08-02T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-08-09T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-08-16T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-08-23T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-08-30T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-09-06T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-09-13T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-09-20T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-09-27T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-10-04T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-10-11T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-10-18T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-10-25T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-11-01T00:00:00-00:00").getEpochSecond());

        filter = JiraIssuesFilter.builder()
                .aggInterval("week")
                .snapshotRange(ImmutablePair.of(Instant.parse("2021-08-03T08:34:23-08:00").getEpochSecond(), Instant.parse("2021-11-02T07:23:34-08:00").getEpochSecond()))
                .build();

        rangeFrom = filter.getSnapshotRange().getLeft();
        rangeTo = filter.getSnapshotRange().getRight();
        intervals = AggTimeQueryHelper.getIngestedAtSetForInterval(rangeFrom, rangeTo, filter.getAggInterval());

        Assertions.assertThat(intervals).isNotNull();
        Assertions.assertThat(intervals).containsExactlyInAnyOrder(
                Instant.parse("2021-08-09T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-08-16T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-08-23T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-08-30T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-09-06T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-09-13T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-09-20T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-09-27T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-10-04T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-10-11T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-10-18T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-10-25T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-11-01T00:00:00-00:00").getEpochSecond());


        filter = JiraIssuesFilter.builder()
                .aggInterval("week")
                .snapshotRange(ImmutablePair.of(Instant.parse("2020-12-01T08:34:23-08:00").getEpochSecond(), Instant.parse("2021-03-02T07:23:34-08:00").getEpochSecond()))
                .build();

        rangeFrom = filter.getSnapshotRange().getLeft();
        rangeTo = filter.getSnapshotRange().getRight();
        intervals = AggTimeQueryHelper.getIngestedAtSetForInterval(rangeFrom, rangeTo, filter.getAggInterval());

        Assertions.assertThat(intervals).isNotNull();
        Assertions.assertThat(intervals).containsExactlyInAnyOrder(
                Instant.parse("2020-12-07T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2020-12-14T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2020-12-21T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2020-12-28T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-01-04T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-01-11T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-01-18T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-01-25T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-02-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-02-08T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-02-15T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-02-22T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-03-01T00:00:00-00:00").getEpochSecond());


        filter = JiraIssuesFilter.builder()
                .aggInterval("week")
                .snapshotRange(ImmutablePair.of(Instant.parse("2020-11-30T08:34:23-08:00").getEpochSecond(), Instant.parse("2021-03-02T07:23:34-08:00").getEpochSecond()))
                .build();

        rangeFrom = filter.getSnapshotRange().getLeft();
        rangeTo = filter.getSnapshotRange().getRight();
        intervals = AggTimeQueryHelper.getIngestedAtSetForInterval(rangeFrom, rangeTo, filter.getAggInterval());

        Assertions.assertThat(intervals).isNotNull();
        Assertions.assertThat(intervals).containsExactlyInAnyOrder(
                Instant.parse("2020-11-30T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2020-12-07T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2020-12-14T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2020-12-21T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2020-12-28T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-01-04T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-01-11T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-01-18T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-01-25T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-02-01T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-02-08T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-02-15T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-02-22T00:00:00-00:00").getEpochSecond(),
                Instant.parse("2021-03-01T00:00:00-00:00").getEpochSecond());
    }

    @Test
    @Ignore
    public void testDrilldownAndGroupByNumbers() throws SQLException {
        DbListResponse<DbAggregationResult> resultDbListResponse = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .issueUpdatedRange(ImmutablePair.of(1590314400L, 1590364800L))
                .across(JiraIssuesFilter.DISTINCT.issue_updated)
                .calculation(JiraIssuesFilter.CALCULATION.assign_to_resolve)
                .build(), false, null, null, Map.of());
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        DbListResponse<DbJiraIssue> listResponse = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("first_assignee", false))
                        .issueUpdatedRange(ImmutablePair.of(1590314400L, 1590364800L))
                        .across(JiraIssuesFilter.DISTINCT.issue_updated)
                        .calculation(JiraIssuesFilter.CALCULATION.assign_to_resolve)
                        .build(), null, Map.of(), 0, 10000);
        Assertions.assertThat(Long.valueOf(listResponse.getTotalCount())).isEqualTo(resultDbListResponse.getRecords()
                .stream().map(DbAggregationResult::getTotalTickets).collect(Collectors.toList()).get(0));
        resultDbListResponse = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .integrationIds(List.of("1"))
                .hygieneCriteriaSpecs(Map.of())
                .ingestedAt(ingestedAt)
                .issueCreatedRange(ImmutablePair.of(1590314400L, 1590364800L))
                .across(JiraIssuesFilter.DISTINCT.issue_updated)
                .calculation(JiraIssuesFilter.CALCULATION.assign_to_resolve)
                .build(), false, null, null, Map.of());
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        listResponse = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .missingFields(Map.of("first_assignee", false))
                        .issueCreatedRange(ImmutablePair.of(1590314400L, 1590364800L))
                        .across(JiraIssuesFilter.DISTINCT.issue_updated)
                        .calculation(JiraIssuesFilter.CALCULATION.assign_to_resolve)
                        .build(), null, Map.of(), 0, 10000);
        Assertions.assertThat(Long.valueOf(listResponse.getTotalCount())).isEqualTo(resultDbListResponse.getRecords()
                .stream().map(DbAggregationResult::getTotalTickets).collect(Collectors.toList()).get(0));
    }

    @Test
    public void testIncludeSolveTime() throws SQLException {
        DbListResponse<DbJiraIssue> listResponse = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .build(), null, Map.of(), 0, 10000);
        Assertions.assertThat(listResponse.getRecords().stream().map(DbJiraIssue::getSolveTime)).containsOnlyNulls();

        listResponse = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .includeSolveTime(true)
                        .build(), null, Map.of(), 0, 10000);
        Assertions.assertThat(listResponse.getRecords().stream().map(DbJiraIssue::getSolveTime)).isNotNull();
    }

    @Test
    public void testListMethodsForJiraVersions() throws SQLException {
        insertVersions();
        List<DbJiraVersion> versionsForIssue = jiraIssueService.getVersionsForIssues(company, List.of("v1", "v2"), List.of(1),
                List.of("P1"));
        Assertions.assertThat(versionsForIssue
                .stream().map(DbJiraVersion::getName).collect(Collectors.toList())).containsExactlyInAnyOrder("v1", "v2");
        versionsForIssue = jiraIssueService.getVersionsForIssues(company, List.of("v4"), List.of(1), List.of("P1"));
        Assertions.assertThat(versionsForIssue
                .stream().map(DbJiraVersion::getName).collect(Collectors.toList())).isEmpty();
        versionsForIssue = jiraIssueService.getVersionsForIssues(company, List.of("v1"), List.of(1), List.of("P1"));
        Assertions.assertThat(versionsForIssue
                .stream().map(DbJiraVersion::getName).collect(Collectors.toList())).containsExactlyInAnyOrder("v1");
        versionsForIssue = jiraIssueService.getVersionsForIssues(company, List.of("v1"), List.of(2), List.of("P1"));
        Assertions.assertThat(versionsForIssue
                .stream().map(DbJiraVersion::getName).collect(Collectors.toList())).isEmpty();
        versionsForIssue = jiraIssueService.getVersionsForIssues(company, List.of("v1", "v2", "v3"), List.of(1), List.of("P1"));
        Assertions.assertThat(versionsForIssue
                .stream().map(DbJiraVersion::getName).collect(Collectors.toList())).containsExactlyInAnyOrder("v1", "v2", "v3");

    }

    @Test
    public void testListMethodsForJiraStoryPoints() throws SQLException {
        insertStoryPoints();
        Map<Pair<String, String>, List<DbJiraStoryPoints>> storyPointsForIssues = jiraIssueService.getStoryPointsForIssues(company, List.of("key1"), List.of(1));
        Assertions.assertThat(storyPointsForIssues.get(Pair.of("1", "key1"))
                .stream().map(DbJiraStoryPoints::getIssueKey).collect(Collectors.toList())).containsExactlyInAnyOrder("key1", "key1");
        Assertions.assertThat(storyPointsForIssues.get(Pair.of("1", "key1"))
                .stream().map(DbJiraStoryPoints::getStoryPoints).collect(Collectors.toList())).containsExactlyInAnyOrder(1, 2);
        Assertions.assertThat(storyPointsForIssues.get(Pair.of("2", "key1"))).isNull();
        storyPointsForIssues = jiraIssueService.getStoryPointsForIssues(company, List.of("key"), List.of(1));
        Assertions.assertThat(storyPointsForIssues.get(Pair.of("1", "key"))).isNull();
        storyPointsForIssues = jiraIssueService.getStoryPointsForIssues(company, List.of("key1", "key2"), List.of(1));
        Assertions.assertThat(storyPointsForIssues.size()).isEqualTo(2);
        Assertions.assertThat(storyPointsForIssues.get(Pair.of("1", "key2")).size()).isEqualTo(1);
        storyPointsForIssues = jiraIssueService.getStoryPointsForIssues(company, List.of("key1", "key", "key2"), List.of(1));
        Assertions.assertThat(storyPointsForIssues.size()).isEqualTo(2);

    }

    @Test
    public void testListMethodsForJiraSalesforceCases() throws SQLException {
        insertSalesforceCases();
        Map<Pair<String, String>, List<DbJiraSalesforceCase>> salesforceCases = jiraIssueService
                .getSalesforceCaseForIssues(company, List.of("key1"), List.of(2));
        Assertions.assertThat(salesforceCases.get(Pair.of("2", "key1"))
                .stream().map(DbJiraSalesforceCase::getIssueKey).collect(Collectors.toList())).containsExactlyInAnyOrder("key1", "key1");
        Assertions.assertThat(salesforceCases.get(Pair.of("2", "key1"))
                .stream().map(DbJiraSalesforceCase::getFieldValue).collect(Collectors.toList())).containsExactlyInAnyOrder("f1", "f2");
        Assertions.assertThat(salesforceCases.get(Pair.of("1", "key1"))).isNull();
        salesforceCases = jiraIssueService.getSalesforceCaseForIssues(company, List.of("key1", "key2"), List.of(2));
        Assertions.assertThat(salesforceCases.size()).isEqualTo(1);

    }

    @Test
    public void testListMethodsForJiraLinks() throws SQLException {
        insertLinks();
        Map<Pair<String, String>, List<DbJiraLink>> links = jiraIssueService.getLinksForIssues(company, List.of("key1"), List.of(1));
        Assertions.assertThat(links.get(Pair.of("1", "key1"))
                .stream().map(DbJiraLink::getFromIssueKey).collect(Collectors.toList())).containsExactlyInAnyOrder("key1", "key1");
        Assertions.assertThat(links.get(Pair.of("1", "key1"))
                .stream().map(DbJiraLink::getToIssueKey).collect(Collectors.toList())).containsExactlyInAnyOrder("key_to1", "key_to2");
        Assertions.assertThat(links.get(Pair.of("2", "key1"))).isNull();
        links = jiraIssueService.getLinksForIssues(company, List.of("key1", "key2"), List.of(1));
        Assertions.assertThat(links.size()).isEqualTo(2);
        Assertions.assertThat(links.get(Pair.of("1", "key2"))
                .stream().map(DbJiraLink::getRelation).collect(Collectors.toList())).containsExactlyInAnyOrder("relation");

    }

    private void insertVersions() throws SQLException {
        DbJiraVersion v1 = DbJiraVersion.builder()
                .versionId(1)
                .name("v1")
                .integrationId(1)
                .projectId(100)
                .build();
        DbJiraVersion v2 = DbJiraVersion.builder()
                .versionId(2)
                .name("v2")
                .integrationId(1)
                .projectId(100)
                .build();
        DbJiraVersion v3 = DbJiraVersion.builder()
                .versionId(3)
                .name("v3")
                .integrationId(1)
                .projectId(100)
                .build();
        jiraIssueService.insertJiraVersion(company, v1);
        jiraIssueService.insertJiraVersion(company, v2);
        jiraIssueService.insertJiraVersion(company, v3);
    }

    private void insertStoryPoints() throws SQLException {
        DbJiraStoryPoints storyPoints1 = DbJiraStoryPoints.builder()
                .storyPoints(1)
                .issueKey("key1")
                .integrationId("1")
                .startTime(1649944549L)
                .endTime(1649944549L)
                .build();
        DbJiraStoryPoints storyPoints2 = DbJiraStoryPoints.builder()
                .storyPoints(2)
                .issueKey("key1")
                .integrationId("1")
                .startTime(1649949549L)
                .endTime(1649949549L)
                .build();
        DbJiraStoryPoints storyPoints3 = DbJiraStoryPoints.builder()
                .storyPoints(2)
                .issueKey("key2")
                .integrationId("1")
                .startTime(1649949549L)
                .endTime(1649949549L)
                .build();
        jiraIssueStoryPointsDatabaseService.insert(company, storyPoints1);
        jiraIssueStoryPointsDatabaseService.insert(company, storyPoints2);
        jiraIssueStoryPointsDatabaseService.insert(company, storyPoints3);
    }

    private void insertSalesforceCases() throws SQLException {

        DbJiraIssue issue = DbJiraIssue.builder()
                .key("key1")
                .salesforceFields(Map.of("k1", List.of("f1", "f2")))
                .integrationId("2")
                .ingestedAt(ingestedAt)
                .project("p1")
                .summary("summary")
                .components(List.of("comp1", "comp2"))
                .labels(List.of("label1", "label2"))
                .fixVersions(List.of())
                .sprintIds(List.of())
                .descSize(4)
                .priority("LOW")
                .reporter("r1")
                .status("s1")
                .issueType("issue1")
                .hops(1)
                .bounces(0)
                .numAttachments(2)
                .issueCreatedAt(Long.valueOf(1613658414))
                .issueUpdatedAt(Long.valueOf(1613658414))
                .build();
        jiraIssueService.insert(company, issue);
    }

    private void insertLinks() {
        jiraIssueService.insertJiraLinkedIssueRelation(company, "1", "key1",
                "key_to1", "duplicate");
        jiraIssueService.insertJiraLinkedIssueRelation(company, "1",
                "key1", "key_to2", "duplicate");
        jiraIssueService.insertJiraLinkedIssueRelation(company, "1",
                "key2", "key_to1", "relation");
    }

    @Test
    public void testJiraIssuesList() throws SQLException {
        List<DbJiraIssue> list = jiraIssueService.listJiraIssues(company, JiraIssuesFilter.builder().build(), Map.of("issue_created_at", SortingOrder.ASC), 0, 10);
        Assert.assertNotNull(list);
        Assertions.assertThat(list.size()).isEqualTo(10);

        /*
        select ingested_at as i, integration_id as it, count(*) as cnt from test.jira_issues group by i, it order by i desc;
             i      | it | cnt
        ------------+----+-----
         1673942400 |  1 |  23
         1673856000 |  1 |  23
         1673769600 |  1 |  23
         */

        //Verify that for one ingested_at there are 10 tickets where integration_id = 1 & issue_updated_at > 1590342640
        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt).integrationIdByIssueUpdatedRange(Map.of(1, ImmutablePair.of(1590342640l, null)))
                .build();
        list = jiraIssueService.listJiraIssues(company, jiraIssuesFilter, Map.of("issue_updated_at", SortingOrder.ASC), 0, 30);
        Assert.assertNotNull(list);
        Assertions.assertThat(list.size()).isEqualTo(14);
        List<String> expectedKeys = List.of("LEV-997", "PROP-2222", "PROP-2223", "PROP-2224", "PROP-2225", "LEV-999", "LEV-1001", "LEV-1202", "LEV-1003", "LEV-1004", "LEV-1005", "TEST-872", "TEST-96", "TEST-881").stream().collect(Collectors.toList());
        Assertions.assertThat(list.stream().map(DbJiraIssue::getKey).collect(Collectors.toList())).isEqualTo(expectedKeys);

        //Verify that for one ingested_at there are 10 tickets where integration_id = 1 & issue_updated_at > 1590342640 & issue_updated_at < 1590342640
        jiraIssuesFilter = JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt).integrationIdByIssueUpdatedRange(Map.of(1, ImmutablePair.of(1590195115l, 1590349550l)))
                .build();
        list = jiraIssueService.listJiraIssues(company, jiraIssuesFilter, Map.of("issue_updated_at", SortingOrder.ASC), 0, 30);
        Assert.assertNotNull(list);
        Assertions.assertThat(list.size()).isEqualTo(11);
        expectedKeys = List.of("LEV-995", "LEV-998", "LEV-1000", "LEV-997", "PROP-2222", "PROP-2223", "PROP-2224", "PROP-2225", "LEV-999", "LEV-1001", "LEV-1202").stream().collect(Collectors.toList());
        Assertions.assertThat(list.stream().map(DbJiraIssue::getKey).collect(Collectors.toList())).isEqualTo(expectedKeys);

        //Verify that for one ingested_at there are 10 tickets where issue_updated_at is provided for integration_id 2, return all 23 tickets for integration_id=1
        jiraIssuesFilter = JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt).integrationIdByIssueUpdatedRange(Map.of(2, ImmutablePair.of(1590342640l, null)))
                .build();
        list = jiraIssueService.listJiraIssues(company, jiraIssuesFilter, Map.of("issue_updated_at", SortingOrder.ASC), 0, 30);
        Assert.assertNotNull(list);
        Assertions.assertThat(list.size()).isEqualTo(27);
    }

    @Test
    public void testStatusOverlap() {

        DbJiraStatus status1 = getDBJiraStatuse("BACKLOG", 1639172108l, 1639601547l);
        DbJiraStatus status2 = getDBJiraStatuse("BACKLOG", 1639601547l, 1664976969l);
        DbJiraStatus status3 = getDBJiraStatuse("IN DEVELOPMENT", 1664976969l, 1665584040l);
        DbJiraStatus status4 = getDBJiraStatuse("IN CODE REVIEW", 1665584040l, 1665591273l);
        DbJiraStatus status5 = getDBJiraStatuse("QA", 1665591273l, 1665594679l);

        DbJiraIssue issue = DbJiraIssue.builder()
                .key("PROP-100")
                .integrationId("1")
                .statuses(List.of(status1, status2, status3, status4, status5))
                .build();

        jiraIssueService.deleteAndUpdateStatuses(company, issue);
        boolean overlap = jiraIssueService.doIssueStatusOverlap(company, issue);
        Assertions.assertThat(overlap).isTrue();

        status1 = getDBJiraStatuse("BACKLOG", 1639172108l, 1664976969l);
        issue = issue.toBuilder()
                .statuses(List.of(status1, status3, status4, status5))
                .build();

        jiraIssueService.deleteAndUpdateStatuses(company, issue);
        overlap = jiraIssueService.doIssueStatusOverlap(company, issue);
        Assertions.assertThat(overlap).isFalse();
    }

    private DbJiraStatus getDBJiraStatuse(String status, long startTime, long endTime) {

        return DbJiraStatus.builder()
                .issueKey("PROP-100")
                .integrationId("1")
                .status(status)
                .createdAt(1665171102l)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }


    @Test
    public void missingCustomFieldsTest() throws SQLException {

        JiraIssuesFilter filter = JiraIssuesFilter.builder()
                .missingFields(Map.of("customfield_13264", true,
                        "customfield_12641", true,
                        "customfield_12746", true,
                        "customfield_10048", true,
                        "customfield_10149", true,
                        "customfield_20001", true,
                        "customfield_12716", true,
                        "customfield_10020", true,
                        "customfield_10105", true
                ))
                .build();

        List<DbJiraIssue> issues = jiraIssueService.list(company, JiraSprintFilter.builder().build(), filter, null, Map.of(), 0, 100).getRecords();

        Assertions.assertThat(issues).isNotNull();
        Assertions.assertThat(issues.size()).isEqualTo(27);
        for (int i = 0; i < issues.size(); i++) {

            Assertions.assertThat(issues.get(i).getCustomFields().keySet()).doesNotContain("customfield_13264",
                    "customfield_12641",
                    "customfield_12746",
                    "customfield_10048",
                    "customfield_10149",
                    "customfield_20001",
                    "customfield_12716",
                    "customfield_10020",
                    "customfield_10105");
        }
    }

}