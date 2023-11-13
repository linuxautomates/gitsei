package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraPriority;
import io.levelops.commons.databases.models.database.jira.DbJiraProject;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraProject;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class JiraProjectServiceTest {
    private static final String COMPANY = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static JiraProjectService jiraProjectService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        jiraProjectService = new JiraProjectService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("jira")
                .name("jira project test")
                .status("enabled")
                .build());
        jiraProjectService.ensureTableExistence(COMPANY);
        String input = ResourceUtils.getResourceAsString("json/databases/jira_project.json");
        PaginatedResponse<JiraProject> projects = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraProject.class));
        List<DbJiraProject> dbJiraProjects = projects.getResponse().getRecords().stream()
                .map(project -> DbJiraProject.fromJiraProject(project, "1"))
                .collect(Collectors.toList());
        if (dbJiraProjects.size() > 0) {
            jiraProjectService.batchUpsert(COMPANY, dbJiraProjects);
        }
    }

    @Test
    public void testValues() {
        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        dbAggregationResults.add(DbAggregationResult.builder()
                .key("LEV")
                .additionalKey("levelops")
                .build());
        dbAggregationResults.add(DbAggregationResult.builder()
                .key("LFE")
                .additionalKey("levelops-lfe")
                .build());
        dbAggregationResults.add(DbAggregationResult.builder()
                .key("LTE")
                .additionalKey("levelops-lte")
                .build());
        List<DbAggregationResult> actual = jiraProjectService.groupByAndCalculateProject(COMPANY, List.of("1"), "project_name")
                .getRecords();
        assertThat(actual).isEqualTo(dbAggregationResults);
        assertThat(actual.size()).isEqualTo(dbAggregationResults.size());
        assertThat(jiraProjectService.groupByAndCalculateProject(COMPANY, List.of("2"), "name")
                .getRecords().size()).isEqualTo(0);
    }

    @Test
    public void testGetPriorities() throws IOException {
        List<DbJiraProject> testProjInputs = getProjectsFromTestResources();
        DbJiraProject testProjInput = testProjInputs.stream()
                .filter(project -> "LEV".equalsIgnoreCase(project.getKey())).findFirst().get();
        List<DbJiraPriority> prioritiesFromService = jiraProjectService.getPriorities(COMPANY, List.of("1"), 0, 100);
        assertThat(prioritiesFromService.size()).isEqualTo(testProjInput.getDefaultPriorities().size());
        prioritiesFromService.forEach(priorityFromService -> {
            assertThat(priorityFromService.getIntegrationId()).isEqualTo(testProjInput.getIntegrationId());
        });

        List<DbJiraPriority> prioritiesFromService1 = jiraProjectService.getPriorities(COMPANY, List.of(), 0, 100);
        assertThat(prioritiesFromService1.size()).isEqualTo(7);

        List<DbJiraPriority> prioritiesFromService2 = jiraProjectService.getPriorities(COMPANY, List.of("2"), 0, 100);
        assertThat(prioritiesFromService2.size()).isEqualTo(0);
    }

    private List<DbJiraProject> getProjectsFromTestResources() throws IOException {
        String input = ResourceUtils.getResourceAsString("json/databases/jira_project.json");
        PaginatedResponse<JiraProject> projects = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraProject.class));
        return projects.getResponse().getRecords().stream()
                .map(project -> DbJiraProject.fromJiraProject(project, "1"))
                .collect(Collectors.toList());
    }
}
