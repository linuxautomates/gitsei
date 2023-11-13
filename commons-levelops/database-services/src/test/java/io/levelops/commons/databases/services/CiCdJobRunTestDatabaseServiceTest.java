package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CiCdJobRunTest;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.jenkins.JUnitTestReport;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobRunTestsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.commons.xml.DefaultXmlMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unused")
public class CiCdJobRunTestDatabaseServiceTest {

    private static final String COMPANY = "test";
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private CiCdJobRunTestDatabaseService testDatabaseService;
    private CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private IntegrationService integrationService;
    private ProductsDatabaseService productsDatabaseService;
    String jobId1, jobId2;
    String jobRunId1, jobRunId2;
    String instanceId1, instanceId2;
    String integrationId1, integrationId2;

    @Before
    public void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        integrationService = new IntegrationService(dataSource);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(OBJECT_MAPPER, dataSource);
        testDatabaseService = new CiCdJobRunTestDatabaseService(dataSource);
        UserService userService = new UserService(dataSource, OBJECT_MAPPER);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        Integration integration1 = Integration.builder()
                .id("1")
                .name("name-1")
                .url("http")
                .status("good")
                .application("azure_devops")
                .description("desc")
                .satellite(true)
                .build();
        Integration integration2 = Integration.builder()
                .id("2")
                .name("name-2")
                .url("http")
                .status("good")
                .application("jenkins")
                .description("desc")
                .satellite(true)
                .build();
        integrationId1 = integrationService.insert(COMPANY, integration1);
        integrationId2 = integrationService.insert(COMPANY, integration2);
        productsDatabaseService = new ProductsDatabaseService(dataSource, OBJECT_MAPPER);
        productsDatabaseService.ensureTableExistence(COMPANY);
        userService.ensureTableExistence(COMPANY);
        ciCdInstancesDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobsDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobRunsDatabaseService.ensureTableExistence(COMPANY);
        testDatabaseService.ensureTableExistence(COMPANY);
        final String userId = userService.insert(COMPANY, User.builder()
                .userType(RoleType.LIMITED_USER)
                .bcryptPassword("asd")
                .email("asd@asd.com")
                .passwordAuthEnabled(Boolean.FALSE)
                .samlAuthEnabled(false)
                .firstName("asd")
                .lastName("asd")
                .build());
        instanceId1 = ciCdInstancesDatabaseService.insert(COMPANY, CICDInstance.builder()
                .id(UUID.randomUUID())
                .name("test-instance")
                .url("testUrl")
                .integrationId(integrationId1)
                .type(CICD_TYPE.azure_devops.toString())
                .build());
        jobId1 = ciCdJobsDatabaseService.insert(COMPANY, CICDJob.builder()
                .jobName("testJobName")
                .jobFullName("testJobName")
                .jobNormalizedFullName("testJobName")
                .projectName("project-1")
                .cicdInstanceId(UUID.fromString(instanceId1))
                .build());
        final Date date = new Date();
        jobRunId1 = ciCdJobRunsDatabaseService.insert(COMPANY, CICDJobRun.builder()
                .cicdJobId(UUID.fromString(jobId1))
                .jobRunNumber(20L)
                .status("SUCCESS")
                .scmCommitIds(Collections.emptyList())
                .startTime(Instant.ofEpochSecond(1611075448))
                .endTime(Instant.ofEpochSecond(1614976448))
                .duration(100)
                .cicdUserId("testUser")
                .source(CICDJobRun.Source.JOB_RUN_COMPLETE_EVENT)
                .build());
        instanceId2 = ciCdInstancesDatabaseService.insert(COMPANY, CICDInstance.builder()
                .id(UUID.randomUUID())
                .name("test-instance")
                .url("testUrl")
                .integrationId(integrationId2)
                .type(CICD_TYPE.jenkins.toString())
                .build());
        jobId2 = ciCdJobsDatabaseService.insert(COMPANY, CICDJob.builder()
                .jobName("testJobName")
                .jobFullName("testJobName")
                .jobNormalizedFullName("testJobName")
                .projectName("project-2")
                .cicdInstanceId(UUID.fromString(instanceId2))
                .build());
        jobRunId2 = ciCdJobRunsDatabaseService.insert(COMPANY, CICDJobRun.builder()
                .cicdJobId(UUID.fromString(jobId2))
                .jobRunNumber(20L)
                .status("FAILED")
                .scmCommitIds(Collections.emptyList())
                .startTime(Instant.ofEpochSecond(1611075448))
                .endTime(Instant.ofEpochSecond(1614976448))
                .duration(100)
                .cicdUserId("testUser")
                .source(CICDJobRun.Source.JOB_RUN_COMPLETE_EVENT)
                .build());
        final String successXml = ResourceUtils.getResourceAsString(
                "json/databases/junit_test_results/TEST-io.levelops.internal_api.controllers.ComponentsControllerTest.xml");
        final String failureXml = ResourceUtils.getResourceAsString(
                "json/databases/junit_test_results/TEST-io.levelops.internal_api.controllers.DatabaseSetupControllerInjectionTest.xml");
        final JUnitTestReport successReport = DefaultXmlMapper.getXmlMapper().readValue(successXml, JUnitTestReport.class);
        final JUnitTestReport failureReport = DefaultXmlMapper.getXmlMapper().readValue(failureXml, JUnitTestReport.class);
        final UUID cicdJobRunId1 = UUID.fromString(jobRunId1);
        final UUID cicdJobRunId2 = UUID.fromString(jobRunId2);
        final List<CiCdJobRunTest> ciCdJobRunTests1 = List.of(successReport, failureReport).stream()
                .flatMap(report -> CiCdJobRunTest.fromJUnitTestSuite(report, cicdJobRunId1).stream())
                .collect(Collectors.toList());
        final List<String> insertedIds1 = testDatabaseService.batchInsert(COMPANY, ciCdJobRunTests1);

