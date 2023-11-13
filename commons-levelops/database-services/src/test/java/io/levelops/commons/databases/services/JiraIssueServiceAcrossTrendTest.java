package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraUser;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueServiceAcrossTrendTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static JiraIssueService jiraIssueService;
    private static Date currentTime;
    private static IntegrationService integrationService;
    private static UserIdentityService userIdentityService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null) {
            return;
        }

        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
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
        jiraIssueService.ensureTableExistence(company);
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

        input = ResourceUtils.getResourceAsString("json/databases/jiraissues_jan22.json");
        PaginatedResponse<JiraIssue> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = new Date();

        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3))
                        .ingestedAt(1640995200L).build();
                jiraIssueService.insert(company, tmp);
                List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
                versions.forEach(dbJiraVersion -> jiraIssueService.insertJiraVersion(company, dbJiraVersion));
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });

        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3))
                        .ingestedAt(1640908800L).build();
                jiraIssueService.insert(company, tmp);
                List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
                versions.forEach(dbJiraVersion -> jiraIssueService.insertJiraVersion(company, dbJiraVersion));
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });

        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3))
                        .ingestedAt(1640822400L).build();
                jiraIssueService.insert(company, tmp);
                List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
                versions.forEach(dbJiraVersion -> jiraIssueService.insertJiraVersion(company, dbJiraVersion));
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });

        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3))
                        .ingestedAt(1640736000L).build();
                jiraIssueService.insert(company, tmp);
                List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
                versions.forEach(dbJiraVersion -> jiraIssueService.insertJiraVersion(company, dbJiraVersion));
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });

        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3))
                        .ingestedAt(1640649600L).build();
                jiraIssueService.insert(company, tmp);
                List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
                versions.forEach(dbJiraVersion -> jiraIssueService.insertJiraVersion(company, dbJiraVersion));
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });

        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3))
                        .ingestedAt(1640563200L).build();
                jiraIssueService.insert(company, tmp);
                List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
                versions.forEach(dbJiraVersion -> jiraIssueService.insertJiraVersion(company, dbJiraVersion));
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });

        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3))
                        .ingestedAt(1640476800L).build();
                jiraIssueService.insert(company, tmp);
                List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
                versions.forEach(dbJiraVersion -> jiraIssueService.insertJiraVersion(company, dbJiraVersion));
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });

        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3))
                        .ingestedAt(1640390400L).build();
                jiraIssueService.insert(company, tmp);
                List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
                versions.forEach(dbJiraVersion -> jiraIssueService.insertJiraVersion(company, dbJiraVersion));
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });

        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3))
                        .ingestedAt(1640304000L).build();
                jiraIssueService.insert(company, tmp);
                List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
                versions.forEach(dbJiraVersion -> jiraIssueService.insertJiraVersion(company, dbJiraVersion));
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });

        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3))
                        .ingestedAt(1640217600L).build();
                jiraIssueService.insert(company, tmp);
                List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
                versions.forEach(dbJiraVersion -> jiraIssueService.insertJiraVersion(company, dbJiraVersion));
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });

        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3))
                        .ingestedAt(1640131200L).build();
                jiraIssueService.insert(company, tmp);
                List<DbJiraVersion> versions = DbJiraVersion.fromJiraIssue(issue, "1");
                versions.forEach(dbJiraVersion -> jiraIssueService.insertJiraVersion(company, dbJiraVersion));
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });
    }

    @Test
    public void test() throws SQLException {
        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .aggInterval("day")
                        .build(),
                null, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getRecords().size()).isEqualTo(11);
        assertThat(dbAggregationResultDbListResponse.getRecords().get(0).getTotalTickets()).isEqualTo(5);

        dbAggregationResultDbListResponse = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .aggInterval("week")
                        .build(),
                null, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getRecords().size()).isEqualTo(2);
        assertThat(dbAggregationResultDbListResponse.getRecords().get(0).getTotalTickets()).isEqualTo(5);
        DbListResponse<DbJiraIssue> dbListResponse = jiraIssueService.list(company,
                JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .aggInterval("week")
                        .ingestedAt(Long.valueOf(dbAggregationResultDbListResponse.getRecords().get(0).getKey())).build(),
                Optional.empty(), null, Optional.empty(), Map.of(), 0, 10);
        assertThat(dbListResponse.getRecords().size()).isEqualTo(5);

        dbAggregationResultDbListResponse = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .aggInterval("biweekly")
                        .build(),
                null, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getRecords().size()).isEqualTo(1);
        assertThat(dbAggregationResultDbListResponse.getRecords().get(0).getTotalTickets()).isEqualTo(5);
        dbListResponse = jiraIssueService.list(company,
                JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .aggInterval("biweekly")
                        .ingestedAt(Long.valueOf(dbAggregationResultDbListResponse.getRecords().get(0).getKey())).build(),
                Optional.empty(), null, Optional.empty(), Map.of(), 0, 10);
        assertThat(dbListResponse.getRecords().size()).isEqualTo(5);

        dbAggregationResultDbListResponse = jiraIssueService.stackedGroupBy(
                company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .aggInterval("month")
                        .build(),
                null, null, null, Map.of());
        assertThat(dbAggregationResultDbListResponse.getRecords().size()).isEqualTo(1);
        assertThat(dbAggregationResultDbListResponse.getRecords().get(0).getTotalTickets()).isEqualTo(5);
        dbListResponse = jiraIssueService.list(company,
                JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.trend)
                        .aggInterval("month")
                        .ingestedAt(Long.valueOf(dbAggregationResultDbListResponse.getRecords().get(0).getKey())).build(),
                Optional.empty(), null, Optional.empty(), Map.of(), 0, 10);
        assertThat(dbListResponse.getRecords().size()).isEqualTo(5);
    }
}
