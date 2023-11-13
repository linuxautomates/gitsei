package io.levelops.integrations.testrails.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.integrations.testrails.models.CaseField;
import io.levelops.integrations.testrails.models.GetMilestoneResponse;
import io.levelops.integrations.testrails.models.GetProjectResponse;
import io.levelops.integrations.testrails.models.TestRailsGetTestCaseResponse;
import io.levelops.integrations.testrails.models.GetTestPlanResponse;
import io.levelops.integrations.testrails.models.GetTestResponse;
import io.levelops.integrations.testrails.models.GetTestResultResponse;
import io.levelops.integrations.testrails.models.GetTestRunResponse;
import io.levelops.integrations.testrails.models.Milestone;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.Test;
import io.levelops.integrations.testrails.models.TestPlan;
import io.levelops.integrations.testrails.models.TestRailsTestCase;
import io.levelops.integrations.testrails.models.TestRailsTestSuite;
import io.levelops.integrations.testrails.models.TestRun;
import io.levelops.integrations.testrails.models.User;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * TestRails Client class which should be used for making calls to TestRails.
 */
@Log4j2
public class TestRailsClient {

    private static final String REQUEST_API_PATH = "index.php";
    private static final String USERS_PARAM = "/api/v2/get_users";
    private static final String PROJECTS_PARAM = "/api/v2/get_projects";
    private static final String MILESTONES_PARAM = "/api/v2/get_milestones";
    private static final String TEST_PLANS_PARAM = "/api/v2/get_plans";
    private static final String TEST_PLAN_PARAM = "/api/v2/get_plan";
    private static final String TEST_RUNS_PARAM = "/api/v2/get_runs";
    private static final String TESTS_PARAM = "/api/v2/get_tests";
    private static final String TEST_CASES_PARAM = "/api/v2/get_cases";
    private static final String TEST_SUITES_PARAM = "/api/v2/get_suites";
    private static final String TEST_STATUSES_PARAM = "/api/v2/get_statuses";
    private static final String TEST_CASE_TYPES_PARAM = "/api/v2/get_case_types";
    private static final String TEST_PRIORITIES_PARAM = "/api/v2/get_priorities";
    private static final String TEST_RESULTS_PARAM = "/api/v2/get_results_for_run";
    private static final String TEST_CASE_FIELDS = "/api/v2/get_case_fields";

    private static final String CREATED_AFTER = "created_after";
    private static final String IS_COMPLETED = "is_completed";
    private static final String SLASH = "/";

    private static final String PAGE_OFFSET = "offset";
    private static final String PAGE_LIMIT = "limit";

    private final ClientHelper<TestRailsClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String resourceUrl;
    private final int pageSize;

    /**
     * all arg constructor for {@link TestRailsClient} class
     *
     * @param okHttpClient {@link OkHttpClient} object to be used for making http calls
     * @param objectMapper {@link ObjectMapper} for deserializing the responses
     * @param resourceUrl  TestRails base url
     * @param pageSize     response page size
     */
    @Builder
    public TestRailsClient(final OkHttpClient okHttpClient, final ObjectMapper objectMapper,
                           String resourceUrl, Integer pageSize) {
        this.resourceUrl = resourceUrl;
        this.pageSize = pageSize != 0 ? pageSize : TestRailsClientFactory.DEFAULT_PAGE_SIZE;
        this.objectMapper = objectMapper;
        this.clientHelper = ClientHelper.<TestRailsClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(TestRailsClientException.class)
                .build();
    }

