package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraProject;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraProject;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueServiceCustomDueDateTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static JiraIssueService jiraIssueService;
    private static Date currentTime;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;

        dataSource =  DatabaseTestUtils.setUpDataSource(pg, company);
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
        String input = ResourceUtils.getResourceAsString("json/databases/jiraissues_duedate.json");
        PaginatedResponse<JiraIssue> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = new Date();
        jiraFieldService.batchUpsert(company,
                List.of(DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("array").fieldKey("customfield_10048").fieldItems("user").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("string").fieldKey("customfield_12746").build()));
        List<IntegrationConfig.ConfigEntry> entries = List.of(
                IntegrationConfig.ConfigEntry.builder().key("customfield_12641").name("something").build(),
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
                jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, "1",
                        DateUtils.addSeconds(currentTime, 2 * -86400),JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build()));
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(),
                        tmp.getIngestedAt() - 86400L).isPresent())
                    throw new RuntimeException("This issue shouldnt exist.");
                jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, "1",
                        DateUtils.addSeconds(currentTime, -86400),JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build()));
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
    }

    @Test
    public void testCustomDueDate() throws SQLException, IOException {
        String input = ResourceUtils.getResourceAsString("json/databases/jiraissues_duedate.json");
        PaginatedResponse<JiraIssue> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = new Date();

        DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issues.getResponse().getRecords().get(0), "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .dueDateField("customfield_10022")
                        .build());
        jiraIssueService.insert(company, tmp);
        Optional<DbJiraIssue> dbJiraIssue = jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt());
        assertThat(dbJiraIssue.get().getIssueDueAt()).isEqualTo(1582617600L);

        tmp = JiraIssueParser.parseJiraIssue(issues.getResponse().getRecords().get(0), "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                .epicLinkField("customfield_10014")
                .storyPointsField("customfield_10030")
                .build());
        jiraIssueService.insert(company, tmp);
        dbJiraIssue = jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt());
        assertThat(dbJiraIssue.get().getIssueDueAt()).isEqualTo(0L);

        tmp = JiraIssueParser.parseJiraIssue(issues.getResponse().getRecords().get(1), "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                .epicLinkField("customfield_10014")
                .storyPointsField("customfield_10030")
                .dueDateField("customfield_20011")
                .build());
        jiraIssueService.insert(company, tmp);
        dbJiraIssue = jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt());
        assertThat(dbJiraIssue.get().getIssueDueAt()).isEqualTo(1578038400L);

        tmp = JiraIssueParser.parseJiraIssue(issues.getResponse().getRecords().get(1), "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                .epicLinkField("customfield_10014")
                .storyPointsField("customfield_10030")
                .build());
        jiraIssueService.insert(company, tmp);
        dbJiraIssue = jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt());
        assertThat(dbJiraIssue.get().getIssueDueAt()).isEqualTo(1582185600L);

    }
}
