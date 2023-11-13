package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeCoverage;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeProject;
import io.levelops.commons.databases.models.filters.SonarQubeMetricFilter;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.sonarqube.models.Project;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Log4j2
public class SonarQubeProjectServiceTest {

    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private SonarQubeProjectService projectService;
    private Date currentTime;

    private TimeZone defaultVal = null;

    @Before
    public void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        defaultVal = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")));
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        projectService = new SonarQubeProjectService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("sonarqube")
                .name("sonarqube_test")
                .status("enabled")
                .build());
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
        projectService.ensureTableExistence(COMPANY);
        insertProjects("json/databases/sonar_projects.json");
        insertProjects("json/databases/sonar_projects.json"); // inserting twice to test for dupes
        insertProjects("json/databases/sonar_analyses.json");
        insertProjects("json/databases/sonar_analyses.json"); // inserting twice to test for dupes
        insertProjects("json/databases/sonar_branches.json");
        insertProjects("json/databases/sonar_branches.json"); // inserting twice to test for dupes
        insertProjects("json/databases/sonar_pr_issues.json");
        insertProjects("json/databases/sonar_pr_issues.json"); // inserting twice to test for dupes

        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"tablefunc\";").execute();
    }

    private void insertProjects(String resourcePath) throws IOException, SQLException {
        String projectsInput = ResourceUtils.getResourceAsString(resourcePath);
        PaginatedResponse<Project> projects = OBJECT_MAPPER.readValue(projectsInput, OBJECT_MAPPER.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, Project.class));
        List<DbSonarQubeProject> dbProjects = projects.getResponse().getRecords().stream()
                .map(project -> DbSonarQubeProject.fromComponent(project, INTEGRATION_ID, currentTime))
                .collect(Collectors.toList());
        for (DbSonarQubeProject dbProject : dbProjects) {
            projectService.insert(COMPANY, dbProject);
            if (projectService.get(COMPANY, dbProject.getKey(), dbProject.getIntegrationId()).isEmpty())
                throw new RuntimeException("The project must exist: " + dbProject);
        }
    }

    @After
    public void clean() {
        TimeZone.setDefault(defaultVal);
    }

    @Test
    public void test() throws SQLException {
        final long ingestedAt = currentTime.toInstant().getEpochSecond();
        String ing = String.valueOf(ingestedAt);
        assertThat(projectService.stackedGroupBy(COMPANY,
                SonarQubeMetricFilter.builder()
                        .metrics(List.of("coverage"))
                        .projects(List.of("commons"))
                        .integrationIds(List.of("1"))
                        .DISTINCT(SonarQubeMetricFilter.DISTINCT.trend)
                        .ingestedAt(ingestedAt)
                        .build(), List.of()).getRecords().get(0).getKey())
                .isEqualTo(ing);
        //test list metric
        assertThat(projectService.listMetrics(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.repo)
                        .ingestedAt(ingestedAt)
                        .build(),
                SortingOrder.DESC,
                0,
                1000).getTotalCount()).isEqualTo(491);
        assertThat(projectService.listMetrics(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.branch)
                        .ingestedAt(ingestedAt)
                        .build(),
                SortingOrder.DESC,
                0,
                1000).getTotalCount()).isEqualTo(52);
        assertThat(projectService.listMetrics(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.pull_request)
                        .ingestedAt(ingestedAt)
                        .build(),
                SortingOrder.DESC,
                0,
                1000).getTotalCount()).isEqualTo(120);
        assertThat(projectService.listMetrics(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.repo)
                        .projects(List.of("io.levelops:aggregations-service"))
                        .organizations(List.of("default-organization"))
                        .visibilities(List.of("public"))
                        .ingestedAt(ingestedAt)
                        .build(),
                SortingOrder.DESC,
                0,
                1000).getTotalCount()).isEqualTo(52);
        assertThat(projectService.listMetrics(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.repo)
                        .projects(List.of("io.levelops:aggregations-service"))
                        .organizations(List.of("default-organization"))
                        .visibilities(List.of("private"))
                        .ingestedAt(ingestedAt)
                        .build(),
                SortingOrder.DESC,
                0,
                1000).getTotalCount()).isEqualTo(0);
        assertThat(projectService.listMetrics(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.pull_request)
                        .projects(List.of("io.levelops:aggregations-service"))
                        .organizations(List.of("default-organization"))
                        .pullRequests(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .build(),
                SortingOrder.DESC,
                0,
                1000).getTotalCount()).isEqualTo(45);
        assertThat(projectService.listMetrics(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.branch)
                        .projects(List.of("io.levelops:aggregations-service"))
                        .organizations(List.of("default-organization"))
                        .metrics(List.of("alert_status", "security_hotspots_to_review_status", "ncloc", "lines_to_cover",
                                "statements", "comment_lines_density", "development_cost", "comment_lines",
                                "effort_to_reach_maintainability_rating_a", "vulnerabilities"))
                        .branches(List.of("master"))
                        .ingestedAt(ingestedAt)
                        .build(),
                SortingOrder.DESC,
                0,
                1000).getTotalCount()).isEqualTo(10);

        //test values
        assertThat(projectService.groupByAndCalculate(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.repo)
                        .DISTINCT(SonarQubeMetricFilter.DISTINCT.metric)
                        .ingestedAt(ingestedAt)
                        .build(), true).getTotalCount()).isEqualTo(58);
        assertThat(projectService.groupByAndCalculate(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.repo)
                        .metrics(List.of("alert_status"))
                        .DISTINCT(SonarQubeMetricFilter.DISTINCT.project)
                        .ingestedAt(ingestedAt)
                        .build(), true).getTotalCount()).isEqualTo(9);

        //test agg
        assertThatThrownBy(() -> projectService.groupByAndCalculate(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.repo)
                        .metrics(List.of("development_cost"))
                        .DISTINCT(SonarQubeMetricFilter.DISTINCT.project)
                        .ingestedAt(ingestedAt)
                        .build(), false)).isInstanceOf(ResponseStatusException.class);
        assertThat(projectService.groupByAndCalculate(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.repo)
                        .metrics(List.of("ncloc"))
                        .DISTINCT(SonarQubeMetricFilter.DISTINCT.project)
                        .ingestedAt(ingestedAt)
                        .build(), false).getTotalCount()).isEqualTo(9);
        assertThat(projectService.groupByAndCalculate(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.repo)
                        .metrics(List.of("ncloc"))
                        .DISTINCT(SonarQubeMetricFilter.DISTINCT.organization)
                        .ingestedAt(ingestedAt)
                        .build(), false).getTotalCount()).isEqualTo(1);
        assertThat(projectService.groupByAndCalculate(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.repo)
                        .metrics(List.of("ncloc"))
                        .DISTINCT(SonarQubeMetricFilter.DISTINCT.visibility)
                        .ingestedAt(ingestedAt)
                        .build(), false).getTotalCount()).isEqualTo(1);

        assertThat(projectService.groupByAndCalculate(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.branch)
                        .metrics(List.of("ncloc"))
                        .DISTINCT(SonarQubeMetricFilter.DISTINCT.branch)
                        .ingestedAt(ingestedAt)
                        .build(), false).getTotalCount()).isEqualTo(1);
        assertThat(projectService.groupByAndCalculate(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.pull_request)
                        .metrics(List.of("ncloc"))
                        .DISTINCT(SonarQubeMetricFilter.DISTINCT.pull_request)
                        .ingestedAt(ingestedAt)
                        .build(), false).getTotalCount()).isEqualTo(2);
        assertThat(projectService.groupByAndCalculate(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.pull_request)
                        .metrics(List.of("duplicated_lines_density"))
                        .DISTINCT(SonarQubeMetricFilter.DISTINCT.pr_branch)
                        .ingestedAt(ingestedAt)
                        .build(), false).getTotalCount()).isEqualTo(3);
        assertThat(projectService.groupByAndCalculate(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.pull_request)
                        .metrics(List.of("duplicated_lines_density"))
                        .DISTINCT(SonarQubeMetricFilter.DISTINCT.pr_base_branch)
                        .ingestedAt(ingestedAt)
                        .build(), false).getTotalCount()).isEqualTo(1);
        assertThat(projectService.groupByAndCalculate(COMPANY,
                SonarQubeMetricFilter.builder()
                        .scope(SonarQubeMetricFilter.SCOPE.pull_request)
                        .metrics(List.of("duplicated_lines_density"))
                        .DISTINCT(SonarQubeMetricFilter.DISTINCT.pr_target_branch)
                        .ingestedAt(ingestedAt)
                        .build(), false).getTotalCount()).isEqualTo(1);

    }

    @Test
    public void testCoverage(){
        final long ingestedAt = currentTime.toInstant().getEpochSecond();
        List<DbSonarQubeCoverage> coverageList = projectService.listCoverage(COMPANY,
                SonarQubeMetricFilter.builder()
                        .integrationIds(List.of("1"))
                        .projects(List.of("io.levelops:aggregations-service"))
                        .organizations(List.of("default-organization"))
                        .ingestedAt(ingestedAt)
                        .build(),0,1000).getRecords();
        assertThat(coverageList.size()).isEqualTo(1);
        assertThat(coverageList.get(0).getCoverage()).isEqualTo("0.0");
        assertThat(coverageList.get(0).getLines()).isEqualTo("6357");
        assertThat(coverageList.get(0).getUncovered_lines()).isEqualTo("2775");
        assertThat(coverageList.get(0).getCovered_lines()).isEqualTo("3582");
    }

    @Test
    public void testDelete() {
        JdbcTemplate template = new NamedParameterJdbcTemplate(dataSource).getJdbcTemplate();

        assertThat(template.queryForObject("SELECT count(*) from test.sonarqube_metrics;", Integer.class)).isEqualTo(663);

        int deleted = projectService.cleanUpOldData(COMPANY, Instant.now().plus(1L, ChronoUnit.DAYS).getEpochSecond(), 0L);
        assertThat(deleted).isEqualTo(663);

        assertThat(template.queryForObject("SELECT count(*) from test.sonarqube_metrics;", Integer.class)).isEqualTo(0);
    }

    @Test
    public void testGetIds() throws SQLException {
        DbSonarQubeProject p = projectService.get(COMPANY, "io.levelops:aggregations-service", "1").orElse(null);
        assertThat(p).isNotNull();
        assertThat(p.getName()).isEqualTo("aggregations-service");

        String projectId = projectService.getProjectId(COMPANY, "io.levelops:aggregations-service", "1").orElse(null);
        assertThat(projectId).isNotNull();

        String branchId = projectService.getBranchId(COMPANY, "master", projectId).orElse(null);
        assertThat(branchId).isNotNull();

        String prId = projectService.getPRId(COMPANY, "1", projectId).orElse(null);
        assertThat(prId).isNotNull();
    }
}
