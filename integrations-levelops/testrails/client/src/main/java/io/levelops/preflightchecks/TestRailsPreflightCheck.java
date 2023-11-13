package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.exceptions.HttpException;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.testrails.client.TestRailsClient;
import io.levelops.integrations.testrails.client.TestRailsClientException;
import io.levelops.integrations.testrails.client.TestRailsClientFactory;
import io.levelops.integrations.testrails.models.Milestone;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.Test;
import io.levelops.integrations.testrails.models.TestPlan;
import io.levelops.integrations.testrails.models.TestRailsTestCase;
import io.levelops.integrations.testrails.models.TestRailsTestSuite;
import io.levelops.integrations.testrails.models.TestRun;
import io.levelops.integrations.testrails.models.User;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementation of the {@link PreflightCheck} for TestRails integration
 */
@Log4j2
@Component
public class TestRailsPreflightCheck implements PreflightCheck {

    private static final String TESTRAILS = "testrails";
    private static final int RESPONSE_PAGE_SIZE = 5;
    private static final int RESPONSE_PAGE_OFFSET = 0;

    private final TestRailsClientFactory clientFactory;

    @Autowired
    public TestRailsPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.clientFactory = TestRailsClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .pageSize(RESPONSE_PAGE_SIZE)
                .build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link TestRailsPreflightCheck#TESTRAILS}
     */
    @Override
    public String getIntegrationType() {
        return TESTRAILS;
    }

    /**
     * checks the validity of {@link Integration} and {@link Token} by calling list of users, projects,
     * and statuses. Validates successful response.
     *
     * @param tenantId    {@link String} id of the tenant for which the {@code integration} is being validated
     * @param integration {@link Integration} to validate
     * @param token       {@link Token} containing the credentials for the {@code integration}
     * @return {@link PreflightCheckResults} containing {@link PreflightCheckResult}
     */
    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        TestRailsClient client;
        try {
            client = clientFactory.buildFromToken(tenantId, integration, token);
        } catch (InventoryException e) {
            log.error("check: error creating client for testrails: " + tenantId + " , integration: "
                    + integration + e.getMessage(), e);
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
//        builder.check(checkUsers(client)); LEV-5213-: This API has been specific to administrators, for non-administrators – requires projectId in TestRail 6.6 or later.
        builder.check(checkProjects(client, builder));
        return builder.build();
    }

