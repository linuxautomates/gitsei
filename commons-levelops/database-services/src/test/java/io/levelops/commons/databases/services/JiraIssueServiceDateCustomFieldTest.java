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

public class JiraIssueServiceDateCustomFieldTest {
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
    private static String integrationId2;


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
                .name("jira test")
                .status("enabled")
                .build());
        jiraIssueService.ensureTableExistence(company);
        jiraFieldService.ensureTableExistence(company);
        integrationTrackingService.ensureTableExistence(company);

        jiraFieldService.batchUpsert(company,
                List.of(DbJiraField.builder().custom(true).name("target time").integrationId("1").fieldType("datetime").fieldKey("customfield_10048").build(),
                        DbJiraField.builder().custom(true).name("story points").integrationId("1").fieldType("number").fieldKey("customfield_10052").build(),
                        DbJiraField.builder().custom(true).name("custom labels").integrationId("1").fieldType("array").fieldKey("customfield_10050").build()));
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
                .customFields(Map.of("customfield_10048", "1628169566000",
                        "customfield_10050", List.of("Magic1", "Magic2"),
                        "customfield_10052", "7"))
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("LEV-2")
                .ingestedAt(1L)
                .customFields(Map.of("customfield_10048", "1628601566000",
                        "customfield_10050", List.of("Magic2", "Magic3"),
                        "customfield_10052", "3"))
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("LEV-3")
                .ingestedAt(1L)
                .customFields(Map.of("customfield_10048", "1628428766000",
                        "customfield_10050", List.of("Magic3", "Magic4"),
                        "customfield_10052", "5"))
                .build());

        List<DbJiraIssue> records = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(1L)
                        .customFields(Map.of("customfield_10048", Map.of("$gt", "1628515166")))
                        .hygieneCriteriaSpecs(Map.of())
                        .filterAcrossValues(false)
                        .build(), null, Map.of(), 0, 10).getRecords();

        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).getKey()).isEqualTo("LEV-2");

        records = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(1L)
                        .customFields(Map.of("customfield_10048", Map.of("$lt", "1628255966")))
                        .hygieneCriteriaSpecs(Map.of())
                        .filterAcrossValues(false)
                        .build(), null, Map.of(), 0, 10).getRecords();

        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).getKey()).isEqualTo("LEV-1");

        records = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(1L)
                        .customFields(Map.of("customfield_10048",
                                Map.of("$lt", "1628515166", "$gt", "1628255966")))
                        .hygieneCriteriaSpecs(Map.of())
                        .filterAcrossValues(false)
                        .build(), null, null, 0, 10).getRecords();

        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).getKey()).isEqualTo("LEV-3");

        records = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(1L)
                        .customFields(Map.of("customfield_10052",
                                Map.of("$lt", "6", "$gt", "4")))
                        .hygieneCriteriaSpecs(Map.of())
                        .filterAcrossValues(false)
                        .build(), null, null, 0, 10).getRecords();

        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).getKey()).isEqualTo("LEV-3");

        records = jiraIssueService.list(company, JiraSprintFilter.builder().build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(1L)
                        .customFields(Map.of(
                                "customfield_10048", Map.of("$gt", "1628255966"),
                                "customfield_10050", List.of("Magic3")))
                        .hygieneCriteriaSpecs(Map.of())
                        .filterAcrossValues(false)
                        .build(), null, null, 0, 10).getRecords();

        assertThat(records.size()).isEqualTo(2);
        assertThat(records.get(0).getKey()).isEqualTo("LEV-2");
        assertThat(records.get(1).getKey()).isEqualTo("LEV-3");

    }
}