package io.levelops.commons.databases.services;

import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Date;

public class JiraIssueDataCleanupTest {
    private static final String company = "test";
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static JiraIssueService jiraIssueService;
    private static IntegrationService integrationService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null) {
            return;
        }

        dataSource =  DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        JiraFieldService jiraFieldService = jiraTestDbs.getJiraFieldService();
        jiraTestDbs.getJiraProjectService();
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
    }

    @Before
    public void setUp() throws Exception {
        dataSource.getConnection().prepareStatement("delete from " + company + ".jira_issues;").execute();
    }

    @Test
    public void testCleanUpData() throws SQLException {
        Long ingestedMoreThan90DaysBackOn1st = LocalDate.now().minusDays(100).with(ChronoField.DAY_OF_MONTH, 1)
                .toEpochSecond(LocalTime.NOON, ZoneOffset.UTC);
        Long ingestedMoreThan90DaysBackOn2nd = LocalDate.now().minusDays(100).with(ChronoField.DAY_OF_MONTH, 2)
                .toEpochSecond(LocalTime.NOON, ZoneOffset.UTC);
        Long ingestedMoreThan90DaysBackOn3rd = LocalDate.now().minusDays(100).with(ChronoField.DAY_OF_MONTH, 3)
                .toEpochSecond(LocalTime.NOON, ZoneOffset.UTC);
        Long ingestedMoreThan90DaysBackOn4th = LocalDate.now().minusDays(100).with(ChronoField.DAY_OF_MONTH, 4)
                .toEpochSecond(LocalTime.NOON, ZoneOffset.UTC);
        jiraIssueService.insert(company, DbJiraIssue.builder().ingestedAt(ingestedMoreThan90DaysBackOn2nd).integrationId("1").key("LEV_999").project("test").summary("asc").descSize(12)
                .priority("high").reporter("as").status("qwe").issueType("wer").hops(1).bounces(1).numAttachments(1).issueCreatedAt(1604385440L).issueUpdatedAt(1604385440L).build());
        jiraIssueService.insert(company, DbJiraIssue.builder().ingestedAt(ingestedMoreThan90DaysBackOn3rd).integrationId("1").key("LEV_999").project("test").summary("asc").descSize(1)
                .priority("high").reporter("qq").status("qw").issueType("wee").hops(1).bounces(1).numAttachments(1).issueCreatedAt(1604385440L).issueUpdatedAt(1604385440L).build());
        jiraIssueService.insert(company, DbJiraIssue.builder().ingestedAt(ingestedMoreThan90DaysBackOn4th).integrationId("1").key("LEV_999").project("test").summary("asc").descSize(1)
                .priority("low").reporter("qqw").status("we").issueType("asdd").hops(1).bounces(1).numAttachments(1).issueCreatedAt(1604385440L).issueUpdatedAt(1604385440L).build());
        jiraIssueService.insert(company, DbJiraIssue.builder().ingestedAt(ingestedMoreThan90DaysBackOn1st).integrationId("1").key("LEV_999").project("testTempProject").summary("asc").descSize(1)
                .priority("high").reporter("ww").status("ss").issueType("ds").hops(1).bounces(1).numAttachments(1).issueCreatedAt(1604385440L).issueUpdatedAt(1604385440L).build());

        int deletedValues = jiraIssueService.cleanUpOldData(company,
                new Date().toInstant().getEpochSecond(),
                86400 * 91L);
        AssertionsForClassTypes.assertThat(deletedValues).isEqualTo(3);
        AssertionsForClassTypes.assertThat(jiraIssueService.get(company, "LEV_999", "1", ingestedMoreThan90DaysBackOn1st)).isNotNull();
    }
}
