package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsProject;
import io.levelops.commons.databases.models.filters.AWSDevToolsProjectsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.awsdevtools.models.CBProject;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class AWSDevToolsProjectDatabaseServiceTest {

    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static AWSDevToolsProjectDatabaseService projectDatabaseService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        projectDatabaseService = new AWSDevToolsProjectDatabaseService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("awsdevtools")
                .name("awsdevtools1_test")
                .status("enabled")
                .build());
        projectDatabaseService.ensureTableExistence(COMPANY);
        final String awsdevtoolsProject = ResourceUtils.getResourceAsString("json/databases/awsdevtools_projects.json");
        final PaginatedResponse<CBProject> projects = OBJECT_MAPPER.readValue(awsdevtoolsProject,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, CBProject.class));
        final List<DbAWSDevToolsProject> dbAWSDevToolsProjects = projects.getResponse().getRecords().stream()
                .map(project -> DbAWSDevToolsProject.fromProject(project, INTEGRATION_ID))
                .collect(Collectors.toList());
        for (DbAWSDevToolsProject dbAWSDevToolsProject : dbAWSDevToolsProjects) {
            final Optional<String> idOpt = projectDatabaseService.insertAndReturnId(COMPANY, dbAWSDevToolsProject);
            if (idOpt.isEmpty()) {
                throw new RuntimeException("The project must exist: " + dbAWSDevToolsProject);
            }
        }
    }

    @Test
    public void testListProjects() throws SQLException {
        assertThat(projectDatabaseService.list(COMPANY, AWSDevToolsProjectsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .build(), Map.of("project_created_at", SortingOrder.DESC), 0, 100)
                .getRecords().get(0).getProjectCreatedAt()).isNotNull();
        DbListResponse<DbAWSDevToolsProject> listResponse = projectDatabaseService.list(COMPANY, AWSDevToolsProjectsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .sourceTypes(List.of("CODECOMMIT"))
                .build(), Map.of("project_created_at", SortingOrder.DESC), 0, 100);
        listResponse.getRecords().forEach(record -> assertThat(record.getSourceType().equals("CODECOMMIT")).isTrue());
        assertThat(projectDatabaseService.list(COMPANY, AWSDevToolsProjectsFilter.builder()
                .integrationIds(List.of("2"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testGroupByProjects() {
        DbListResponse<DbAggregationResult> projectsResponse = projectDatabaseService.groupByAndCalculate(COMPANY,
                AWSDevToolsProjectsFilter.builder()
                        .calculation(AWSDevToolsProjectsFilter.CALCULATION.project_count)
                        .across(AWSDevToolsProjectsFilter.DISTINCT.source_type)
                        .build(), null
        );
        assertThat(projectsResponse.getRecords().size()).isEqualTo(2);
        List<String> keyList = projectsResponse.getRecords().stream().map(DbAggregationResult::getKey)
                .collect(Collectors.toList());
        assertThat(keyList).contains("CODECOMMIT");
        assertThat(keyList).contains("S3");
        assertThat(projectDatabaseService.groupByAndCalculate(COMPANY, AWSDevToolsProjectsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsProjectsFilter.DISTINCT.source_type)
                .build(), null).getTotalCount()).isEqualTo(2);
        assertThat(projectDatabaseService.groupByAndCalculate(COMPANY, AWSDevToolsProjectsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsProjectsFilter.DISTINCT.region)
                .build(), null).getTotalCount()).isEqualTo(2);
    }

    @Test
    public void testStackedGroupBy() throws SQLException {
        assertThat(projectDatabaseService.stackedGroupBy(COMPANY, AWSDevToolsProjectsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsProjectsFilter.DISTINCT.source_type)
                        .sourceTypes(List.of("CODECOMMIT"))
                        .build(),
                List.of(AWSDevToolsProjectsFilter.DISTINCT.source_type), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(projectDatabaseService.stackedGroupBy(COMPANY, AWSDevToolsProjectsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsProjectsFilter.DISTINCT.source_type)
                        .build(),
                List.of(AWSDevToolsProjectsFilter.DISTINCT.source_type), null)
                .getTotalCount()).isEqualTo(2);
        assertThat(projectDatabaseService.stackedGroupBy(COMPANY, AWSDevToolsProjectsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsProjectsFilter.DISTINCT.region)
                        .build(),
                List.of(AWSDevToolsProjectsFilter.DISTINCT.region), null)
                .getTotalCount()).isEqualTo(2);
    }
}
