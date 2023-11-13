package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraUser;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class JiraIssueEscapeCharacterTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static JiraIssueService jiraIssueService;
    private static Date currentTime;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null) {
            return;
        }
        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        final JiraFieldService jiraFieldService = new JiraFieldService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
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
        jiraIssueService.ensureTableExistence(company);
        jiraFieldService.ensureTableExistence(company);
        String input = ResourceUtils.getResourceAsString("json/databases/jira_issues_custom_escape.json");
        PaginatedResponse<JiraIssue> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = new Date();
        jiraFieldService.batchUpsert(company,
                List.of(DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("array").fieldKey("customfield_10048").fieldItems("user").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("string").fieldKey("customfield_12641").build(),
                        DbJiraField.builder().custom(true).name("hello").integrationId("1").fieldType("string").fieldKey("customfield_20001").build(),
                        DbJiraField.builder().custom(true).name("sprint").integrationId("1").fieldType("array").fieldKey("customfield_10020").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("string").fieldKey("customfield_12746").build()));
        List<IntegrationConfig.ConfigEntry> entries = List.of(
                IntegrationConfig.ConfigEntry.builder().key("customfield_12641").name("something").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_12746").name("something 1").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_10048").name("USER ARRAY").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_10149").name("USER SINGLE").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_20001").name("hello").delimiter(",").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_12716").name("something 2").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_10020").name("sprint").build());
        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .customFieldConfig(entries)
                        .build());
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3)).build();
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
    }

    @Test
    public void testEscapeSingleQuoteCharacter() throws SQLException {
        List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters = List.of(JiraIssuesFilter.TicketCategorizationFilter.builder()
                        .name("The OG's")
                        .index(10)
                        .filter(JiraIssuesFilter.builder()
                                .customFields(Map.of("customfield_10020", List.of("The OG's")))
                                .build())
                        .build(),
                JiraIssuesFilter.TicketCategorizationFilter.builder()
                        .name("AC/DC")
                        .index(20)
                        .filter(JiraIssuesFilter.builder()
                                .customFields(Map.of("customfield_10020", List.of("AC/DC")))
                                .build())
                        .build());
        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = jiraIssueService.groupByAndCalculate(
                company,
                JiraIssuesFilter.builder()
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .across(JiraIssuesFilter.DISTINCT.ticket_category)
                        .ticketCategorizationFilters(ticketCategorizationFilters)
                        .build(), false, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getRecords().size()).isEqualTo(3);
        assertThat(dbAggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("The OG's", "AC/DC", "Other");
    }
}
