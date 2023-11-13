package io.levelops.aggregations_shared.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsCaseField;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTest;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTestCase;
import io.levelops.commons.databases.models.filters.TestRailsTestsFilter;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.TestRailsCaseFieldDatabaseService;
import io.levelops.commons.databases.services.TestRailsCustomCaseFieldConditionsBuilder;
import io.levelops.commons.databases.services.TestRailsProjectDatabaseService;
import io.levelops.commons.databases.services.TestRailsTestCaseDatabaseService;
import io.levelops.commons.databases.services.TestRailsTestPlanDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ListUtils;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.testrails.models.CaseField;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.TestPlan;
import io.levelops.integrations.testrails.models.TestRailsTestSuite;
import io.levelops.integrations.testrails.models.TestRun;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class TestRailsAggHelperServiceTest {

    private static DataSource dataSource;
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    private static final String company = "test";
    private static String integrationId = "1";
    private static TestRailsTestCaseDatabaseService testRailsTestCaseDatabaseService;
    private static TestRailsCaseFieldDatabaseService testRailsCaseFieldDatabaseService;
    private static TestRailsTestPlanDatabaseService testRailsTestPlanDatabaseService;
    private static TestRailsProjectDatabaseService testRailsProjectDatabaseService;
    private static TestRailsCustomCaseFieldConditionsBuilder testRailsCustomCaseFieldConditionsBuilder;
    private static IntegrationTrackingService trackingService;
    private static TestRailsAggHelperService testRailsAggHelperService;
    private static NamedParameterJdbcTemplate template;
    private static Date currentTime;

    @BeforeClass
    public static void setup() throws SQLException {
        if (dataSource != null) {
            return;
        }
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dataSource.getConnection().prepareStatement("DROP SCHEMA IF EXISTS " + company + " CASCADE; ").execute();
        dataSource.getConnection().prepareStatement("CREATE SCHEMA " + company + " ; ").execute();

        template = new NamedParameterJdbcTemplate(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        integrationId = integrationService.insert(company, Integration.builder()
                .application("testrails")
                .name("testrails test")
                .status("enabled")
                .build());
        trackingService = new IntegrationTrackingService(dataSource);
        trackingService.ensureTableExistence(company);
        testRailsTestCaseDatabaseService = new TestRailsTestCaseDatabaseService(mapper, dataSource);
        testRailsTestCaseDatabaseService.ensureTableExistence(company);
        testRailsCaseFieldDatabaseService = new TestRailsCaseFieldDatabaseService(dataSource);
        testRailsCaseFieldDatabaseService.ensureTableExistence(company);
        testRailsProjectDatabaseService = new TestRailsProjectDatabaseService(dataSource);
        testRailsProjectDatabaseService.ensureTableExistence(company);
        testRailsCustomCaseFieldConditionsBuilder = new TestRailsCustomCaseFieldConditionsBuilder(testRailsCaseFieldDatabaseService);
        testRailsTestPlanDatabaseService = new TestRailsTestPlanDatabaseService(mapper, dataSource, testRailsCustomCaseFieldConditionsBuilder, testRailsCaseFieldDatabaseService);
        testRailsTestPlanDatabaseService.ensureTableExistence(company);
        testRailsAggHelperService = new TestRailsAggHelperService(testRailsTestCaseDatabaseService,
                testRailsCaseFieldDatabaseService, testRailsTestPlanDatabaseService, testRailsProjectDatabaseService, trackingService);
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
        testRailsAggHelperService.createTempTable(company, integrationId, currentTime);
    }

    @Test
    public void testProcessTestRailsTestCases() throws IOException, SQLException {
        TestRailsTestSuite testRailsTestSuite = ResourceUtils.getResourceAsObject("testrails/test_suites.json", TestRailsTestSuite.class);
        testRailsAggHelperService.processTestRailsTestCases(testRailsTestSuite, company, integrationId, currentTime);
        DbListResponse<DbTestRailsTestCase> testCaseDbListResponse = testRailsTestCaseDatabaseService.list(company, 0, 10);
        Assert.assertEquals(testRailsTestSuite.getTestCases().size(), testCaseDbListResponse.getRecords().size());
    }

    @Test
    public void testProcessTestRailsCaseFields() throws IOException, SQLException {
        PaginatedResponse<CaseField> testRailsCaseFields = ResourceUtils.getResourceAsObject("testrails/case_fields.json",
                mapper.getTypeFactory().constructParametricType(PaginatedResponse.class, CaseField.class));
        for(var caseField: ListUtils.emptyIfNull(testRailsCaseFields.getResponse().getRecords())){
            testRailsAggHelperService.processTestRailsCaseFields(caseField, company, integrationId, currentTime);
        }
        DbListResponse<DbTestRailsCaseField> caseFieldDbListResponse = testRailsCaseFieldDatabaseService.list(company, 0, 10);
        Assert.assertEquals(testRailsCaseFields.getResponse().getRecords().size(), caseFieldDbListResponse.getRecords().size());

        // Test Temp Table Records
        String countSql = "SELECT count(*) FROM " + company + "." + (TestRailsCaseFieldDatabaseService.TESTRAILS_CASE_FIELD_TEMP_TABLE + integrationId + "_" + currentTime.toInstant().getEpochSecond());
        Integer count = template.queryForObject(countSql, new HashMap<>(), Integer.class);
        Assert.assertEquals(Optional.of(6).get(), count);
    }

    @Test
    public void testProcessTestRailsProjectsTestPlansTestRuns() throws IOException, SQLException {
        PaginatedResponse<Project> testRailsProject = ResourceUtils.getResourceAsObject("testrails/projects.json",
                mapper.getTypeFactory().constructParametricType(PaginatedResponse.class, Project.class));
        for(var project : ListUtils.emptyIfNull(testRailsProject.getResponse().getRecords())){
            testRailsAggHelperService.processTestRailsProjects(project, company, integrationId);
        }

        // Test processTestRailsTestPlans
        PaginatedResponse<TestPlan> testRailsTestPlan = ResourceUtils.getResourceAsObject("testrails/test_plans.json",
                mapper.getTypeFactory().constructParametricType(PaginatedResponse.class, TestPlan.class));
        for(var testPlan : ListUtils.emptyIfNull(testRailsTestPlan.getResponse().getRecords())){
            testRailsAggHelperService.processTestRailsTestPlans(testPlan, company, integrationId, currentTime);
        }
        int expectedTestCount = testRailsTestPlan.getResponse().getRecords().stream()
                .map(testPlan -> testPlan.getTestRuns().stream()
                        .flatMap(testRun -> Stream.of(testRun.getTests().size()))
                        .mapToInt(Integer::intValue)
                        .sum())
                .mapToInt(Integer::intValue)
                .sum();
        DbListResponse<DbTestRailsTest> testDbListResponse1 = testRailsTestPlanDatabaseService.list(company, TestRailsTestsFilter.builder().build(), Map.of(),0, 10);
        Assert.assertEquals(expectedTestCount , testDbListResponse1.getRecords().size());
        String testRunCountSql = "SELECT count(*) FROM " + company + "." + (TestRailsTestPlanDatabaseService.TESTRAILS_TEST_RUN_TEMP_TABLE + integrationId + "_" + currentTime.toInstant().getEpochSecond());
        String testCountSql = "SELECT count(*) FROM " + company + "." + (TestRailsTestPlanDatabaseService.TESTRAILS_TEST_TEMP_TABLE + integrationId + "_" + currentTime.toInstant().getEpochSecond());
        String testResultCountSql = "SELECT count(*) FROM " + company + "." + (TestRailsTestPlanDatabaseService.TESTRAILS_TEST_RESULT_TEMP_TABLE + integrationId + "_" + currentTime.toInstant().getEpochSecond());

        // Test Temp Table Records
        Integer testRunCount = template.queryForObject(testRunCountSql, new HashMap<>(), Integer.class);
        Assert.assertEquals(Optional.of(1).get(), testRunCount);
        Integer testCount = template.queryForObject(testCountSql, new HashMap<>(), Integer.class);
        Assert.assertEquals(Optional.of(1).get(), testCount);
        Integer testResultCount = template.queryForObject(testResultCountSql, new HashMap<>(), Integer.class);
        Assert.assertEquals(Optional.of(0).get(), testResultCount);

        // Test processTestRailsTestRuns
        PaginatedResponse<TestRun> testRailsTestRun = ResourceUtils.getResourceAsObject("testrails/test_runs.json",
                mapper.getTypeFactory().constructParametricType(PaginatedResponse.class, TestRun.class));
        for(var testRun: ListUtils.emptyIfNull(testRailsTestRun.getResponse().getRecords())){
            testRailsAggHelperService.processTestRailsTestRuns(testRun, company, integrationId, currentTime);
        }
        expectedTestCount = testRailsTestRun.getResponse().getRecords().stream()
                .map(testRun -> testRun.getTests().size())
                .mapToInt(Integer::intValue)
                .sum();
        DbListResponse<DbTestRailsTest> testDbListResponse2 = testRailsTestPlanDatabaseService.list(company, TestRailsTestsFilter.builder().build(), Map.of(), 0, 10);
        Assert.assertEquals(expectedTestCount, testDbListResponse2.getRecords().size());

        // Test Temp Table Records
        testRunCount = template.queryForObject(testRunCountSql, new HashMap<>(), Integer.class);
        Assert.assertEquals(Optional.of(5).get(), testRunCount);
        testCount = template.queryForObject(testCountSql, new HashMap<>(), Integer.class);
        Assert.assertEquals(Optional.of(9).get(), testCount);
        testResultCount = template.queryForObject(testResultCountSql, new HashMap<>(), Integer.class);
        Assert.assertEquals(Optional.of(9).get(), testResultCount);
    }
}
