package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsBuildBatch;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsProject;
import io.levelops.commons.databases.models.filters.AWSDevToolsBuildBatchesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.awsdevtools.models.CBBuildBatch;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class AWSDevToolsBuildBatchDatabaseServiceTest {

    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static AWSDevToolsBuildBatchDatabaseService buildBatchDatabaseService;
    private static AWSDevToolsProjectDatabaseService projectDatabaseService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        buildBatchDatabaseService = new AWSDevToolsBuildBatchDatabaseService(dataSource);
        projectDatabaseService = new AWSDevToolsProjectDatabaseService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("awsdevtools")
                .name("awsdevtools_test")
                .status("enabled")
                .build());
        buildBatchDatabaseService.ensureTableExistence(COMPANY);
        projectDatabaseService.ensureTableExistence(COMPANY);
        final String awsdevtoolsBuildBatch = ResourceUtils.getResourceAsString("json/databases/awsdevtools_build_batches.json");
        final PaginatedResponse<CBBuildBatch> buildBatches = OBJECT_MAPPER.readValue(awsdevtoolsBuildBatch,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, CBBuildBatch.class));
        final List<DbAWSDevToolsBuildBatch> dbAWSDevToolsBuildBatches = buildBatches.getResponse().getRecords().stream()
                .map(buildBatch -> DbAWSDevToolsBuildBatch.fromBuildBatch(buildBatch, INTEGRATION_ID))
                .collect(Collectors.toList());
        for (DbAWSDevToolsBuildBatch dbAWSDevToolsBuildBatch : dbAWSDevToolsBuildBatches) {
            final Optional<String> idOpt = buildBatchDatabaseService.insertAndReturnId(COMPANY, dbAWSDevToolsBuildBatch);
            if (idOpt.isEmpty()) {
                throw new RuntimeException("The build batch must exist: " + dbAWSDevToolsBuildBatch);
            }
        }
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
    public void testListBuildBatches() throws SQLException {
        assertThat(buildBatchDatabaseService.list(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .build(), Map.of("build_batch_started_at", SortingOrder.DESC), 0, 100)
                .getRecords().get(0).getBuildBatchStartedAt()).isNotNull();
        assertThat(buildBatchDatabaseService.list(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.list(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .statuses(List.of("FAILED"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.list(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                .integrationIds(List.of("2"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(0);
        assertThat(buildBatchDatabaseService.list(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .initiators(List.of("john"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(1);
    }

    @Test
    public void testGroupByBuildBatches() {
        assertThat(buildBatchDatabaseService.groupByAndCalculate(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildBatchesFilter.DISTINCT.project_name)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.groupByAndCalculate(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildBatchesFilter.DISTINCT.status)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.groupByAndCalculate(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildBatchesFilter.DISTINCT.initiator)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.groupByAndCalculate(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildBatchesFilter.DISTINCT.last_phase)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.groupByAndCalculate(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildBatchesFilter.DISTINCT.last_phase)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.groupByAndCalculate(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildBatchesFilter.DISTINCT.last_phase_status)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.groupByAndCalculate(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildBatchesFilter.DISTINCT.region)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.groupByAndCalculate(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildBatchesFilter.DISTINCT.source_type)
                .calculation(AWSDevToolsBuildBatchesFilter.CALCULATION.duration)
                .build(), null).getRecords().stream()
                .map(DbAggregationResult::getMax)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(36);

    }

    @Test
    public void testStackedGroupBy() throws SQLException {
        assertThat(buildBatchDatabaseService.stackedGroupBy(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildBatchesFilter.DISTINCT.source_type)
                        .build(),
                List.of(AWSDevToolsBuildBatchesFilter.DISTINCT.source_type), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.stackedGroupBy(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildBatchesFilter.DISTINCT.project_name)
                        .build(),
                List.of(AWSDevToolsBuildBatchesFilter.DISTINCT.project_name), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.stackedGroupBy(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildBatchesFilter.DISTINCT.last_phase)
                        .build(),
                List.of(AWSDevToolsBuildBatchesFilter.DISTINCT.last_phase), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.stackedGroupBy(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildBatchesFilter.DISTINCT.last_phase_status)
                        .build(),
                List.of(AWSDevToolsBuildBatchesFilter.DISTINCT.last_phase_status), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.stackedGroupBy(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildBatchesFilter.DISTINCT.status)
                        .build(),
                List.of(AWSDevToolsBuildBatchesFilter.DISTINCT.status), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.stackedGroupBy(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildBatchesFilter.DISTINCT.initiator)
                        .build(),
                List.of(AWSDevToolsBuildBatchesFilter.DISTINCT.initiator), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.stackedGroupBy(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildBatchesFilter.DISTINCT.region)
                        .build(),
                List.of(AWSDevToolsBuildBatchesFilter.DISTINCT.region), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildBatchDatabaseService.stackedGroupBy(COMPANY, AWSDevToolsBuildBatchesFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildBatchesFilter.DISTINCT.source_type)
                        .calculation(AWSDevToolsBuildBatchesFilter.CALCULATION.duration)
                        .build(),
                List.of(AWSDevToolsBuildBatchesFilter.DISTINCT.source_type), null)
                .getRecords().stream()
                .map(DbAggregationResult::getMax)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(36);
    }
}
