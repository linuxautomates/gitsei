package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.PaginationStrategy;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.storage.models.StorageResult;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.TestPlan;
import io.levelops.integrations.testrails.models.TestRailsQuery;
import io.levelops.integrations.testrails.models.TestRailsTestSuite;
import io.levelops.integrations.testrails.models.TestRun;
import io.levelops.integrations.testrails.models.CaseField;
import io.levelops.integrations.testrails.models.User;
import io.levelops.integrations.testrails.sources.TestRailsCaseFieldDataSource;
import io.levelops.integrations.testrails.sources.TestRailsProjectDataSource;
import io.levelops.integrations.testrails.sources.TestRailsTestSuiteDataSource;
import io.levelops.integrations.testrails.sources.TestRailsTestPlanDataSource;
import io.levelops.integrations.testrails.sources.TestRailsTestRunDataSource;
import io.levelops.integrations.testrails.sources.TestRailsUserDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * TestRails's implementation of the {@link DataController}
 */
@Log4j2
public class TestRailsController implements DataController<TestRailsQuery> {

    private static final String INTEGRATION_TYPE = "testrails";
    private static final String DATATYPE_USERS = "users";
    private static final String DATATYPE_PROJECTS = "projects";
    private static final String DATATYPE_TEST_PLANS = "test_plans";
    private static final String DATATYPE_TEST_RUNS = "test_runs";
    private static final String DATATYPE_CASE_FIELDS = "case_fields";
    private static final String DATATYPE_TEST_SUITES = "test_suites";

    private final ObjectMapper objectMapper;
    private final PaginationStrategy<Project, TestRailsQuery> projectPaginationStrategy;
    private final PaginationStrategy<User, TestRailsQuery> userPaginationStrategy;
    private final PaginationStrategy<TestPlan, TestRailsQuery> testPlanPaginationStrategy;
    private final PaginationStrategy<TestRun, TestRailsQuery> testRunPaginationStrategy;
    private final PaginationStrategy<CaseField, TestRailsQuery> caseFieldPaginationStrategy;
    private final PaginationStrategy<TestRailsTestSuite, TestRailsQuery> testSuitePaginationStrategy;
    private final int onboardingScanInDays;

