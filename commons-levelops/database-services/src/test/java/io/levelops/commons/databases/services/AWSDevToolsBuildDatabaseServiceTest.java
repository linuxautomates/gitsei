package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsBuild;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsProject;
import io.levelops.commons.databases.models.filters.AWSDevToolsBuildsFilter;
import io.levelops.commons.databases.models.filters.AWSDevToolsTestcasesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.awsdevtools.models.CBBuild;
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

public class AWSDevToolsBuildDatabaseServiceTest {

    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";
    private static final String REPORT_ARN = "arn:aws:codebuild:us-east-2:report/codebuild-msg1:71ee492f-9e70-4b5f-844d-78c8240692ae";

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static AWSDevToolsBuildDatabaseService buildDatabaseService;
    private static AWSDevToolsProjectDatabaseService projectDatabaseService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        buildDatabaseService = new AWSDevToolsBuildDatabaseService(dataSource);
        projectDatabaseService = new AWSDevToolsProjectDatabaseService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("awsdevtools")
                .name("awsdevtools1_test")
                .status("enabled")
                .build());
        buildDatabaseService.ensureTableExistence(COMPANY);
        projectDatabaseService.ensureTableExistence(COMPANY);
        final String awsdevtoolsBuild = ResourceUtils.getResourceAsString("json/databases/awsdevtools_builds.json");
        final PaginatedResponse<CBBuild> builds = OBJECT_MAPPER.readValue(awsdevtoolsBuild,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, CBBuild.class));
        final List<DbAWSDevToolsBuild> dbAWSDevToolsBuilds = builds.getResponse().getRecords().stream()
                .map(build -> DbAWSDevToolsBuild.fromBuild(build, INTEGRATION_ID))
                .collect(Collectors.toList());
        for (DbAWSDevToolsBuild dbAWSDevToolsBuild : dbAWSDevToolsBuilds) {
            final Optional<String> idOpt = buildDatabaseService.insertAndReturnId(COMPANY, dbAWSDevToolsBuild);
            if (idOpt.isEmpty()) {
                throw new RuntimeException("The build must exist: " + dbAWSDevToolsBuild);
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
    public void testListBuilds() {
        assertThat(buildDatabaseService.listBuilds(COMPANY, AWSDevToolsBuildsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .build(), Map.of("build_started_at", SortingOrder.DESC), 0, 100)
                .getRecords().get(0).getBuildStartedAt()).isNotNull();
        assertThat(buildDatabaseService.listBuilds(COMPANY, AWSDevToolsBuildsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(22);
        assertThat(buildDatabaseService.listBuilds(COMPANY, AWSDevToolsBuildsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .statuses(List.of("FAILED"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(20);
        assertThat(buildDatabaseService.listBuilds(COMPANY, AWSDevToolsBuildsFilter.builder()
                .integrationIds(List.of("2"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(0);
        assertThat(buildDatabaseService.listBuilds(COMPANY, AWSDevToolsBuildsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .initiators(List.of("mukesh"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(22);
    }

    @Test
    public void testGroupByBuilds() {
        assertThat(buildDatabaseService.groupByAndCalculateBuild(COMPANY, AWSDevToolsBuildsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildsFilter.DISTINCT.project_name)
                .build(), null).getTotalCount()).isEqualTo(12);
        assertThat(buildDatabaseService.groupByAndCalculateBuild(COMPANY, AWSDevToolsBuildsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildsFilter.DISTINCT.status)
                .build(), null).getTotalCount()).isEqualTo(2);
        assertThat(buildDatabaseService.groupByAndCalculateBuild(COMPANY, AWSDevToolsBuildsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildsFilter.DISTINCT.initiator)
                .build(), null).getTotalCount()).isEqualTo(2);
        assertThat(buildDatabaseService.groupByAndCalculateBuild(COMPANY, AWSDevToolsBuildsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildsFilter.DISTINCT.last_phase)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.groupByAndCalculateBuild(COMPANY, AWSDevToolsBuildsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildsFilter.DISTINCT.last_phase)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.groupByAndCalculateBuild(COMPANY, AWSDevToolsBuildsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildsFilter.DISTINCT.last_phase_status)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.groupByAndCalculateBuild(COMPANY, AWSDevToolsBuildsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildsFilter.DISTINCT.build_batch_arn)
                .build(), null).getTotalCount()).isEqualTo(2);
        assertThat(buildDatabaseService.groupByAndCalculateBuild(COMPANY, AWSDevToolsBuildsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildsFilter.DISTINCT.region)
                .build(), null).getTotalCount()).isEqualTo(2);
        assertThat(buildDatabaseService.groupByAndCalculateBuild(COMPANY, AWSDevToolsBuildsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .across(AWSDevToolsBuildsFilter.DISTINCT.source_type)
                .calculation(AWSDevToolsBuildsFilter.CALCULATION.duration)
                .build(), null).getRecords().stream()
                .map(DbAggregationResult::getMax)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(52);

    }

    @Test
    public void testBuildStackedGroupBy() throws SQLException {
        assertThat(buildDatabaseService.buildStackedGroupBy(COMPANY, AWSDevToolsBuildsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildsFilter.DISTINCT.source_type)
                        .build(),
                List.of(AWSDevToolsBuildsFilter.DISTINCT.source_type), null)
                .getTotalCount()).isEqualTo(2);
        assertThat(buildDatabaseService.buildStackedGroupBy(COMPANY, AWSDevToolsBuildsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildsFilter.DISTINCT.project_name)
                        .build(),
                List.of(AWSDevToolsBuildsFilter.DISTINCT.project_name), null)
                .getTotalCount()).isEqualTo(12);
        assertThat(buildDatabaseService.buildStackedGroupBy(COMPANY, AWSDevToolsBuildsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildsFilter.DISTINCT.last_phase)
                        .build(),
                List.of(AWSDevToolsBuildsFilter.DISTINCT.last_phase), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.buildStackedGroupBy(COMPANY, AWSDevToolsBuildsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildsFilter.DISTINCT.last_phase_status)
                        .build(),
                List.of(AWSDevToolsBuildsFilter.DISTINCT.last_phase_status), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.buildStackedGroupBy(COMPANY, AWSDevToolsBuildsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildsFilter.DISTINCT.status)
                        .build(),
                List.of(AWSDevToolsBuildsFilter.DISTINCT.status), null)
                .getTotalCount()).isEqualTo(2);
        assertThat(buildDatabaseService.buildStackedGroupBy(COMPANY, AWSDevToolsBuildsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildsFilter.DISTINCT.initiator)
                        .build(),
                List.of(AWSDevToolsBuildsFilter.DISTINCT.initiator), null)
                .getTotalCount()).isEqualTo(2);
        assertThat(buildDatabaseService.buildStackedGroupBy(COMPANY, AWSDevToolsBuildsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildsFilter.DISTINCT.build_batch_arn)
                        .build(),
                List.of(AWSDevToolsBuildsFilter.DISTINCT.build_batch_arn), null)
                .getTotalCount()).isEqualTo(2);
        assertThat(buildDatabaseService.buildStackedGroupBy(COMPANY, AWSDevToolsBuildsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildsFilter.DISTINCT.region)
                        .build(),
                List.of(AWSDevToolsBuildsFilter.DISTINCT.region), null)
                .getTotalCount()).isEqualTo(2);
        assertThat(buildDatabaseService.buildStackedGroupBy(COMPANY, AWSDevToolsBuildsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .across(AWSDevToolsBuildsFilter.DISTINCT.source_type)
                        .calculation(AWSDevToolsBuildsFilter.CALCULATION.duration)
                        .build(),
                List.of(AWSDevToolsBuildsFilter.DISTINCT.source_type), null)
                .getRecords().stream()
                .map(DbAggregationResult::getMax)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(52);
    }

    @Test
    public void testListTestcases() {
        assertThat(buildDatabaseService.listTestcases(COMPANY, AWSDevToolsTestcasesFilter.builder()
                .reportArns(List.of(REPORT_ARN))
                .build(), 0, 100)
                .getRecords().get(0).getDuration()).isNotNull();
        assertThat(buildDatabaseService.listTestcases(COMPANY, AWSDevToolsTestcasesFilter.builder()
                .reportArns(List.of(REPORT_ARN))
                .build(), 0, 100).getTotalCount()).isEqualTo(3);
        assertThat(buildDatabaseService.listTestcases(COMPANY, AWSDevToolsTestcasesFilter.builder()
                .statuses(List.of("FAILED"))
                .build(), 0, 100).getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.listTestcases(COMPANY, AWSDevToolsTestcasesFilter.builder()
                .reportArns(List.of("12334567"))
                .build(), 0, 100).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testGroupByTestcases() {
        assertThat(buildDatabaseService.groupByAndCalculateTestcase(COMPANY, AWSDevToolsTestcasesFilter.builder()
                .across(AWSDevToolsTestcasesFilter.DISTINCT.status)
                .build(), null).getTotalCount()).isEqualTo(2);
        assertThat(buildDatabaseService.groupByAndCalculateTestcase(COMPANY, AWSDevToolsTestcasesFilter.builder()
                .across(AWSDevToolsTestcasesFilter.DISTINCT.report_arn)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.groupByAndCalculateTestcase(COMPANY, AWSDevToolsTestcasesFilter.builder()
                .across(AWSDevToolsTestcasesFilter.DISTINCT.region)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.groupByAndCalculateTestcase(COMPANY, AWSDevToolsTestcasesFilter.builder()
                .across(AWSDevToolsTestcasesFilter.DISTINCT.project_name)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.groupByAndCalculateTestcase(COMPANY, AWSDevToolsTestcasesFilter.builder()
                .across(AWSDevToolsTestcasesFilter.DISTINCT.source_type)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.groupByAndCalculateTestcase(COMPANY, AWSDevToolsTestcasesFilter.builder()
                .across(AWSDevToolsTestcasesFilter.DISTINCT.initiator)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.groupByAndCalculateTestcase(COMPANY, AWSDevToolsTestcasesFilter.builder()
                .across(AWSDevToolsTestcasesFilter.DISTINCT.build_batch_arn)
                .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.groupByAndCalculateTestcase(COMPANY, AWSDevToolsTestcasesFilter.builder()
                .across(AWSDevToolsTestcasesFilter.DISTINCT.status)
                .calculation(AWSDevToolsTestcasesFilter.CALCULATION.duration)
                .build(), null).getRecords().stream()
                .map(DbAggregationResult::getMax)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(31);

    }

    @Test
    public void testTestcaseStackedGroupBy() throws SQLException {
        assertThat(buildDatabaseService.testcaseStackedGroupBy(COMPANY, AWSDevToolsTestcasesFilter.builder()
                        .across(AWSDevToolsTestcasesFilter.DISTINCT.status)
                        .build(),
                List.of(AWSDevToolsTestcasesFilter.DISTINCT.status), null)
                .getTotalCount()).isEqualTo(2);
        assertThat(buildDatabaseService.testcaseStackedGroupBy(COMPANY, AWSDevToolsTestcasesFilter.builder()
                        .across(AWSDevToolsTestcasesFilter.DISTINCT.report_arn)
                        .build(),
                List.of(AWSDevToolsTestcasesFilter.DISTINCT.report_arn), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.testcaseStackedGroupBy(COMPANY, AWSDevToolsTestcasesFilter.builder()
                        .across(AWSDevToolsTestcasesFilter.DISTINCT.region)
                        .build(),
                List.of(AWSDevToolsTestcasesFilter.DISTINCT.region), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.testcaseStackedGroupBy(COMPANY, AWSDevToolsTestcasesFilter.builder()
                        .across(AWSDevToolsTestcasesFilter.DISTINCT.project_name)
                        .build(),
                List.of(AWSDevToolsTestcasesFilter.DISTINCT.project_name), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.testcaseStackedGroupBy(COMPANY, AWSDevToolsTestcasesFilter.builder()
                        .across(AWSDevToolsTestcasesFilter.DISTINCT.source_type)
                        .build(),
                List.of(AWSDevToolsTestcasesFilter.DISTINCT.source_type), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.testcaseStackedGroupBy(COMPANY, AWSDevToolsTestcasesFilter.builder()
                        .across(AWSDevToolsTestcasesFilter.DISTINCT.initiator)
                        .build(),
                List.of(AWSDevToolsTestcasesFilter.DISTINCT.initiator), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.testcaseStackedGroupBy(COMPANY, AWSDevToolsTestcasesFilter.builder()
                        .across(AWSDevToolsTestcasesFilter.DISTINCT.build_batch_arn)
                        .build(),
                List.of(AWSDevToolsTestcasesFilter.DISTINCT.build_batch_arn), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(buildDatabaseService.testcaseStackedGroupBy(COMPANY, AWSDevToolsTestcasesFilter.builder()
                        .across(AWSDevToolsTestcasesFilter.DISTINCT.status)
                        .calculation(AWSDevToolsTestcasesFilter.CALCULATION.duration)
                        .build(),
                List.of(AWSDevToolsTestcasesFilter.DISTINCT.status), null)
                .getRecords().stream()
                .map(DbAggregationResult::getMax)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(31);
    }
}