        final List<CiCdJobRunTest> ciCdJobRunTests2 = List.of(successReport, failureReport).stream()
                .flatMap(report -> CiCdJobRunTest.fromJUnitTestSuite(report, cicdJobRunId2).stream())
                .collect(Collectors.toList());
        final List<String> insertedIds2 = testDatabaseService.batchInsert(COMPANY, ciCdJobRunTests2);
        if (insertedIds1.size() != ciCdJobRunTests1.size() && insertedIds2.size() != ciCdJobRunTests1.size()) {
            throw new RuntimeException("All the tests must be inserted successfully");
        }

    }

    @Test
    public void testCiCdJobRunTestExcludeFilters() {
        DbListResponse<CiCdJobRunTest> dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .excludeJobNames(List.of("testJobName1"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(10, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .jobNames(List.of("testJobName1"))
                        .excludeJobNames(List.of("testJobName1"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .excludeProjects(List.of("project-2"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(5, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .projects(List.of("project-2"))
                        .excludeProjects(List.of("project-2"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .excludeJobStatuses(List.of("FAILURE"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(10, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .jobStatuses(List.of("FAILURE"))
                        .excludeJobStatuses(List.of("FAILURE"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .excludeInstanceNames(List.of("test-instance-0"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(10, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .instanceNames(List.of("test-instance-0"))
                        .excludeInstanceNames(List.of("test-instance-0"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .excludeTestStatuses(List.of("PASSED"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(2, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .testStatuses(List.of("PASSED"))
                        .excludeTestStatuses(List.of("PASSED"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .excludeTestSuites(List.of("io.levelops.internal_api.controllers.ComponentsControllerTest"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(6, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .testSuites(List.of("io.levelops.internal_api.controllers.ComponentsControllerTest"))
                        .excludeTestSuites(List.of("io.levelops.internal_api.controllers.ComponentsControllerTest"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .excludeTypes(List.of(CICD_TYPE.azure_devops))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(5, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .types(List.of(CICD_TYPE.azure_devops))
                        .excludeTypes(List.of(CICD_TYPE.azure_devops))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .excludeJobRunIds(List.of("e14b4218-2bb5-4830-8fe3-7ac6bf8f7157"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(10, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .jobRunIds(List.of("e14b4218-2bb5-4830-8fe3-7ac6bf8f7157"))
                        .excludeJobRunIds(List.of("e14b4218-2bb5-4830-8fe3-7ac6bf8f7157"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .excludeJobRunNumbers(List.of("30"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(10, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .jobRunNumbers(List.of("30"))
                        .excludeJobRunNumbers(List.of("30"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .excludeCiCdUserIds(List.of("testUser1"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(10, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .cicdUserIds(List.of("testUser1"))
                        .excludeCiCdUserIds(List.of("testUser1"))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
    }

   // @Test
    public void test() throws SQLException {
        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .build(), 0, 100).getTotalCount()).isEqualTo(10);
        // test filters
        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .cicdUserIds(List.of("testUser1"))
                .build(), 0, 100).getTotalCount()).isEqualTo(0);
        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .jobNames(List.of("testJobName"))
                .build(), 0, 100).getTotalCount()).isEqualTo(10);
        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("FAILED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .build(), 0, 100).getTotalCount()).isEqualTo(2);
        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .build(), 0, 100).getTotalCount()).isEqualTo(8);
        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .instanceNames(List.of("test-instance"))
                .build(), 0, 100).getTotalCount()).isEqualTo(8);
        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .instanceNames(List.of("does-not-exist"))
                .build(), 0, 100).getTotalCount()).isEqualTo(0);
        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .instanceNames(List.of("test-instance"))
                .types(List.of(CICD_TYPE.azure_devops))
                .build(), 0, 100).getTotalCount()).isEqualTo(4);
        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .instanceNames(List.of("test-instance"))
                .types(List.of(CICD_TYPE.jenkins))
                .build(), 0, 100).getTotalCount()).isEqualTo(4);

        // test aggregations
        assertThat(testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.test_status)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build()).getTotalCount()).isEqualTo(2);
        assertThat(testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.test_suite)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.duration)
                .build()).getTotalCount()).isEqualTo(2);
        DbListResponse<DbAggregationResult> response = testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .instanceNames(List.of("test-instance"))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.instance_name)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build());
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getRecords().get(0).getKey()).isEqualTo("test-instance");
        DbListResponse<DbAggregationResult> aggResponse = testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .instanceNames(List.of("test-instance"))
                .integrationIds(List.of("1"))
                .types(List.of(CICD_TYPE.azure_devops))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_end)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build());
        DefaultObjectMapper.prettyPrint(aggResponse);
        assertThat(aggResponse.getTotalCount()).isEqualTo(1);
        assertThat(aggResponse.getRecords().get(0).getKey()).isEqualTo("1614931200");
        assertThat(aggResponse.getRecords().get(0).getAdditionalKey()).isEqualTo("5-3-2021");

        aggResponse = testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .instanceNames(List.of("test-instance"))
                .integrationIds(List.of("1"))
                .types(List.of(CICD_TYPE.azure_devops))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_end)
                .aggInterval(CICD_AGG_INTERVAL.week)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build());
        DefaultObjectMapper.prettyPrint(aggResponse);
        assertThat(aggResponse.getTotalCount()).isEqualTo(1);
        assertThat(aggResponse.getRecords().get(0).getKey()).isEqualTo("1614585600");
        assertThat(aggResponse.getRecords().get(0).getAdditionalKey()).isEqualTo("9-2021");

        aggResponse = testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .instanceNames(List.of("test-instance"))
                .integrationIds(List.of("1"))
                .types(List.of(CICD_TYPE.azure_devops))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_end)
                .aggInterval(CICD_AGG_INTERVAL.month)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build());
        DefaultObjectMapper.prettyPrint(aggResponse);
        assertThat(aggResponse.getTotalCount()).isEqualTo(1);
        assertThat(aggResponse.getRecords().get(0).getKey()).isEqualTo("1614585600");
        assertThat(aggResponse.getRecords().get(0).getAdditionalKey()).isEqualTo("3-2021");

        aggResponse = testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .instanceNames(List.of("test-instance"))
                .integrationIds(List.of("1"))
                .types(List.of(CICD_TYPE.azure_devops))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_end)
                .aggInterval(CICD_AGG_INTERVAL.quarter)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build());
        DefaultObjectMapper.prettyPrint(aggResponse);
        assertThat(aggResponse.getTotalCount()).isEqualTo(1);
        assertThat(aggResponse.getRecords().get(0).getKey()).isEqualTo("1609488000");
        assertThat(aggResponse.getRecords().get(0).getAdditionalKey()).isEqualTo("Q1-2021");

        aggResponse = testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .instanceNames(List.of("test-instance"))
                .integrationIds(List.of("1"))
                .types(List.of(CICD_TYPE.azure_devops))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_end)
                .aggInterval(CICD_AGG_INTERVAL.year)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build());
        assertThat(aggResponse.getTotalCount()).isEqualTo(1);
        assertThat(aggResponse.getRecords().get(0).getKey()).isEqualTo("1609488000");
        assertThat(aggResponse.getRecords().get(0).getAdditionalKey()).isEqualTo("2021");

        aggResponse = testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .endTimeRange(ImmutablePair.of(1614976447L, 1614976449L))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.cicd_user_id)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build());
        assertThat(aggResponse.getTotalCount()).isEqualTo(1);

        aggResponse = testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .endTimeRange(ImmutablePair.of(1614976448L, 1614976450L))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_name)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build());
        assertThat(aggResponse.getTotalCount()).isEqualTo(0);

        aggResponse = testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .startTimeRange(ImmutablePair.of(1611065447L, 1611085000L))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_status)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build());
        assertThat(aggResponse.getTotalCount()).isEqualTo(2);

        aggResponse = testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .startTimeRange(ImmutablePair.of(1611075440L, 1611075442L))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_name)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build());
        assertThat(aggResponse.getTotalCount()).isEqualTo(0);

        List<DbAggregationResult> aggs = testDatabaseService.stackedGroupBy(COMPANY, CiCdJobRunTestsFilter.builder()
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.test_suite).
                                CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count).build(),
                List.of(CiCdJobRunTestsFilter.DISTINCT.test_status)).getRecords();
        assertThat(aggs.get(0).getStacks().get(0).getTotalTests()).isEqualTo(2);
        assertThat(aggs.get(0).getStacks().get(1).getTotalTests()).isEqualTo(4);
        assertThat(aggs.get(1).getStacks().get(0).getTotalTests()).isEqualTo(4);

        aggs = testDatabaseService.stackedGroupBy(COMPANY, CiCdJobRunTestsFilter.builder()
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.test_status).
                                CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count).build(),
                List.of(CiCdJobRunTestsFilter.DISTINCT.test_suite)).getRecords();
        assertThat(aggs.get(0).getStacks().get(0).getTotalTests()).isEqualTo(4);
        assertThat(aggs.get(0).getStacks().get(1).getTotalTests()).isEqualTo(4);
        assertThat(aggs.get(1).getStacks().get(0).getTotalTests()).isEqualTo(2);

        response = testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .instanceNames(List.of("test-instance"))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.project_name)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build());
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getTotalCount()).isEqualTo(2);
        assertThat(response.getRecords().get(0).getKey()).isEqualTo("project-1");

        DbListResponse<DbAggregationResult> aggsResponse = testDatabaseService.stackedGroupBy(COMPANY, CiCdJobRunTestsFilter.builder()
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_status)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count).build(),
                List.of(CiCdJobRunTestsFilter.DISTINCT.project_name));
        DefaultObjectMapper.prettyPrint(aggsResponse);
        assertThat(aggsResponse.getRecords().get(0).getStacks().get(0).getKey()).isEqualTo("project-1");
        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("testUser"))
                .projects(List.of("project-1"))
                .build(), 0, 100).getTotalCount()).isEqualTo(4);

        String instanceId = ciCdInstancesDatabaseService.insert(COMPANY, CICDInstance.builder()
                .id(UUID.randomUUID())
                .name("test-instance1")
                .url("testUrl")
                .integrationId("1")
                .type(CICD_TYPE.azure_devops.toString()).build());
        String jobId = ciCdJobsDatabaseService.insert(COMPANY, CICDJob.builder()
                .jobName("testJobName1")
                .jobFullName("testJobName")
                .jobNormalizedFullName("testJobName")
                .projectName("project-1")
                .cicdInstanceId(UUID.fromString(instanceId)).build());
        String jobRunId = ciCdJobRunsDatabaseService.insert(COMPANY, CICDJobRun.builder()
                .cicdJobId(UUID.fromString(jobId))
                .jobRunNumber(25L)
                .status("SUCCESS")
                .scmCommitIds(Collections.emptyList())
                .startTime(Instant.ofEpochSecond(1611075448))
                .endTime(Instant.ofEpochSecond(1614976448))
                .duration(-2)
                .cicdUserId("testUser1")
                .source(CICDJobRun.Source.JOB_RUN_COMPLETE_EVENT).build());
        assertThat(ciCdJobRunsDatabaseService.get(COMPANY, jobRunId).get().getDuration()).isEqualTo(0);
    }

    @Test
    public void testJobRunTestProductFilters() throws SQLException {
        DBOrgProduct orgProduct1 = DBOrgProduct.builder()
                .name("product-1")
                .description("prod-1")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId1))
                        .name("name-1")
                        .type("azure_devops")
                        .filters(Map.of("job_names", List.of("testJobName"),
                                "job_statuses", List.of("SUCCESS")))
                        .build())).build();

        DBOrgProduct orgProduct2 = DBOrgProduct.builder()
                .name("product-2")
                .description("prod-2")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId1))
                        .name("name-1")
                        .type("azure_devops")
                        .filters(Map.of("job_names", List.of("jobname-12"),
                                "projects", List.of("project-123"),
                                "job_statuses", List.of("SUCCESS")))
                        .build())).build();
        DBOrgProduct orgProduct3 = DBOrgProduct.builder()
                .name("product-3")
                .description("prod-3")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId1))
                        .name("name-1")
                        .type("azure_devops")
                        .filters(Map.of("job_statuses", List.of("FAILURE")))
                        .build()))
                .build();
        DBOrgProduct orgProduct4 = DBOrgProduct.builder()
                .name("product-4")
                .description("prod-4")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId2))
                        .name("name-2")
                        .type("jenkins")
                        .filters(Map.of("job_names", List.of("testJobName"),
                                "test_statuses", List.of("PASSED"),
                                "projects", List.of("project-2")))
                        .build()))
                .build();
        String orgProductId1 = productsDatabaseService.insert(COMPANY, orgProduct1);
        String orgProductId2 = productsDatabaseService.insert(COMPANY, orgProduct2);
        String orgProductId3 = productsDatabaseService.insert(COMPANY, orgProduct3);
        String orgProductId4 = productsDatabaseService.insert(COMPANY, orgProduct4);


        // endregion
        //Without stacks

        var response = testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.project_name)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build());
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getTotalCount()).isEqualTo(1);

        var dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.duration)
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.test_status)
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                        .build());
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1), UUID.fromString(orgProductId4)))
                .build());
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.duration)
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.cicd_user_id)
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                        .build());
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.project_name)
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                        .build());
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.project_name)
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId4)))
                        .build());
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        //negative
        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.cicd_user_id)
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId2)))
                        .build());
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.test_status)
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId3)))
                        .build());
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());


        //With stacks
        //positive test
        dbAggsResponse = testDatabaseService.stackedGroupBy(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_name)
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                        .build(), List.of(CiCdJobRunTestsFilter.DISTINCT.project_name));
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = testDatabaseService.stackedGroupBy(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.duration)
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_status)
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId1), UUID.fromString(orgProductId4)))
                        .build(), List.of(CiCdJobRunTestsFilter.DISTINCT.instance_name));
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = testDatabaseService.stackedGroupBy(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.duration)
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.project_name)
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId4)))
                        .build(), List.of(CiCdJobRunTestsFilter.DISTINCT.job_name));
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = testDatabaseService.stackedGroupBy(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.project_name)
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId1), UUID.fromString(orgProductId4)))
                        .build(), List.of(CiCdJobRunTestsFilter.DISTINCT.test_suite));
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());

        //negative
        dbAggsResponse = testDatabaseService.stackedGroupBy(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.duration)
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.test_status)
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId3)))
                        .build(), List.of(CiCdJobRunTestsFilter.DISTINCT.test_suite));
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = testDatabaseService.stackedGroupBy(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_name)
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId2)))
                        .build(), List.of(CiCdJobRunTestsFilter.DISTINCT.test_status));
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());

        //list
        var dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(5, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId1), UUID.fromString(orgProductId4)))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(9, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId4), UUID.fromString(orgProductId1)))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(9, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId2)))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId3)))
                        .build(), 0, 2);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

    }

    @Test
    public void testSortBy() throws SQLException {
        var dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_status)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("job_status", SortingOrder.ASC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_status)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("job_status", SortingOrder.DESC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_status)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("count", SortingOrder.DESC)
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getTotalTests).reversed());

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_name)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("job_name", SortingOrder.ASC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_name)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("job_name", SortingOrder.DESC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_name)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.duration)
                        .build(), Map.of("duration", SortingOrder.DESC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getMedian).reversed());

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.test_status)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("test_status", SortingOrder.ASC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.test_status)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("test_status", SortingOrder.DESC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.test_suite)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("test_suite", SortingOrder.ASC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.instance_name)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("instance_name", SortingOrder.ASC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.instance_name)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("instance_name", SortingOrder.DESC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.cicd_user_id)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("cicd_user_id", SortingOrder.ASC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_end)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("job_end", SortingOrder.ASC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.project_name)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("project_name", SortingOrder.ASC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.project_name)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("project_name", SortingOrder.DESC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.trend)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("trend", SortingOrder.ASC)
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = testDatabaseService.groupByAndCalculate(COMPANY,
                CiCdJobRunTestsFilter.builder().DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_run_number)
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .build(), Map.of("job_run_number", SortingOrder.ASC)
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = testDatabaseService.stackedGroupBy(COMPANY,
                CiCdJobRunTestsFilter.builder()
                        .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                        .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.project_name)
                        .build(), List.of(CiCdJobRunTestsFilter.DISTINCT.test_suite),Map.of("project_name",SortingOrder.ASC), null);
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        // drilldown 

        DbListResponse<CiCdJobRunTest>  dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder().build(), Map.of("cicd_user_id", SortingOrder.ASC), 0, 10);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CiCdJobRunTest::getCicdUserId));

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder().build(), Map.of("cicd_user_id", SortingOrder.DESC), 0, 10);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CiCdJobRunTest::getCicdUserId).reversed());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder().build(), Map.of("duration", SortingOrder.ASC), 0, 10);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CiCdJobRunTest::getDuration));

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder().build(), Map.of("duration", SortingOrder.DESC), 0, 10);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CiCdJobRunTest::getDuration).reversed());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder().build(), Map.of("job_run_number", SortingOrder.ASC), 0, 10);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CiCdJobRunTest::getJobRunNumber));

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder().build(), Map.of("job_run_number", SortingOrder.DESC), 0, 10);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CiCdJobRunTest::getJobRunNumber).reversed());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder().build(), Map.of("job_status", SortingOrder.ASC), 0, 10);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CiCdJobRunTest::getJobStatus));

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder().build(), Map.of("job_status", SortingOrder.DESC), 0, 10);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CiCdJobRunTest::getJobStatus).reversed());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder().build(), Map.of("test_name", SortingOrder.ASC), 0, 10);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CiCdJobRunTest::getTestName));

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder().build(), Map.of("test_name", SortingOrder.DESC), 0, 10);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CiCdJobRunTest::getTestName).reversed());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder().build(), Map.of("test_suite", SortingOrder.ASC), 0, 10);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CiCdJobRunTest::getTestSuite));

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder().build(), Map.of("test_suite", SortingOrder.DESC), 0, 10);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CiCdJobRunTest::getTestSuite).reversed());

        dbListResponse = testDatabaseService.list(COMPANY,
                CiCdJobRunTestsFilter.builder().build(), Map.of("status", SortingOrder.ASC), 0, 10);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(ciCdJobRunTest -> ciCdJobRunTest.getStatus().toString()));

    }
}
