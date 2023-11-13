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
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
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
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

public class JiraIssueTrendStackTest {

    private static final String company = "test";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static JiraIssueService jiraIssueService;
    private static Long ingestedAt;
    private static DbJiraIssue randomIssue;

    @BeforeClass
    public static void setup() throws Exception {
        final ObjectMapper m = DefaultObjectMapper.get();

        DataSource dataSource = DatabaseTestUtils.setUpDataSource(pg, company);

        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);

        final JiraFieldService jiraFieldService = new JiraFieldService(dataSource);
        final JiraProjectService jiraProjectService = new JiraProjectService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

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
        String input = ResourceUtils.getResourceAsString("json/databases/jiraissues_jun62020.json");
        PaginatedResponse<JiraIssue> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        Date currentTime = new Date();
        jiraFieldService.batchUpsert(company,
                List.of(DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("array")
                                .fieldKey("customfield_10048").fieldItems("user").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("string")
                                .fieldKey("customfield_12641").build(),
                        DbJiraField.builder().custom(true).name("hello").integrationId("1").fieldType("string")
                                .fieldKey("customfield_20001").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("string")
                                .fieldKey("customfield_12746").build()));
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
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime,
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
                    jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, "2", currentTime,
                            JiraIssueParser.JiraParserConfig.builder()
                                    .epicLinkField("customfield_10014")
                                    .storyPointsField("customfield_10030")
                                    .customFieldConfig(entries)
                                    .build()));
                }
                jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, "1",
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
                jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, "1",
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
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(1).name("sprint1")
                .integrationId(Integer.parseInt("1")).state("active")
                .startDate(1617290995L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(2).name("sprint2")
                .integrationId(Integer.parseInt("1")).state("closed")
                .startDate(1617290985L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(3).name("sprint3")
                .integrationId(Integer.parseInt("1")).state("closed")
                .startDate(1617290975L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build());

    }

    @Test
    public void testAcrossIssueCreatedStack() throws SQLException {
        List<DbAggregationResult> records = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .aggInterval("quarter")
                        .across(JiraIssuesFilter.DISTINCT.issue_created)
                        .integrationIds(List.of("1"))
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.priority),
                null, null, Map.of()).getRecords();

        Assert.assertNotNull(records);
        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder().key("1585724400").additionalKey("Q2-2020").totalTickets(11L).stacks(
                        List.of(DbAggregationResult.builder()
                                        .key("HIGHEST").additionalKey(null).totalTickets(3L).build(),
                                // DbAggregationResult.builder()
                                //         .key("HIGH").additionalKey(null).totalTickets(11L).build(),
                                DbAggregationResult.builder()
                                        .key("MEDIUM").additionalKey(null).totalTickets(8L).build())
                ).build(),
                DbAggregationResult.builder().key("1577865600").additionalKey("Q1-2020").totalTickets(9L).stacks(
                        List.of(
                                // DbAggregationResult.builder()
                                //         .key("HIGHEST").additionalKey(null).totalTickets(3L).build(),
                                DbAggregationResult.builder()
                                        .key("HIGH").additionalKey(null).totalTickets(2L).build(),
                                DbAggregationResult.builder()
                                        .key("MEDIUM").additionalKey(null).totalTickets(7L).build())
                ).build()
                // DbAggregationResult.builder().key("1569913200").additionalKey("Q4-2019").totalTickets(3L).stacks(
                //         List.of(DbAggregationResult.builder()
                //                 .key("MEDIUM").additionalKey(null).totalTickets(3L).build())
                // ).build()
        );
        Assert.assertEquals(expected.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()),
                records.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()));
        verifyRecords(records, expected);
    }

    //     @Test
    public void testIssueCreatedCustomStack() throws SQLException {
        List<DbAggregationResult> records = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .aggInterval("quarter")
                        .priorities(List.of("HIGHEST"))
                        .across(JiraIssuesFilter.DISTINCT.issue_created)
                        .integrationIds(List.of("1"))
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .customStacks(List.of("customfield_10020"))
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.custom_field),
                null, null, Map.of()).getRecords();

        Assert.assertNotNull(records);
        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder().key("1585724400").additionalKey("Q2-2020").totalTickets(3L).stacks(
                        List.of(DbAggregationResult.builder()
                                        .key("[\"Apr 27\"]").additionalKey(null).totalTickets(2L).build(),
                                DbAggregationResult.builder()
                                        .key("[\"Apr 27\", \"May 4\"]").additionalKey(null).totalTickets(1L).build())
                ).build()
                // DbAggregationResult.builder().key("1577865600").additionalKey("Q1-2020").totalTickets(3L).stacks(
                //         Collections.emptyList()).build()
        );
        Assert.assertEquals(expected.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()),
                records.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()));
        verifyRecords(records, expected);
    }

    //     @Test
    public void testAcrossIssueUpdatedStack() throws SQLException {
        List<DbAggregationResult> records = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .aggInterval("month")
                        .across(JiraIssuesFilter.DISTINCT.issue_updated)
                        .integrationIds(List.of("1"))
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.priority),
                null, null, Map.of()).getRecords();

        Assert.assertNotNull(records);
        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder().key("1588316400").additionalKey("5-2020").totalTickets(223L).stacks(
                        List.of(DbAggregationResult.builder()
                                        .key("HIGHEST").additionalKey(null).totalTickets(2L).build(),
                                DbAggregationResult.builder()
                                        .key("HIGH").additionalKey(null).totalTickets(9L).build(),
                                DbAggregationResult.builder()
                                        .key("MEDIUM").additionalKey(null).totalTickets(212L).build())
                ).build(),
                DbAggregationResult.builder().key("1585724400").additionalKey("4-2020").totalTickets(154L).stacks(
                        List.of(DbAggregationResult.builder()
                                        .key("HIGHEST").additionalKey(null).totalTickets(2L).build(),
                                DbAggregationResult.builder()
                                        .key("HIGH").additionalKey(null).totalTickets(3L).build(),
                                DbAggregationResult.builder()
                                        .key("MEDIUM").additionalKey(null).totalTickets(149L).build())
                ).build(),
                DbAggregationResult.builder().key("1583049600").additionalKey("3-2020").totalTickets(87L).stacks(
                        List.of(DbAggregationResult.builder()
                                        .key("HIGHEST").additionalKey(null).totalTickets(2L).build(),
                                DbAggregationResult.builder()
                                        .key("MEDIUM").additionalKey(null).totalTickets(85L).build())
                ).build(),
                DbAggregationResult.builder().key("1580544000").additionalKey("2-2020").totalTickets(36L).stacks(
                        List.of(DbAggregationResult.builder()
                                .key("MEDIUM").additionalKey(null).totalTickets(36L).build())
                ).build()
        );

        Assert.assertEquals(expected.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()),
                records.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()));
        verifyRecords(records, expected);
    }

    @Test
    public void testAcrossIssueResolvedStack() throws SQLException {
        List<DbAggregationResult> records = jiraIssueService.stackedGroupBy(company,
                JiraIssuesFilter.builder().hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(ingestedAt)
                        .aggInterval("quarter")
                        .across(JiraIssuesFilter.DISTINCT.issue_resolved)
                        .integrationIds(List.of("1"))
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .build(),
                List.of(JiraIssuesFilter.DISTINCT.status),
                null, null, Map.of()).getRecords();

        Assert.assertNotNull(records);
        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder().key("1585724400").additionalKey("Q2-2020").totalTickets(4L).stacks(
                        List.of(DbAggregationResult.builder()
                                        .key("DONE").additionalKey(null).totalTickets(4L).build()
                                // DbAggregationResult.builder()
                                //         .key("WONT DO").additionalKey(null).totalTickets(0L).build()
                        )
                ).build()
                // DbAggregationResult.builder().key("1577865600").additionalKey("Q1-2020").totalTickets(87L).stacks(
                //         List.of(DbAggregationResult.builder()
                //                 .key("DONE").additionalKey(null).totalTickets(87L).build())
                // ).build()
        );
        Assert.assertEquals(expected.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()),
                records.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()));
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