    /**
     * Fetches the list of all {@link User}
     *
     * @return {@link List<User>} containing the users
     * @throws TestRailsClientException when the client encounters an exception while making the call
     */
    public List<User> getUsers() throws TestRailsClientException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegment(REQUEST_API_PATH)
                .addEncodedQueryParameter(USERS_PARAM, null)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<List<User>> response = clientHelper.executeAndParseWithHeaders(request,
                objectMapper.getTypeFactory().constructCollectionType(List.class, User.class));
        return response.getBody();
    }

    /**
     * Fetches the list of corresponding {@link User} for the TestRails project with {@code projectId}
     *
     * @param projectId id of the TestRails project
     * @return {@link List<User>} containing the users
     * @throws TestRailsClientException when the client encounters an exception while making the call
     */
    public List<User> getUsersByProjectId(Integer projectId) throws TestRailsClientException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegments(REQUEST_API_PATH)
                .addEncodedQueryParameter(USERS_PARAM + SLASH + projectId, null)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<List<User>> response = clientHelper.executeAndParseWithHeaders(request,
                objectMapper.getTypeFactory().constructCollectionType(List.class, User.class));
        return response.getBody();
    }

    /**
     * Fetches the list of all {@link Project} for the TestRails
     *
     * @return {@link List<Project>} containing the projects
     * @throws TestRailsClientException when the client encounters an exception while making the call
     */
    public Stream<Project> getProjects() throws TestRailsClientException {
        try {
            return PaginationUtils.stream(0, pageSize, offset -> {
                try {
                    return getProjects(offset, pageSize);
                } catch (TestRailsClientException e) {
                    log.warn("Failed to get projects after page {}", offset, e);
                    throw new RuntimeStreamException("Failed to get projects after page=" + offset, e);
                }
            });
        } catch (RuntimeStreamException e) {
            throw new TestRailsClientException("Failed to list all projects ", e);
        }
    }

    public List<Project> getProjects(Integer offset, Integer pageSize) throws TestRailsClientException {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegments(REQUEST_API_PATH)
                .addEncodedQueryParameter(PROJECTS_PARAM, null)
                .addQueryParameter(PAGE_LIMIT, String.valueOf(pageSize))
                .addQueryParameter(PAGE_OFFSET, String.valueOf(offset));
        HttpUrl url = urlBuilder.build();
        Request request = buildRequest(url);
        GetProjectResponse response = clientHelper.executeAndParse(request, GetProjectResponse.class);
        return response.getProjects();
    }

    /**
     * Fetches the list of corresponding {@link Milestone} for the TestRails milestones with {@code projectId}
     *
     * @param projectId id of the TestRails project
     * @return {@link List<Milestone>} containing the milestones
     * @throws TestRailsClientException when the client encounters an exception while making the call
     */
    public Stream<Milestone> getMilestones(Integer projectId) throws TestRailsClientException {
        try {
            return PaginationUtils.stream(0, pageSize, offset -> {
                try {
                    return getPaginatedMilestones(projectId, offset, pageSize);
                } catch (TestRailsClientException e) {
                    log.warn("Failed to get milestones after page {}", offset, e);
                    throw new RuntimeStreamException("Failed to get milestones after page=" + offset, e);
                }
            });
        } catch (RuntimeStreamException e) {
            throw new TestRailsClientException("Failed to list all milestones ", e);
        }
    }

    public List<Milestone> getPaginatedMilestones(Integer projectId, int offset, int pageSize) throws TestRailsClientException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegments(REQUEST_API_PATH)
                .addEncodedQueryParameter(MILESTONES_PARAM + SLASH + projectId, null)
                .addQueryParameter(PAGE_OFFSET, String.valueOf(offset))
                .addQueryParameter(PAGE_LIMIT, String.valueOf(pageSize))
                .build();
        Request request = buildRequest(url);
        GetMilestoneResponse response = clientHelper.executeAndParse(request, GetMilestoneResponse.class);
        return response.getMilestones();
    }

    /**
     * Fetches the list of corresponding {@link TestPlan} for the TestRails test plans with {@code projectId}
     *
     * @param projectId id of the TestRails project
     * @param pageLimit for pagination purpose
     * @param offset    for pagination purpose
     * @return {@link List<TestPlan>} containing the test plans
     * @throws TestRailsClientException when the client encounters an exception while making the call
     */
    public List<TestPlan> getTestPlans(Integer projectId,
                                       Integer pageLimit, Integer offset) throws TestRailsClientException {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegments(REQUEST_API_PATH)
                .addEncodedQueryParameter(TEST_PLANS_PARAM + SLASH + projectId, null)
                .addQueryParameter(PAGE_OFFSET, String.valueOf(offset))
                .addQueryParameter(PAGE_LIMIT, String.valueOf(pageLimit));
        HttpUrl url = urlBuilder.build();
        Request request = buildRequest(url);
        GetTestPlanResponse response = clientHelper.executeAndParse(request, GetTestPlanResponse.class);
        return response.getPlans();
    }

    /**
     * Fetches a corresponding {@link TestPlan} for the TestRails test plan with entries
     *
     * @param planId id of the TestRails test plan
     * @return {@link TestPlan} with entries containing the test plan
     * @throws TestRailsClientException when the client encounters an exception while making the call
     */
    public TestPlan getPlan(Integer planId) throws TestRailsClientException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegments(REQUEST_API_PATH)
                .addEncodedQueryParameter(TEST_PLAN_PARAM + SLASH + planId, null)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<TestPlan> response = clientHelper.executeAndParseWithHeaders(request,
                TestPlan.class);
        return response.getBody();
    }

    /**
     * Fetches the list of corresponding {@link TestRun} for the TestRails test runs with {@code projectId}
     *
     * @param projectId id of the TestRails project
     * @param offset    for pagination purpose
     * @param pageSize  for pagination purpose
     * @return {@link List<TestRun>} containing the test runs
     * @throws TestRailsClientException when the client encounters an exception while making the call
     */
    public List<TestRun> getTestRuns(Integer projectId, int offset, int pageSize) throws TestRailsClientException {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegments(REQUEST_API_PATH)
                .addEncodedQueryParameter(TEST_RUNS_PARAM + SLASH + projectId, null)
                .addQueryParameter(PAGE_OFFSET, String.valueOf(offset))
                .addQueryParameter(PAGE_LIMIT, String.valueOf(pageSize));
        HttpUrl url = urlBuilder.build();
        Request request = buildRequest(url);
        GetTestRunResponse response = clientHelper.executeAndParse(request, GetTestRunResponse.class);
        return response.getRuns();
    }

    /**
     * Fetches the list of corresponding {@link Test} for the TestRails test run with {@code testRunId}
     *
     * @param testRunId id of the TestRails test run
     * @return {@link List<Test>} containing the tests
     * @throws TestRailsClientException when the client encounters an exception while making the call
     */
    public Stream<Test> getTests(Integer testRunId) throws TestRailsClientException {
        try {
            return PaginationUtils.stream(0, pageSize, offset -> {
                try {
                    return getPaginatedTests(testRunId, offset, pageSize);
                } catch (TestRailsClientException e) {
                    log.warn("Failed to get tests after page {}", offset, e);
                    throw new RuntimeStreamException("Failed to get tests after page=" + offset, e);
                }
            });
        } catch (RuntimeStreamException e) {
            throw new TestRailsClientException("Failed to list all tests ", e);
        }
    }

    public List<Test> getPaginatedTests(Integer testRunId, int offset, int pageSize) throws TestRailsClientException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegments(REQUEST_API_PATH)
                .addEncodedQueryParameter(TESTS_PARAM + SLASH + testRunId, null)
                .addQueryParameter(PAGE_OFFSET, String.valueOf(offset))
                .addQueryParameter(PAGE_LIMIT, String.valueOf(pageSize))
                .build();
        Request request = buildRequest(url);
        GetTestResponse response = clientHelper.executeAndParse(request, GetTestResponse.class);
        return response.getTests();
    }

    public List<TestRailsTestSuite> getTestSuites(Integer projectId) throws TestRailsClientException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegment(REQUEST_API_PATH)
                .addEncodedQueryParameter(TEST_SUITES_PARAM + SLASH + projectId, null)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<List<TestRailsTestSuite>> response = clientHelper.executeAndParseWithHeaders(request,
                objectMapper.getTypeFactory().constructCollectionType(List.class, TestRailsTestSuite.class));
        return response.getBody();
    }

    public Stream<TestRailsTestCase> getTestCases(Integer projectId, Integer suiteId) throws TestRailsClientException {
        return PaginationUtils.stream(0, pageSize, offset -> {
            try {
                return getPaginatedTestCases(projectId, suiteId, offset, pageSize);
            } catch (TestRailsClientException e) {
                throw new RuntimeStreamException("Failed to get all testcases of project id: " + projectId + " after page=" + offset, e);
            }
        });
    }

    public List<TestRailsTestCase> getPaginatedTestCases(Integer projectId, Integer suiteId, Integer offset, Integer pageSize) throws TestRailsClientException {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegment(REQUEST_API_PATH)
                .addEncodedQueryParameter(TEST_CASES_PARAM + SLASH + projectId, null)
                .addQueryParameter("suite_id", String.valueOf(suiteId))
                .addQueryParameter(PAGE_OFFSET, String.valueOf(offset))
                .addQueryParameter(PAGE_LIMIT, String.valueOf(pageSize));
        Request request = buildRequest(urlBuilder.build());
        TestRailsGetTestCaseResponse response = clientHelper.executeAndParse(request, TestRailsGetTestCaseResponse.class);
        return response.getTestCases();
    }

    public Stream<Test.Result> getResults(int testId) throws TestRailsClientException {
        return PaginationUtils.stream(0, pageSize, offset -> {
            try {
                return getPaginatedResults(testId, offset, pageSize);
            } catch (TestRailsClientException e) {
                throw new RuntimeStreamException("Failed to get results of test after page=" + offset, e);
            }
        });
    }
    public List<Test.Result> getPaginatedResults(int testRunId, int offset, int pageSize) throws TestRailsClientException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegment(REQUEST_API_PATH)
                .addEncodedQueryParameter(TEST_RESULTS_PARAM + SLASH + testRunId, null)
                .addQueryParameter(PAGE_OFFSET, String.valueOf(offset))
                .addQueryParameter(PAGE_LIMIT, String.valueOf(pageSize))
                .build();
        Request request = buildRequest(url);
        GetTestResultResponse response = clientHelper.executeAndParse(request, GetTestResultResponse.class);
        return response.getResults();
    }

    /**
     * Fetches the list of corresponding {@link Test.Status}
     *
     * @return {@link List<Test.Status>} containing all statuses
     * @throws TestRailsClientException when the client encounters an exception while making the call
     */
    public List<Test.Status> getStatuses() throws TestRailsClientException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegments(REQUEST_API_PATH)
                .addEncodedQueryParameter(TEST_STATUSES_PARAM, null)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<List<Test.Status>> response = clientHelper.executeAndParseWithHeaders(request,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Test.Status.class));
        return response.getBody();
    }

    /**
     * Fetches the list of corresponding {@link Test.CaseType}
     *
     * @return {@link List<Test.CaseType>} containing all case types
     * @throws TestRailsClientException when the client encounters an exception while making the call
     */
    public List<Test.CaseType> getCaseTypes() throws TestRailsClientException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegment(REQUEST_API_PATH)
                .addEncodedQueryParameter(TEST_CASE_TYPES_PARAM, null)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<List<Test.CaseType>> response = clientHelper.executeAndParseWithHeaders(request,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Test.CaseType.class));
        return response.getBody();
    }

    /**
     * Fetches the list of corresponding {@link Test.Priority}
     *
     * @return {@link List<Test.Priority>} containing all the priorities
     * @throws TestRailsClientException when the client encounters an exception while making the call
     */
    public List<Test.Priority> getPriorities() throws TestRailsClientException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegment(REQUEST_API_PATH)
                .addEncodedQueryParameter(TEST_PRIORITIES_PARAM, null)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<List<Test.Priority>> response = clientHelper.executeAndParseWithHeaders(request,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Test.Priority.class));
        return response.getBody();
    }

    public List<CaseField> getCaseFields() throws TestRailsClientException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegment(REQUEST_API_PATH)
                .addEncodedQueryParameter(TEST_CASE_FIELDS, null)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<List<CaseField>> response = clientHelper.executeAndParseWithHeaders(request,
                objectMapper.getTypeFactory().constructCollectionType(List.class, CaseField.class));
        return response.getBody();
    }

    @NotNull
    private Request buildRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.CONTENT_TYPE, ClientConstants.APPLICATION_JSON.toString())
                .header("x-api-ident", "beta")
                .get()
                .build();
    }
}
