package io.levelops.aggregations_shared.helpers;

import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsCaseField;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsProject;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTest;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTestCase;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTestPlan;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTestResult;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTestRun;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.TestRailsCaseFieldDatabaseService;
import io.levelops.commons.databases.services.TestRailsProjectDatabaseService;
import io.levelops.commons.databases.services.TestRailsTestCaseDatabaseService;
import io.levelops.commons.databases.services.TestRailsTestPlanDatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.utils.ListUtils;
import io.levelops.integrations.testrails.models.CaseField;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.TestPlan;
import io.levelops.integrations.testrails.models.TestRailsTestSuite;
import io.levelops.integrations.testrails.models.TestRun;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class TestRailsAggHelperService {

    private final TestRailsTestCaseDatabaseService testRailsTestCaseDatabaseService;
    private final TestRailsCaseFieldDatabaseService testRailsCaseFieldDatabaseService;
    private final TestRailsTestPlanDatabaseService testRailsTestPlanDatabaseService;
    private final TestRailsProjectDatabaseService testRailsProjectDatabaseService;
    private final IntegrationTrackingService trackingService;

    @Autowired
    public TestRailsAggHelperService(TestRailsTestCaseDatabaseService testRailsTestCaseDatabaseService,
                                     TestRailsCaseFieldDatabaseService testRailsCaseFieldDatabaseService,
                                     TestRailsTestPlanDatabaseService testRailsTestPlanDatabaseService,
                                     TestRailsProjectDatabaseService testRailsProjectDatabaseService,
                                     IntegrationTrackingService trackingService) {
        this.testRailsTestCaseDatabaseService = testRailsTestCaseDatabaseService;
        this.testRailsCaseFieldDatabaseService = testRailsCaseFieldDatabaseService;
        this.testRailsTestPlanDatabaseService = testRailsTestPlanDatabaseService;
        this.testRailsProjectDatabaseService = testRailsProjectDatabaseService;
        this.trackingService = trackingService;
    }

    public void createTempTable(String customer, String integrationId, Date currentTime) throws RuntimeException {
        testRailsTestCaseDatabaseService.createTempTable(customer, integrationId, String.valueOf(currentTime.toInstant().getEpochSecond()));
        testRailsTestPlanDatabaseService.createTempTables(customer, integrationId, String.valueOf(currentTime.toInstant().getEpochSecond()));
        testRailsCaseFieldDatabaseService.createTempTable(customer, integrationId, String.valueOf(currentTime.toInstant().getEpochSecond()));
    }
    public int deleteTestCaseRecords(String company, String integrationId, Date currentTime) throws RuntimeException {
        return testRailsTestCaseDatabaseService.deleteTestCaseRecords(company, integrationId, String.valueOf(currentTime.toInstant().getEpochSecond()));
    }
    public int deleteTestRecords(String company, String integrationId, Date currentTime) throws RuntimeException {
        return testRailsTestPlanDatabaseService.deleteTestRecords(company, integrationId, String.valueOf(currentTime.toInstant().getEpochSecond()));
    }
    public int deleteCaseFieldRecords(String company, String integrationId, Date currentTime) throws RuntimeException {
        return testRailsCaseFieldDatabaseService.deleteCaseFieldRecords(company, integrationId, String.valueOf(currentTime.toInstant().getEpochSecond()));
    }
    public void dropTempTable(String company, String integrationId, Date currentTime){
        try{
            testRailsTestCaseDatabaseService.dropTempTable(company, integrationId, String.valueOf(currentTime.toInstant().getEpochSecond()));
        }
        catch (RuntimeException e){
            log.error("postProcess: Failed to dropped temp table of test case tenant: " + company + "integration_id: " + integrationId, e);
        }
        try{
            testRailsTestPlanDatabaseService.dropTempTables(company, integrationId, String.valueOf(currentTime.toInstant().getEpochSecond()));
        }
        catch (RuntimeException e){
            log.error("postProcess: Failed to dropped temp table of test plan tenant: " + company + "integration_id: " + integrationId, e);
        }
        try {
            testRailsCaseFieldDatabaseService.dropTempTable(company, integrationId, String.valueOf(currentTime.toInstant().getEpochSecond()));
        }
        catch (RuntimeException e){
            log.error("postProcess: Failed to dropped temp table of case field tenant: " + company + "integration_id: " + integrationId, e);
        }
    }
    public void insertIntoTestCaseTempTable(String customer, String integrationId, Date currentTime, List<Integer> ids) throws RuntimeException {
        testRailsTestCaseDatabaseService.insertIntoTempTable(customer, integrationId, String.valueOf(currentTime.toInstant().getEpochSecond()), ids);
    }
    public void insertIntoTestRunTempTable(String customer, String integrationId, Date currentTime, List<Integer> ids) throws RuntimeException {
        testRailsTestPlanDatabaseService.insertIntoTestRunTempTable(customer, integrationId, String.valueOf(currentTime.toInstant().getEpochSecond()), ids);
    }
    public void insertIntoTestTempTable(String customer, String integrationId, Date currentTime, List<Integer> ids) throws RuntimeException {
        testRailsTestPlanDatabaseService.insertIntoTestTempTable(customer, integrationId, String.valueOf(currentTime.toInstant().getEpochSecond()), ids);
    }
    public void insertIntoTestResultTempTable(String customer, String integrationId, Date currentTime, List<Long> ids) throws RuntimeException {
        testRailsTestPlanDatabaseService.insertIntoTestResultTempTable(customer, integrationId, String.valueOf(currentTime.toInstant().getEpochSecond()), ids);
    }
    public void insertIntoCaseFieldTempTable(String customer, String integrationId, Date currentTime, Integer id) throws RuntimeException {
        testRailsCaseFieldDatabaseService.insertIntoTempTable(customer, integrationId, String.valueOf(currentTime.toInstant().getEpochSecond()), id);
    }

    public void processTestRailsTestCases(TestRailsTestSuite testSuite, String customer, String integrationId, Date currentTime) throws SQLException {
        try {
            List<DbTestRailsTestCase> testCases = testSuite.getTestCases().stream()
                    .map(testCase -> DbTestRailsTestCase.fromTestCase(testCase, integrationId))
                    .collect(Collectors.toList());
            testRailsTestCaseDatabaseService.insertBatch(customer, testCases);
            List<Integer> testCaseIds = testCases.stream().map(DbTestRailsTestCase::getCaseId).collect(Collectors.toList());
            insertIntoTestCaseTempTable(customer, integrationId, currentTime, testCaseIds);
        } catch (Exception e) {
            throw new SQLException("Failed to insert the test cases", e);
        }
        log.info("Processed the testrails testcase of project_id: {} for tenant: {} integration_id: {}", testSuite.getProjectId(), customer, integrationId);
    }

    public void processTestRailsCaseFields(CaseField caseField, String customer, String integrationId, Date currentTime) throws SQLException {
        try {
            DbTestRailsCaseField dbTestRailsCaseField = DbTestRailsCaseField.fromCaseField(caseField, integrationId);
            testRailsCaseFieldDatabaseService.insert(customer, dbTestRailsCaseField);
            insertIntoCaseFieldTempTable(customer, integrationId, currentTime, dbTestRailsCaseField.getCaseFieldId());
        } catch (Exception e) {
            throw new SQLException("processTestRailsCaseFields: error inserting caseField with id: " +
                    caseField.getId(), e);
        }
        log.info("Processed the testrails casefields for tenant: {} integration_id: {}", customer, integrationId);
    }

    public void processTestRailsTestPlans(TestPlan testPlan, String customer, String integrationId, Date currentTime) throws SQLException {
        try {
            DbTestRailsTestPlan dbTestPlan = DbTestRailsTestPlan.fromTestPlan(testPlan, integrationId);
            testRailsTestPlanDatabaseService.insertAndReturnId(customer, dbTestPlan);
            List<DbTestRailsTestRun> testRuns = dbTestPlan.getTestRuns();
            List<Integer> testRunIds = testRuns.stream().map(DbTestRailsTestRun::getRunId).collect(Collectors.toList());
            List<DbTestRailsTest> tests = testRuns.stream().map(DbTestRailsTestRun::getTests).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());
            List<Integer> testIds = tests.stream().map(DbTestRailsTest::getTestId).collect(Collectors.toList());
            List<Long> testResultIds = tests.stream().map(DbTestRailsTest::getResults).flatMap(Collection::stream).map(DbTestRailsTestResult::getResultId).collect(Collectors.toList());
            insertIntoTestRunTempTable(customer, integrationId, currentTime, testRunIds);
            insertIntoTestTempTable(customer, integrationId, currentTime, testIds);
            insertIntoTestResultTempTable(customer, integrationId, currentTime, testResultIds);
        } catch (Exception e) {
            throw new SQLException("processTestRailsTestPlans: error inserting test plan with id: " +
                    testPlan.getId(), e);
        }
        log.info("Processed the testrails testplans for tenant: {} integration_id: {}", customer, integrationId);
    }

    public void processTestRailsTestRuns(TestRun testRun, String customer, String integrationId, Date currentTime) throws SQLException {
        try {
            DbTestRailsTestRun dbTestRun = DbTestRailsTestRun.fromTestRun(testRun, integrationId, null);
            testRailsTestPlanDatabaseService.insertTestRun(customer, dbTestRun);
            insertIntoTestRunTempTable(customer, integrationId, currentTime, List.of(dbTestRun.getRunId()));
            List<DbTestRailsTest> tests = ListUtils.emptyIfNull(dbTestRun.getTests()).stream().filter(Objects::nonNull).collect(Collectors.toList());
            List<Integer> testIds = tests.stream().map(DbTestRailsTest::getTestId).collect(Collectors.toList());
            List<Long> testResultIds = tests.stream().map(DbTestRailsTest::getResults).flatMap(Collection::stream).map(DbTestRailsTestResult::getResultId).collect(Collectors.toList());
            insertIntoTestTempTable(customer, integrationId, currentTime, testIds);
            insertIntoTestResultTempTable(customer, integrationId, currentTime, testResultIds);
        } catch (Exception e) {
            throw new SQLException("processTestRailsTestRuns: error inserting test run with id: " +
                    testRun.getId(), e);
        }
        log.info("Processed the testrails testruns for tenant: {} integration_id: {}", customer, integrationId);
    }

    public void processTestRailsProjects(Project project, String customer, String integrationId) throws SQLException {
        try {
            DbTestRailsProject dbTestRailsProject = DbTestRailsProject.fromProject(project, integrationId);
            testRailsProjectDatabaseService.insertAndReturnId(customer, dbTestRailsProject);
        } catch (Exception e) {
            throw new SQLException("processTestRailsProjects: error inserting project with id: " +
                    project.getId(), e);
        }
        log.info("Processed the testrails projects for tenant: {} integration_id: {}", customer, integrationId);
    }
}