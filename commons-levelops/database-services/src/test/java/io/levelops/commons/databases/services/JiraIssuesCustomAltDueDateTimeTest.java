package io.levelops.commons.databases.services;

import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssuesCustomAltDueDateTimeTest {

    private static final String company = "test";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static JiraIssueService jiraIssueService;
    private static IntegrationService integrationService;
    private static JiraProjectService jiraProjectService;
    private static JiraIssueSprintMappingDatabaseService sprintMappingDatabaseService;
    private static IntegrationTrackingService integrationTrackingService;
    private static String integrationId;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource =  DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);

        final JiraFieldService jiraFieldService = new JiraFieldService(dataSource);
        integrationService = new IntegrationService(dataSource);
        jiraProjectService = new JiraProjectService(dataSource);
        jiraIssueService = jiraTestDbs.getJiraIssueService();
        sprintMappingDatabaseService = new JiraIssueSprintMappingDatabaseService(dataSource);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        new TagsService(dataSource).ensureTableExistence(company);
        new TagItemDBService(dataSource).ensureTableExistence(company);
        integrationTrackingService = new IntegrationTrackingService(dataSource);
        integrationService.ensureTableExistence(company);
        jiraProjectService.ensureTableExistence(company);
        sprintMappingDatabaseService.ensureTableExistence(company);
        integrationId = integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test2")
                .status("enabled")
                .build());
        jiraIssueService.ensureTableExistence(company);
        jiraFieldService.ensureTableExistence(company);
        integrationTrackingService.ensureTableExistence(company);

        jiraFieldService.batchUpsert(company,
                List.of(DbJiraField.builder().custom(true).name("Alt Due Date Time").integrationId(integrationId).fieldType("datetime").fieldKey("customfield_10001").build(),
                        DbJiraField.builder().custom(true).name("Alt Due Date").integrationId(integrationId).fieldType("date").fieldKey("customfield_10002").build()));
    }

    private static DbJiraIssue buildBaseIssue() {
        return DbJiraIssue.builder()
                .key("LEV-123")
                .integrationId(integrationId)
                .project("def")
                .summary("sum")
                .descSize(3)
                .priority("high")
                .reporter("max")
                .status("won't do")
                .issueType("bug")
                .hops(2)
                .bounces(4)
                .numAttachments(0)
                .issueCreatedAt(1L)
                .issueUpdatedAt(2L)
                .ingestedAt(0L)
                .reporter("gandalf")
                .assignee("frodo")
                .build();
    }

    @Test
    public void testDateTimeCustomField() throws SQLException {
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("LEV-1")
                .ingestedAt(1L)
                .customFields(Map.of("customfield_10001", 1628169566000L,
                        "customfield_10002", 1605657600000L))
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("LEV-2")
                .ingestedAt(1L)
                .customFields(Map.of("customfield_10001", 1628169566000L,
                        "customfield_10002", 1605657600000L))
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("LEV-3")
                .ingestedAt(1L)
                .customFields(Map.of("customfield_10001", 1628169566000L,
                        "customfield_10002", 1605657600000L))
                .build());

        List<DbJiraIssue> records = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of(integrationId))
                        .ingestedAt(1L)
                        .customFields(Map.of("customfield_10001", List.of("1628169566000")))
                        .build(), null, Map.of(), 0, 10).getRecords();
        assertThat(records.size()).isEqualTo(3);
        assertThat(records.get(0).getKey()).isEqualTo("LEV-1");
        assertThat(records.get(1).getKey()).isEqualTo("LEV-2");
        assertThat(records.get(2).getKey()).isEqualTo("LEV-3");

        records = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of(integrationId))
                        .ingestedAt(1L)
                        .customFields(Map.of("customfield_10002", List.of("1605657600000")))
                        .build(), null, Map.of(), 0, 10).getRecords();
        assertThat(records.size()).isEqualTo(3);
        assertThat(records.get(0).getKey()).isEqualTo("LEV-1");
        assertThat(records.get(1).getKey()).isEqualTo("LEV-2");
        assertThat(records.get(2).getKey()).isEqualTo("LEV-3");

        records = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(1L)
                        .customFields(Map.of(
                                "customfield_10001", List.of("1628169566000"),
                                "customfield_10002", List.of("1605657600000")))
                        .hygieneCriteriaSpecs(Map.of())
                        .filterAcrossValues(false)
                        .build(), null, null, 0, 10).getRecords();
        assertThat(records.size()).isEqualTo(3);

        records = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(1L)
                        .customFields(Map.of("customfield_10001", List.of("1505657600000")))
                        .hygieneCriteriaSpecs(Map.of())
                        .filterAcrossValues(false)
                        .build(), null, null, 0, 10).getRecords();
        assertThat(records.size()).isEqualTo(0);

        records = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(1L)
                        .customFields(Map.of("customfield_10002", List.of("1628169566000")))
                        .hygieneCriteriaSpecs(Map.of())
                        .filterAcrossValues(false)
                        .build(), null, null, 0, 10).getRecords();
        assertThat(records.size()).isEqualTo(0);
    }
}