    /**
     * validates the response from {@link TestRailsClient#getUsers()}
     *
     * @param client {@link TestRailsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkUsers(TestRailsClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/users")
                .success(true);
        try {
            List<User> response = client.getUsers();
            if (response == null) {
                checkResultBuilder.success(false).error("got no response from /users");
            }
        } catch (TestRailsClientException e) {
            log.error("checkUser: encountered error while fetching users: " + e.getMessage(), e);
            checkResultBuilder = checkResultFor(e);
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link TestRailsClient#getProjects(Integer, Integer)}
     *
     * @param client {@link TestRailsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkProjects(TestRailsClient client,
                                               PreflightCheckResults.PreflightCheckResultsBuilder builder) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/projects")
                .success(true);
        try {
            List<Project> response = client.getProjects(
                    RESPONSE_PAGE_OFFSET, RESPONSE_PAGE_SIZE);
            if (response == null) {
                checkResultBuilder.success(false).error("got no response from /projects");
            } else if (!response.isEmpty()){
                Integer projectId = response.get(0).getId();
                builder.check(checkMilestones(client, projectId));
                builder.check(checkTestPlans(client, builder, projectId));
                builder.check(checkTestSuites(client, builder, projectId));
                builder.check(checkTestRuns(client, builder, projectId));
            }
        } catch (TestRailsClientException e) {
            log.error("checkProjects: encountered error while fetching projects:  " + e.getMessage(), e);
            checkResultBuilder = checkResultFor(e);
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link TestRailsClient#getTestSuites(Integer)}
     *
     * @param client {@link TestRailsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkTestSuites(TestRailsClient client,
                                                PreflightCheckResults.PreflightCheckResultsBuilder builder,
                                                Integer projectId) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/test-suites")
                .success(true);
        try {
            List<TestRailsTestSuite> response = client.getTestSuites(projectId);
            if (response == null) {
                checkResultBuilder.success(false).error("got no response from /test-suites");
            } else if (!response.isEmpty()) {
                Integer testSuiteId = response.get(0).getId();
                builder.check(checkTestCases(client, builder, projectId, testSuiteId));
            }
        } catch (TestRailsClientException e) {
            log.error("checkTestSuites: encountered error while fetching test plans: " + e.getMessage(), e);
            checkResultBuilder = checkResultFor(e);
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link TestRailsClient#getPaginatedTestCases(Integer, Integer, Integer, Integer)} (Integer)}
     *
     * @param client {@link TestRailsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkTestCases(TestRailsClient client,
                                                PreflightCheckResults.PreflightCheckResultsBuilder builder,
                                                Integer projectId,
                                                Integer suiteId) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/test-cases")
                .success(true);
        try {
            List<TestRailsTestCase> response = client.getPaginatedTestCases(projectId, suiteId, 0, 1);
            if (response == null) {
                checkResultBuilder.success(false).error("got no response from /test-cases");
            }
        } catch (TestRailsClientException e) {
            log.error("checkTestCases: encountered error while fetching test cases: " + e.getMessage(), e);
            checkResultBuilder = checkResultFor(e);
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link TestRailsClient#getMilestones(Integer)}
     *
     * @param client {@link TestRailsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkMilestones(TestRailsClient client, Integer projectId) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/milestones")
                .success(true);
        try {
            List<Milestone> response = client.getPaginatedMilestones(projectId, RESPONSE_PAGE_OFFSET, RESPONSE_PAGE_SIZE);
            if (response == null) {
                checkResultBuilder.success(false).error("got no response from /milestones");
            }
        } catch (TestRailsClientException e) {
            log.error("checkMilestones: encountered error while fetching milestones: " + e.getMessage(), e);
            checkResultBuilder = checkResultFor(e);
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link TestRailsClient#getTestPlans(Integer, Integer, Integer)}
     *
     * @param client {@link TestRailsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkTestPlans(TestRailsClient client,
                                                PreflightCheckResults.PreflightCheckResultsBuilder builder,
                                                Integer projectId) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/plans")
                .success(true);
        try {
            List<TestPlan> response = client.getTestPlans(
                    projectId, RESPONSE_PAGE_SIZE, RESPONSE_PAGE_OFFSET);
            if (response == null) {
                checkResultBuilder.success(false).error("got no response from /plans");
            } else if (!response.isEmpty()) {
                Integer testPlanId = response.get(0).getId();
                builder.check(checkTestPlan(client, testPlanId));
            }
        } catch (TestRailsClientException e) {
            log.error("checkTestPlans: encountered error while fetching test plans: " + e.getMessage(), e);
            checkResultBuilder = checkResultFor(e);
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link TestRailsClient#getPlan(Integer)}
     *
     * @param client {@link TestRailsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkTestPlan(TestRailsClient client, Integer testPlanId) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/plan")
                .success(true);
        try {
            TestPlan response = client.getPlan(testPlanId);
            if (response == null) {
                checkResultBuilder.success(false).error("got no response from /plan");
            }
        } catch (TestRailsClientException e) {
            log.error("checkTestPlan: encountered error while fetching a test plan: " + e.getMessage(), e);
            checkResultBuilder = checkResultFor(e);
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link TestRailsClient#getTestRuns(Integer, int, int)}
     *
     * @param client {@link TestRailsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkTestRuns(TestRailsClient client,
                                               PreflightCheckResults.PreflightCheckResultsBuilder builder,
                                               Integer projectId) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/runs")
                .success(true);
        try {
            List<TestRun> response = client.getTestRuns(projectId,
                    RESPONSE_PAGE_OFFSET, RESPONSE_PAGE_SIZE);
            if (response == null) {
                checkResultBuilder.success(false).error("got no response from /runs");
            } else if (!response.isEmpty()) {
                Integer runId = response.get(0).getId();
                builder.check(checkTests(client, runId));
                builder.check(checkTestResults(client, runId));
            }
        } catch (TestRailsClientException e) {
            log.error("checkTestRuns: encountered error while fetching test runs: " + e.getMessage(), e);
            checkResultBuilder = checkResultFor(e);
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link TestRailsClient#getTests(Integer)}
     *
     * @param client {@link TestRailsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkTests(TestRailsClient client, Integer runId) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/tests")
                .success(true);
        try {
            List<Test> response = client.getPaginatedTests(runId, RESPONSE_PAGE_OFFSET, RESPONSE_PAGE_SIZE);
            if (response == null) {
                checkResultBuilder.success(false).error("got no response from /tests");
            }
        } catch (TestRailsClientException e) {
            log.error("checkTests: encountered error while fetching tests: " + e.getMessage(), e);
            checkResultBuilder = checkResultFor(e);
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link TestRailsClient#getResults(int)}
     *
     * @param client {@link TestRailsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkTestResults(TestRailsClient client, Integer testRunId) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/results")
                .success(true);
        try {
            List<Test.Result> response = client.getPaginatedResults(testRunId, RESPONSE_PAGE_OFFSET, RESPONSE_PAGE_SIZE);
            if (response == null) {
                checkResultBuilder.success(false).error("got no response from /results");
            }
        } catch (TestRailsClientException e) {
            log.error("checkTestResults: encountered error while fetching results: " + e.getMessage(), e);
            checkResultBuilder = checkResultFor(e);
        }
        return checkResultBuilder.build();
    }

    private PreflightCheckResult.PreflightCheckResultBuilder checkResultFor(TestRailsClientException e) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder();
        if (e.getCause() instanceof HttpException) {
            HttpException httpException = (HttpException) e.getCause();
            Integer exceptionCode = httpException.getCode();
            if (HttpStatus.valueOf(exceptionCode).is5xxServerError()) {
                return checkResultBuilder.success(true).exception(e.getMessage())
                        .warning("Ingestion may not go through: " + e.getMessage());
            } else {
                return checkResultBuilder.success(false).exception(e.getMessage());
            }
        } else {
            return checkResultBuilder.success(false).exception(e.getMessage());
        }
    }
}