    @Builder
    public TestRailsController(ObjectMapper objectMapper, TestRailsProjectDataSource projectDataSource,
                               TestRailsUserDataSource userDataSource, TestRailsTestPlanDataSource testPlanDataSource,
                               TestRailsTestRunDataSource testRunDataSource, TestRailsCaseFieldDataSource caseFieldDataSource,
                               TestRailsTestSuiteDataSource testSuiteDataSource,
                               StorageDataSink storageDataSink,
                               int onboardingScanInDays) {
        this.objectMapper = objectMapper;
        this.onboardingScanInDays = onboardingScanInDays != 0 ? onboardingScanInDays : 90;
        this.userPaginationStrategy = StreamedPaginationStrategy.<User, TestRailsQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_USERS)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(userDataSource)
                .skipEmptyResults(true)
                .build();
        this.projectPaginationStrategy = StreamedPaginationStrategy.<Project, TestRailsQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_PROJECTS)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(projectDataSource)
                .skipEmptyResults(true)
                .build();
        this.testPlanPaginationStrategy = StreamedPaginationStrategy.<TestPlan, TestRailsQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_TEST_PLANS)
                .outputPageSize(1) // Per file 1 test plan with batch size of 10 test runs with batch size of 100 tests
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(testPlanDataSource)
                .skipEmptyResults(true)
                .build();
        this.testRunPaginationStrategy = StreamedPaginationStrategy.<TestRun, TestRailsQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_TEST_RUNS)
                .outputPageSize(10) // Per file 10 test runs with batch size of 100 tests
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(testRunDataSource)
                .skipEmptyResults(true)
                .build();
        this.caseFieldPaginationStrategy = StreamedPaginationStrategy.<CaseField, TestRailsQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_CASE_FIELDS)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(caseFieldDataSource)
                .skipEmptyResults(true)
                .build();
        this.testSuitePaginationStrategy = StreamedPaginationStrategy.<TestRailsTestSuite, TestRailsQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_TEST_SUITES)
                .outputPageSize(5) // Per file 5 records of test suites with batch size of 100 test cases
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(testSuiteDataSource)
                .skipEmptyResults(true)
                .build();
    }

    /**
     * parses the {@code arg0}
     *
     * @param arg {@link Object} corresponding to the required {@link TestRailsQuery}
     * @return {@link TestRailsQuery} for the job
     */
    @Override
    public TestRailsQuery parseQuery(Object arg) {
        log.info("parseQuery: received args: {}", arg);
        TestRailsQuery query = objectMapper.convertValue(arg, TestRailsQuery.class);
        log.info("parseQuery: parsed query successfully: {}", query);
        return query;
    }

    /**
     * Ingests the data for {@code jobId} with the {@code query}.
     * It calls the {@link TestRailsUserDataSource} for fetching the users.
     * It calls the {@link TestRailsProjectDataSource} for fetching the projects.
     * It calls the {@link TestRailsTestPlanDataSource} for fetching the test plans.
     * It calls the {@link TestRailsTestRunDataSource} for fetching the test runs.
     * It calls the {@link TestRailsCaseFieldDataSource} for fetching the case fields.
     * It calls the {@link TestRailsTestSuiteDataSource} for fetching the test suites.
     *
     * @param jobContext {@link JobContext} jobContext object
     * @param query {@link TestRailsQuery} describing the job
     * @return {@link ControllerIngestionResult} for the executed job
     * @throws IngestException for any exception during the ingestion process
     */
    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, TestRailsQuery query) throws IngestException {
        log.info("ingest: ingesting data for jobId: {} with query: {}", jobContext.getJobId(), query);
        TestRailsQuery testRailsQuery;
        if (query.getFrom() == null) {
            testRailsQuery = TestRailsQuery.builder()
                    .from(Date.from(Instant.now().minus(onboardingScanInDays, ChronoUnit.DAYS)))
                    .integrationKey(query.getIntegrationKey())
                    .shouldFetchUsers(true)
                    .shouldFetchTestExecutions(false)
                    .build();
        } else {
            testRailsQuery = query;
        }
        boolean shouldFetchUser = Boolean.TRUE.equals(query.getShouldFetchUsers());
        boolean shouldFetchExecution = Boolean.TRUE.equals(query.getShouldFetchTestExecutions());
        StorageResult projectStorageResult = projectPaginationStrategy.ingestAllPages(jobContext,
                query.getIntegrationKey(), testRailsQuery);
        StorageResult caseFieldStorageResult = caseFieldPaginationStrategy.ingestAllPages(jobContext,
                query.getIntegrationKey(), testRailsQuery);
        StorageResult testSuiteStorageResult = testSuitePaginationStrategy.ingestAllPages(jobContext,
                query.getIntegrationKey(), testRailsQuery);
        if (shouldFetchUser) {
            StorageResult userStorageResult = userPaginationStrategy.ingestAllPages(jobContext,
                    query.getIntegrationKey(), query);
            if (shouldFetchExecution){
                StorageResult testPlanStorageResult = testPlanPaginationStrategy.ingestAllPages(jobContext,
                        query.getIntegrationKey(), testRailsQuery);
                StorageResult testRunStorageResult = testRunPaginationStrategy.ingestAllPages(jobContext,
                        query.getIntegrationKey(), testRailsQuery);
                return new ControllerIngestionResultList(userStorageResult, testPlanStorageResult, testRunStorageResult, projectStorageResult, caseFieldStorageResult, testSuiteStorageResult);
            }
            return new ControllerIngestionResultList(userStorageResult, projectStorageResult, caseFieldStorageResult, testSuiteStorageResult);
        }
        if (shouldFetchExecution){
            StorageResult testPlanStorageResult = testPlanPaginationStrategy.ingestAllPages(jobContext,
                    query.getIntegrationKey(), testRailsQuery);
            StorageResult testRunStorageResult = testRunPaginationStrategy.ingestAllPages(jobContext,
                    query.getIntegrationKey(), testRailsQuery);
            return new ControllerIngestionResultList(testPlanStorageResult, testRunStorageResult, projectStorageResult, caseFieldStorageResult, testSuiteStorageResult);
        }
        return new ControllerIngestionResultList(projectStorageResult, caseFieldStorageResult, testSuiteStorageResult);
    }
}
