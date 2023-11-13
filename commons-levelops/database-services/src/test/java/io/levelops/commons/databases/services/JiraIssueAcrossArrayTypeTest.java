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

public class JiraIssueAcrossArrayTypeTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static JiraIssueService jiraIssueService;
    private static DbJiraIssue randomIssue;
    private static Date currentTime;
    private static Long ingestedAt;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null) {
            return;
        }

        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        dataSource.getConnection().prepareStatement("CREATE OR REPLACE FUNCTION array_intersect(a1 anyarray, a2 anyarray) " +
                "RETURNS anyarray AS " +
                "$$ SELECT ARRAY( SELECT v from unnest($1) as v where v like ANY($2) ); $$ LANGUAGE sql;").execute();

        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        JiraFieldService jiraFieldService = jiraTestDbs.getJiraFieldService();
        JiraProjectService jiraProjectService = jiraTestDbs.getJiraProjectService();
        IntegrationService integrationService = jiraTestDbs.getIntegrationService();

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
        String input = ResourceUtils.getResourceAsString("json/databases/jiraissues_jun62020_old.json");
        PaginatedResponse<JiraIssue> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = new Date();
        jiraFieldService.batchUpsert(company,
                List.of(DbJiraField.builder().custom(true).name("yello").integrationId("1")
                                .fieldType("array").fieldKey("customfield_10048").fieldItems("user").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId("1")
                                .fieldType("string").fieldKey("customfield_12641").build(),
                        DbJiraField.builder().custom(true).name("hello").integrationId("1")
                                .fieldType("string").fieldKey("customfield_20001").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId("1")
                                .fieldType("string").fieldKey("customfield_12746").build()));
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
                .integrationId(Integer.parseInt("1")).state("active").startDate(1617290995L).endDate(1617290995L)
                .updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(2).name("sprint2")
                .integrationId(Integer.parseInt("1")).state("closed").startDate(1617290985L).endDate(1617290995L)
                .updatedAt(currentTime.toInstant().getEpochSecond()).build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder().sprintId(3).name("sprint3")
                .integrationId(Integer.parseInt("1")).state("closed").startDate(1617290975L).endDate(1617290995L)
                .updatedAt(currentTime.toInstant().getEpochSecond()).build());
    }

    @Test
    public void testFilterAcrossValue() throws SQLException {
        // region across labels, filter by labels, filter across value: false
        List<DbAggregationResult> aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.label)
                                .labels(List.of("plugins"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(false)
                                .build(), false, null, null, Map.of())
                .getRecords();

        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder().key("plugins").totalTickets(3L).build(),
                DbAggregationResult.builder().key("dast").totalTickets(1L).build(),
                DbAggregationResult.builder().key("sast").totalTickets(1L).build(),
                DbAggregationResult.builder().key("tools").totalTickets(1L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across labels, filter by labels, filter across value: true
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.label)
                                .labels(List.of("plugins"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(true)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("plugins").totalTickets(3L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across labels, filter by labels, filter across value: true, partial match
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.label)
                                .partialMatch(Map.of("labels", Map.of("$ends", "ast")))
                                .labels(List.of("plugins"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(true)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("plugins").totalTickets(1L).build(),
                DbAggregationResult.builder().key("dast").totalTickets(1L).build(),
                DbAggregationResult.builder().key("sast").totalTickets(1L).build()
        );
        verifyRecords(aggs, expected);

        // region across status, stacks labels, filter by labels, filter across value: false
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.status)
                                .labels(List.of("plugins"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(false)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.label), null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("DONE").totalTickets(3L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("plugins").totalTickets(3L).build(),
                                DbAggregationResult.builder().key("dast").totalTickets(1L).build(),
                                DbAggregationResult.builder().key("sast").totalTickets(1L).build(),
                                DbAggregationResult.builder().key("tools").totalTickets(1L).build()
                        )
                ).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across status, stacks labels, filter by labels, filter across value: true
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.status)
                                .labels(List.of("plugins"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(true)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.label), null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("DONE").totalTickets(3L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("plugins").totalTickets(3L).build()
                        )
                ).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across version, filter by versions, filter across value: false
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.version)
                                .versions(List.of("456"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(false)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("456").totalTickets(2L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across version, no filter, filter across value: true
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.version)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(true)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("123").totalTickets(2L).build(),
                DbAggregationResult.builder().key("456").totalTickets(2L).build(),
                DbAggregationResult.builder().key("789").totalTickets(3L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across version, no filter, filter across value: false
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.version)
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(false)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("456").totalTickets(2L).build(),
                DbAggregationResult.builder().key("789").totalTickets(3L).build(),
                DbAggregationResult.builder().key("123").totalTickets(2L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across version, filter by version, filter across value: true
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.version)
                                .versions(List.of("456"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(true)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("456").totalTickets(2L).build()
        );
        verifyRecords(aggs, expected);
        // endregion


        // region across version, filter by version partial match, filter across value: true
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.version)
                                .partialMatch(Map.of("versions", Map.of("$begins", "12")))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(true)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("456").totalTickets(1L).build(),
                DbAggregationResult.builder().key("789").totalTickets(2L).build(),
                DbAggregationResult.builder().key("123").totalTickets(2L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across fix_version, filter by fix_version, filter across value: false
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.fix_version)
                                .versions(List.of("456"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(false)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("456").totalTickets(1L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across fix_version, filter by fix_version, filter across value: true
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.fix_version)
                                .fixVersions(List.of("456"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(true)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("456").totalTickets(1L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across fix_version, filter by fix_version partial, filter across value: true
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.fix_version)
                                .partialMatch(Map.of("fix_versions", Map.of("$contains", "8")))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(true)
                                .build(), false, null, null, Map.of())
                .getRecords();
        expected = List.of(
                DbAggregationResult.builder().key("456").totalTickets(1L).build(),
                DbAggregationResult.builder().key("789").totalTickets(2L).build(),
                DbAggregationResult.builder().key("123").totalTickets(1L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across components, filter by components, filter across value: false
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.component)
                                .components(List.of("runbooks"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(false)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("runbooks").totalTickets(4L).build(),
                DbAggregationResult.builder().key("ingestion-levelops").totalTickets(2L).build(),
                DbAggregationResult.builder().key("commons-levelops").totalTickets(1L).build(),
                DbAggregationResult.builder().key("serverapi-levelops").totalTickets(1L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across components, filter by components, filter across value: true
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.component)
                                .components(List.of("runbooks"))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(true)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("runbooks").totalTickets(4L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across components, filter by components partial, filter across value: true
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.component)
                                .components(List.of("runbooks"))
                                .partialMatch(Map.of("components", Map.of("$begins", "commons")))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(true)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("runbooks").totalTickets(1L).build(),
                DbAggregationResult.builder().key("commons-levelops").totalTickets(1L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across custom_field, no filter, filter across value: false
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.custom_field)
                                .customAcross("customfield_20001")
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(false)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("Magic1").totalTickets(1L).build(),
                DbAggregationResult.builder().key("Magic2").totalTickets(2L).build(),
                DbAggregationResult.builder().key("Magic3").totalTickets(3L).build(),
                DbAggregationResult.builder().key("Magic4").totalTickets(1L).build(),
                DbAggregationResult.builder().key("Magic5").totalTickets(1L).build(),
                DbAggregationResult.builder().key("Magic7").totalTickets(1L).build()
        );
        verifyRecords(aggs, expected);
        // endregion


        // region across custom_field, no filter, filter across value: true
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.custom_field)
                                .customAcross("customfield_20001")
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(true)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("Magic1").totalTickets(1L).build(),
                DbAggregationResult.builder().key("Magic2").totalTickets(2L).build(),
                DbAggregationResult.builder().key("Magic3").totalTickets(3L).build(),
                DbAggregationResult.builder().key("Magic4").totalTickets(1L).build(),
                DbAggregationResult.builder().key("Magic5").totalTickets(1L).build(),
                DbAggregationResult.builder().key("Magic7").totalTickets(1L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across custom_field, filter by custom_field, filter across value: false
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.custom_field)
                                .customAcross("customfield_20001")
                                .customFields(Map.of("customfield_20001", List.of("Magic4")))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(false)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("Magic3").totalTickets(1L).build(),
                DbAggregationResult.builder().key("Magic4").totalTickets(1L).build(),
                DbAggregationResult.builder().key("Magic5").totalTickets(1L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across custom_field, filter by custom_field, filter across value: true
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.custom_field)
                                .customAcross("customfield_20001")
                                .customFields(Map.of("customfield_20001", List.of("Magic4")))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(true)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("Magic4").totalTickets(1L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across custom_field, filter by custom_field partial, filter across value: true
        aggs = jiraIssueService.groupByAndCalculate(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.custom_field)
                                .customAcross("customfield_20001")
                                .customFields(Map.of("customfield_20001", List.of("Magic4")))
                                .partialMatch(Map.of("customfield_20001", Map.of("$ends", "ic5")))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(true)
                                .build(), false, null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("Magic4").totalTickets(1L).build(),
                DbAggregationResult.builder().key("Magic5").totalTickets(1L).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across status, stacks custom_field, filter by custom_field, filter across value: false
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.status)
                                .customStacks(List.of("customfield_20001"))
                                .customFields(Map.of("customfield_20001", List.of("Magic4")))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(false)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.custom_field), null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("TO DO").totalTickets(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("Magic3").totalTickets(1L).build(),
                                DbAggregationResult.builder().key("Magic4").totalTickets(1L).build(),
                                DbAggregationResult.builder().key("Magic5").totalTickets(1L).build()
                        )
                ).build()
        );
        verifyRecords(aggs, expected);
        // endregion

        // region across status, stacks custom_field, filter by custom_field, filter across value: true
        aggs = jiraIssueService.stackedGroupBy(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1"))
                                .ingestedAt(ingestedAt)
                                .across(JiraIssuesFilter.DISTINCT.status)
                                .customStacks(List.of("customfield_20001"))
                                .customFields(Map.of("customfield_20001", List.of("Magic4")))
                                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                                .hygieneCriteriaSpecs(Map.of())
                                .filterAcrossValues(true)
                                .build(), List.of(JiraIssuesFilter.DISTINCT.custom_field), null, null, Map.of())
                .getRecords();

        expected = List.of(
                DbAggregationResult.builder().key("TO DO").totalTickets(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("Magic4").totalTickets(1L).build()
                        )
                ).build()
        );
        verifyRecords(aggs, expected);
        // endregion
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
