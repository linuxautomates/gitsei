package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbTestRailsConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsCaseField;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTest;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTestPlan;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTestResult;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTestRun;
import io.levelops.commons.databases.models.filters.TestRailsCaseFieldFilter;
import io.levelops.commons.databases.models.filters.TestRailsTestsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ListUtils;
import io.levelops.integrations.testrails.models.CaseField;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class TestRailsTestPlanDatabaseService extends DatabaseService<DbTestRailsTestPlan> {

    private static final String TESTRAILS_PLANS = "testrails_plans";
    private static final String TESTRAILS_TEST_RUNS = "testrails_test_runs";
    private static final String TESTRAILS_TESTS = "testrails_tests";
    private static final String TESTRAILS_TEST_CASES = "testrails_test_cases";
    private static final String TESTRAILS_TEST_RESULTS = "testrails_test_results";
    private static final String TESTRAILS_PROJECTS = "testrails_projects";
    private static final String TESTRAILS_MILESTONES = "testrails_milestones";
    private static final String TESTRAILS_FINAL_TABLE = "final_table";
    public static final String TESTRAILS_TEST_RUN_TEMP_TABLE = "testrails_test_run_temp_table_";
    public static final String TESTRAILS_TEST_TEMP_TABLE = "testrails_test_temp_table_";
    public static final String TESTRAILS_TEST_RESULT_TEMP_TABLE = "testrails_test_result_temp_table_";
    private static final Set<String> SORTABLE_COLUMNS = Set.of("estimate", "estimate_forecast", "created_at",
            "updated_at", "created_on");
    private static final Integer BATCH_SIZE = 100;

    private static final Set<TestRailsTestsFilter.DISTINCT> stackSupported = Set.of(
            TestRailsTestsFilter.DISTINCT.project, // stacks supported
            TestRailsTestsFilter.DISTINCT.milestone, // stacks supported
            TestRailsTestsFilter.DISTINCT.test_plan, // stacks supported
            TestRailsTestsFilter.DISTINCT.test_run, // stacks supported
            TestRailsTestsFilter.DISTINCT.type,
            TestRailsTestsFilter.DISTINCT.priority, // stacks supported
            TestRailsTestsFilter.DISTINCT.status, // stacks supported
            TestRailsTestsFilter.DISTINCT.assignee // stacks supported
    );

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper mapper;
    private final TestRailsCustomCaseFieldConditionsBuilder customCaseFieldConditionsBuilder;
    private final TestRailsCaseFieldDatabaseService caseFieldDatabaseService;

    @Autowired
    public TestRailsTestPlanDatabaseService(final ObjectMapper mapper, DataSource dataSource,
                                               TestRailsCustomCaseFieldConditionsBuilder customCaseFieldConditionsBuilder,
                                               TestRailsCaseFieldDatabaseService caseFieldDatabaseService) {
        super(dataSource);
        this.mapper = mapper;
        template = new NamedParameterJdbcTemplate(dataSource);
        this.customCaseFieldConditionsBuilder = customCaseFieldConditionsBuilder;
        this.caseFieldDatabaseService = caseFieldDatabaseService;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbTestRailsTestPlan testPlan) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            final int integrationId = NumberUtils.toInt(testPlan.getIntegrationId());
            String insertPlan = "INSERT INTO " + company + "." + TESTRAILS_PLANS + " (integration_id, plan_id, " +
                    "milestone_id, project_id, name, description, assignee, url, created_by, completed_on, created_on," +
                    " is_completed) VALUES(?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (plan_id, integration_id) DO UPDATE SET " +
                    "milestone_id=EXCLUDED.milestone_id, name=EXCLUDED.name, description=EXCLUDED.description, " +
                    "assignee=EXCLUDED.assignee, url=EXCLUDED.url, created_by=EXCLUDED.created_by, " +
                    "completed_on=EXCLUDED.completed_on, created_on=EXCLUDED.created_on," +
                    " is_completed=EXCLUDED.is_completed RETURNING id";
            String insertTestRun = "INSERT INTO " + company + "." + TESTRAILS_TEST_RUNS + " (integration_id, run_id, " +
                    "project_id, plan_id, milestone_id, name, description, url, assignee, created_by, " +
                    "completed_on, created_at, updated_at, test_created_at, test_updated_at) VALUES" +
                    "(?,?,?,?,?,?,?,?,?,?,?,'now','now',?,?)" +
                    " ON CONFLICT (run_id, integration_id) DO UPDATE SET " +
                    "project_id=EXCLUDED.project_id, plan_id=EXCLUDED.plan_id, milestone_id=EXCLUDED.milestone_id, " +
                    "name=EXCLUDED.name, description=EXCLUDED.description, " +
                    "url=EXCLUDED.url, assignee=EXCLUDED.assignee, created_by=EXCLUDED.created_by, " +
                    "completed_on=EXCLUDED.completed_on, created_at=" + company + "." + TESTRAILS_TEST_RUNS +
                    ".created_at, updated_at=EXCLUDED.updated_at, test_created_at=EXCLUDED.test_created_at, " +
                    "test_updated_at=EXCLUDED.test_updated_at WHERE " + company + "." + TESTRAILS_TEST_RUNS +
                    ".test_updated_at < EXCLUDED.test_updated_at RETURNING id";
            String insertTest = "INSERT INTO " + company + "." + TESTRAILS_TESTS + " (integration_id, test_id, run_id, " +
                    "case_id, milestone_id, title, status, priority, type, refs, assignee, estimate, estimate_forecast, custom_case_fields, created_on, " +
                    "created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?,'now','now')" +
                    " ON CONFLICT (test_id, integration_id) DO UPDATE SET " +
                    "run_id=EXCLUDED.run_id, case_id=EXCLUDED.case_id, " +
                    "milestone_id=EXCLUDED.milestone_id, title=EXCLUDED.title, status=EXCLUDED.status, " +
                    "priority=EXCLUDED.priority, assignee=EXCLUDED.assignee, estimate=EXCLUDED.estimate, " +
                    "estimate_forecast=EXCLUDED.estimate_forecast, custom_case_fields=EXCLUDED.custom_case_fields, created_on=EXCLUDED.created_on, " +
                    "created_at=" + company + "." + TESTRAILS_TESTS +
                    ".created_at, updated_at=EXCLUDED.updated_at RETURNING id";
            String insertResult = "INSERT INTO " + company + "." + TESTRAILS_TEST_RESULTS +
                    " (integration_id, result_id, test_id, status, defects, assignee, creator, created_on, comment, elapsed, created_at, updated_at)" +
                    " VALUES(?,?,?,?,?,?,?,?,?,?,'now','now')" +
                    " ON CONFLICT (result_id, integration_id) DO UPDATE SET " +
                    "test_id=EXCLUDED.test_id, status=EXCLUDED.status, creator=EXCLUDED.creator, created_on=EXCLUDED.created_on, assignee=EXCLUDED.assignee, comment=EXCLUDED.comment, " +
                    "elapsed=EXCLUDED.elapsed, created_at=" + company + "." + TESTRAILS_TEST_RESULTS +
                    ".created_at, updated_at=EXCLUDED.updated_at";
            try (PreparedStatement planStmt = conn.prepareStatement(insertPlan, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement testRunStmt = conn.prepareStatement(insertTestRun, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement testStmt = conn.prepareStatement(insertTest, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement testResultStmt = conn.prepareStatement(insertResult, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                planStmt.setObject(i++, integrationId);
                planStmt.setObject(i++, testPlan.getPlanId());
                planStmt.setObject(i++, testPlan.getMilestoneId());
                planStmt.setObject(i++, testPlan.getProjectId());
                planStmt.setObject(i++, testPlan.getName());
                planStmt.setObject(i++, testPlan.getDescription());
                planStmt.setObject(i++, testPlan.getAssignee());
                planStmt.setObject(i++, testPlan.getUrl());
                planStmt.setObject(i++, testPlan.getCreator());
                planStmt.setObject(i++, getTimestamp(testPlan.getCompletedOn()));
                planStmt.setObject(i++, testPlan.getCreatedOn() != null ? Timestamp.from(testPlan.getCreatedOn()) : null);
                planStmt.setObject(i, testPlan.getIsCompleted());
                planStmt.executeUpdate();
                String planId = getTestPlanIdOrFetch(planStmt, company, testPlan.getPlanId(), testPlan.getProjectId(),
                        integrationId);
                final UUID testPlanUuid = UUID.fromString(planId);

                if (CollectionUtils.isNotEmpty(testPlan.getTestRuns())) {
                    for (DbTestRailsTestRun testRun : testPlan.getTestRuns()) {
                        int index = 1;
                        testRunStmt.setObject(index++, integrationId);
                        testRunStmt.setObject(index++, testRun.getRunId());
                        testRunStmt.setObject(index++, testRun.getProjectId());
                        testRunStmt.setObject(index++, testPlanUuid);
                        testRunStmt.setObject(index++, testRun.getMilestoneId());
                        testRunStmt.setObject(index++, testRun.getName());
                        testRunStmt.setObject(index++, testRun.getDescription());
                        testRunStmt.setObject(index++, testRun.getUrl());
                        testRunStmt.setObject(index++, testRun.getAssignee());
                        testRunStmt.setObject(index++, testRun.getCreator());
                        testRunStmt.setObject(index++, getTimestamp(testRun.getCompletedOn()));
                        testRunStmt.setObject(index++, testRun.getCreatedOn() != null ? Timestamp.from(testRun.getCreatedOn()) : null);
                        testRunStmt.setObject(index, testRun.getUpdatedOn() != null ? Timestamp.from(testRun.getUpdatedOn()) : null);
                        testRunStmt.executeUpdate();
                        final UUID testRunUuid = UUID.fromString(getTestRunIdOrFetch(testRunStmt, company,
                                testRun.getRunId(), integrationId));

                        if (CollectionUtils.isNotEmpty(testRun.getTests())) {
                            int count = 0;
                            List<List<DbTestRailsTestResult>> listOfTestResults = new ArrayList<>();
                            for (DbTestRailsTest test : testRun.getTests()) {
                                int j = 1;
                                testStmt.setObject(j++, integrationId);
                                testStmt.setObject(j++, test.getTestId());
                                testStmt.setObject(j++, testRunUuid);
                                testStmt.setObject(j++, test.getCaseId());
                                testStmt.setObject(j++, test.getMilestoneId());
                                testStmt.setObject(j++, test.getTitle());
                                testStmt.setObject(j++, test.getStatus());
                                testStmt.setObject(j++, test.getPriority());
                                testStmt.setObject(j++, test.getType());
                                testStmt.setObject(j++, test.getRefs());
                                testStmt.setObject(j++, test.getAssignee());
                                testStmt.setObject(j++, test.getEstimate());
                                testStmt.setObject(j++, test.getEstimateForecast());
                                testStmt.setObject(j++, ParsingUtils.serialize(mapper, "custom case fields", test.getCustomCaseFields(), "{}"));
                                testStmt.setObject(j, test.getCreated_on() != null ? Timestamp.from(test.getCreated_on()) : null);
                                listOfTestResults.add(ListUtils.emptyIfNull(test.getResults()));
                                count++;
                                testStmt.addBatch();
                                if (count % 100 == 0) {
                                    testStmt.executeBatch();
                                    insertResults(integrationId, listOfTestResults, testStmt, testResultStmt, conn);
                                    listOfTestResults = new ArrayList<>();
                                }
                            }
                            if (count % 100 != 0) {
                                testStmt.executeBatch();
                                insertResults(integrationId, listOfTestResults, testStmt, testResultStmt, conn);
                            }
                        }
                    }
                }
                return planId;
            }
        }));
    }

    public String insertTestRun(String company, DbTestRailsTestRun testRun) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            final int integrationId = NumberUtils.toInt(testRun.getIntegrationId());
            String insertTestRun = "INSERT INTO " + company + "." + TESTRAILS_TEST_RUNS + " (integration_id, run_id, " +
                    "project_id, plan_id, milestone_id, name, description, url, assignee, created_by, " +
                    "completed_on, created_at, updated_at, test_created_at, test_updated_at) VALUES" +
                    "(?,?,?,?,?,?,?,?,?,?,?,'now','now',?,?)" +
                    " ON CONFLICT (run_id, integration_id) DO UPDATE SET " +
                    "project_id=EXCLUDED.project_id, plan_id=EXCLUDED.plan_id, milestone_id=EXCLUDED.milestone_id, " +
                    "name=EXCLUDED.name, description=EXCLUDED.description, " +
                    "url=EXCLUDED.url, assignee=EXCLUDED.assignee, created_by=EXCLUDED.created_by, " +
                    "completed_on=EXCLUDED.completed_on, created_at=" + company + "." + TESTRAILS_TEST_RUNS +
                    ".created_at, updated_at=EXCLUDED.updated_at, test_created_at=EXCLUDED.test_created_at, " +
                    "test_updated_at=EXCLUDED.test_updated_at WHERE " + company + "." + TESTRAILS_TEST_RUNS +
                    ".test_updated_at < EXCLUDED.test_updated_at RETURNING id";
            String insertTest = "INSERT INTO " + company + "." + TESTRAILS_TESTS + " (integration_id, test_id, run_id, " +
                    "case_id, milestone_id, title, status, priority, type, refs, assignee, estimate, estimate_forecast, custom_case_fields, created_on, " +
                    "created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?,'now','now')" +
                    " ON CONFLICT (test_id, integration_id) DO UPDATE SET " +
                    "run_id=EXCLUDED.run_id, case_id=EXCLUDED.case_id, " +
                    "milestone_id=EXCLUDED.milestone_id, title=EXCLUDED.title, status=EXCLUDED.status, " +
                    "priority=EXCLUDED.priority, assignee=EXCLUDED.assignee, estimate=EXCLUDED.estimate, " +
                    "estimate_forecast=EXCLUDED.estimate_forecast, custom_case_fields=EXCLUDED.custom_case_fields, created_on=EXCLUDED.created_on, " +
                    "created_at=" + company + "." + TESTRAILS_TESTS +
                    ".created_at, updated_at=EXCLUDED.updated_at RETURNING id";
            String insertResult = "INSERT INTO " + company + "." + TESTRAILS_TEST_RESULTS +
                    " (integration_id, result_id, test_id, status, defects, assignee, creator, created_on, comment, elapsed, created_at, updated_at)" +
                    " VALUES(?,?,?,?,?,?,?,?,?,?,'now','now')" +
                    " ON CONFLICT (result_id, integration_id) DO UPDATE SET " +
                    "test_id=EXCLUDED.test_id, status=EXCLUDED.status, creator=EXCLUDED.creator, created_on=EXCLUDED.created_on, assignee=EXCLUDED.assignee, comment=EXCLUDED.comment, " +
                    "elapsed=EXCLUDED.elapsed, created_at=" + company + "." + TESTRAILS_TEST_RESULTS +
                    ".created_at, updated_at=EXCLUDED.updated_at";
            try (PreparedStatement testRunStmt = conn.prepareStatement(insertTestRun, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement testStmt = conn.prepareStatement(insertTest, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement testResultStmt = conn.prepareStatement(insertResult, Statement.RETURN_GENERATED_KEYS)) {
                int index = 1;
                testRunStmt.setObject(index++, integrationId);
                testRunStmt.setObject(index++, testRun.getRunId());
                testRunStmt.setObject(index++, testRun.getProjectId());
                testRunStmt.setObject(index++, null);
                testRunStmt.setObject(index++, testRun.getMilestoneId());
                testRunStmt.setObject(index++, testRun.getName());
                testRunStmt.setObject(index++, testRun.getDescription());
                testRunStmt.setObject(index++, testRun.getUrl());
                testRunStmt.setObject(index++, testRun.getAssignee());
                testRunStmt.setObject(index++, testRun.getCreator());
                testRunStmt.setObject(index++, getTimestamp(testRun.getCompletedOn()));
                testRunStmt.setObject(index++, testRun.getCreatedOn() != null ? Timestamp.from(testRun.getCreatedOn()) : null);
                testRunStmt.setObject(index, testRun.getUpdatedOn() != null ? Timestamp.from(testRun.getUpdatedOn()) : null);
                testRunStmt.executeUpdate();
                String testRunId = getTestRunIdOrFetch(testRunStmt, company, testRun.getRunId(), integrationId);
                final UUID testRunUuid = UUID.fromString(testRunId);

                if (CollectionUtils.isNotEmpty(testRun.getTests())) {
                    int count = 0;
                    List<List<DbTestRailsTestResult>> listOfTestResults = new ArrayList<>();
                    for (DbTestRailsTest test : testRun.getTests()) {
                        int j = 1;
                        testStmt.setObject(j++, integrationId);
                        testStmt.setObject(j++, test.getTestId());
                        testStmt.setObject(j++, testRunUuid);
                        testStmt.setObject(j++, test.getCaseId());
                        testStmt.setObject(j++, test.getMilestoneId());
                        testStmt.setObject(j++, test.getTitle());
                        testStmt.setObject(j++, test.getStatus());
                        testStmt.setObject(j++, test.getPriority());
                        testStmt.setObject(j++, test.getType());
                        testStmt.setObject(j++, test.getRefs());
                        testStmt.setObject(j++, test.getAssignee());
                        testStmt.setObject(j++, test.getEstimate());
                        testStmt.setObject(j++, test.getEstimateForecast());
                        testStmt.setObject(j++, ParsingUtils.serialize(mapper, "custom case fields", test.getCustomCaseFields(), "{}"));
                        testStmt.setObject(j, test.getCreated_on() != null ? Timestamp.from(test.getCreated_on()) : null);
                        listOfTestResults.add(ListUtils.emptyIfNull(test.getResults()));
                        count++;
                        testStmt.addBatch();
                        if (count % 100 == 0) {
                            testStmt.executeBatch();
                            insertResults(integrationId, listOfTestResults, testStmt, testResultStmt, conn);
                            listOfTestResults = new ArrayList<>();
                        }
                    }
                    if (count % 100 != 0) {
                        testStmt.executeBatch();
                        insertResults(integrationId, listOfTestResults, testStmt, testResultStmt, conn);
                    }
                }
                return testRunId;
            }
        }));
    }

    private void insertResults(int integrationId, List<List<DbTestRailsTestResult>> listOfTestResults, PreparedStatement testStmt, PreparedStatement testResultStmt, Connection conn) throws SQLException {
        ResultSet testKeys = testStmt.getGeneratedKeys();
        List<String> testUuids = new ArrayList<>();
        while (testKeys.next()) {
            String testUuid = testKeys.getString(1);
            testUuids.add(testUuid);
        }
        long resultCount = 0;
        int index = 0;
        for (var testResults : listOfTestResults) {
            String testUuid = testUuids.get(index);
            for (DbTestRailsTestResult testResult : testResults) {
                int k = 1;
                testResultStmt.setObject(k++, integrationId);
                testResultStmt.setObject(k++, testResult.getResultId());
                testResultStmt.setObject(k++, UUID.fromString(testUuid));
                testResultStmt.setObject(k++, testResult.getStatus());
                testResultStmt.setObject(k++, conn.createArrayOf("varchar", ListUtils.emptyIfNull(testResult.getDefects()).toArray()));
                testResultStmt.setObject(k++, testResult.getAssignee());
                testResultStmt.setObject(k++, testResult.getCreator());
                testResultStmt.setObject(k++, testResult.getCreatedOn() != null ? Timestamp.from(testResult.getCreatedOn()) : null);
                testResultStmt.setObject(k++, testResult.getComment());
                testResultStmt.setObject(k, testResult.getElapsed());
                testResultStmt.addBatch();
                resultCount++;
            }
            if (resultCount % 100 == 0) {
                testResultStmt.executeBatch();
            }
            index++;
        }
        if (resultCount % 100 != 0) {
            testResultStmt.executeBatch();
        }
    }

    private String getTestPlanIdOrFetch(PreparedStatement insertStmt, String company, int planId, int projectId,
                                        int integrationId) throws SQLException {
        String id;
        try (ResultSet rs = insertStmt.getGeneratedKeys()) {
            if (rs.next())
                id = rs.getString(1);
            else {
                final Optional<String> idOpt = getTestPlanId(company, planId, projectId, integrationId);
                if (idOpt.isPresent()) {
                    id = idOpt.get();
                } else {
                    throw new SQLException("Failed to get test plan row id");
                }
            }
        }
        return id;
    }

    private Optional<String> getTestPlanId(String company, int planId, int projectId, int integrationId) {
        String query = "SELECT id FROM " + company + "." + TESTRAILS_PLANS + " where plan_id = :plan_id AND" +
                " integration_id = :integration_id AND project_id = :project_id";
        final Map<String, Object> params = Map.of(
                "plan_id", planId,
                "integration_id", integrationId,
                "project_id", projectId);
        return Optional.ofNullable(template.query(query, params,
                rs -> rs.next() ? rs.getString("id") : null));
    }

    private String getTestRunIdOrFetch(PreparedStatement insertStmt, String company, int runId, int integrationId) throws SQLException {
        String id;
        try (ResultSet rs = insertStmt.getGeneratedKeys()) {
            if (rs.next())
                id = rs.getString(1);
            else {
                final Optional<String> idOpt = getTestRunId(company, runId, integrationId);
                if (idOpt.isPresent()) {
                    id = idOpt.get();
                } else {
                    throw new SQLException("Failed to get test run row id");
                }
            }
        }
        return id;
    }

    private Optional<String> getTestRunId(String company, int runId, int integrationId) {
        String query = "SELECT id FROM " + company + "." + TESTRAILS_TEST_RUNS + " where run_id = :run_id AND" +
                " integration_id = :integration_id";
        final Map<String, Object> params = Map.of(
                "run_id", runId,
                "integration_id", integrationId);
        return Optional.ofNullable(template.query(query, params,
                rs -> rs.next() ? rs.getString("id") : null));
    }

    public DbListResponse<DbTestRailsTest> list(String company,
                                                TestRailsTestsFilter filter,
                                                Map<String, SortingOrder> sortBy,
                                                Integer pageNumber,
                                                Integer pageSize) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        filter = filter.toBuilder().createdOnTimeRange(ImmutablePair.of(null, filter.getCreatedOnTimeRange() != null ? filter.getCreatedOnTimeRange().getRight() : null)).build();
        boolean isTestCase = TestRailsTestsFilter.CALCULATION.test_case_count.equals(filter.getCALCULATION());
        final Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(company, params, filter.getProjects(),
                filter.getTestRuns(), filter.getMilestones(), filter.getTestPlans(), filter.getAssignees(),
                filter.getIntegrationIds(), filter.getStatuses(), filter.getPriorities(), filter.getTestTypes(), filter.getCustomCaseFields(), filter.getExcludeCustomCaseFields(), filter.getCreatedOnTimeRange(), filter.getCreatedAtTimeRange());
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "created_on";
                })
                .orElse("created_on");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        String testsWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(TESTRAILS_TESTS))) {
            testsWhere = " WHERE " + String.join(" AND ", conditions.get(TESTRAILS_TESTS));
        }
        String testRunsWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(TESTRAILS_TEST_RUNS))) {
            testRunsWhere = " WHERE " + String.join(" AND ", conditions.get(TESTRAILS_TEST_RUNS));
        }
        String testPlansWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(TESTRAILS_PLANS))) {
            testPlansWhere = " WHERE " + String.join(" AND ", conditions.get(TESTRAILS_PLANS));
        }
        String testMilestonesWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(TESTRAILS_MILESTONES))) {
            testMilestonesWhere = " WHERE " + String.join(" AND ", conditions.get(TESTRAILS_MILESTONES));
        }
        String testProjectsWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(TESTRAILS_PROJECTS))) {
            testProjectsWhere = " WHERE " + String.join(" AND ", conditions.get(TESTRAILS_PROJECTS));
        }
        String finalTableWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(TESTRAILS_FINAL_TABLE))) {
            finalTableWhere = " WHERE " + String.join(" AND ", conditions.get(TESTRAILS_FINAL_TABLE));
        }
        String testResultWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(TESTRAILS_TEST_RESULTS))) {
            testResultWhere = " WHERE " + String.join(" AND ", conditions.get(TESTRAILS_TEST_RESULTS));
        }
        String testCaseTableWhere = "";
        if(CollectionUtils.isNotEmpty(conditions.get(TESTRAILS_TEST_CASES))) {
            testCaseTableWhere = " WHERE " + String.join(" AND ", conditions.get(TESTRAILS_TEST_CASES));
        }
        String selectResultForDefects = " LEFT JOIN ( select array_agg(defects) as defects, test_id from ( select distinct( unnest(result.defects) ) as defects, result.test_id from " + company + "." + TESTRAILS_TEST_RESULTS + " as result " + testResultWhere + " group by result.test_id, result.defects ) a group by test_id ) results ON results.test_id = test.id ";
        String selectTests = "SELECT * FROM " + company + "." + TESTRAILS_TESTS + " as test " + selectResultForDefects + testsWhere;
        String selectTestCases = "SELECT * FROM " + company + "." + TESTRAILS_TEST_CASES + testCaseTableWhere;
        String selectTestRuns = "SELECT * FROM " + company + "." + TESTRAILS_TEST_RUNS + testRunsWhere;
        String selectTestPlans = "SELECT * FROM " + company + "." + TESTRAILS_PLANS + testPlansWhere;
        String selectMilestones = "SELECT * FROM " + company + "." + TESTRAILS_MILESTONES + testMilestonesWhere;
        String selectProjects = "SELECT * FROM " + company + "." + TESTRAILS_PROJECTS + testProjectsWhere;
        String fromTable;
        if (isTestCase){
            fromTable = "(SELECT t.*, COALESCE(m.name, 'UNASSIGNED') milestone, p.name project FROM " +
                    "( " + selectTestCases + " ) t LEFT JOIN "
                    + "(" + selectMilestones + ") m" + " ON t.milestone_id=m.milestone_id AND" +
                    " t.integration_id=m.integration_id  INNER JOIN (" + selectProjects + ")" + " p " +
                    "ON t.integration_id=p.integration_id AND t.project_id=p.project_id ) final_table " + finalTableWhere;
        }
        else{
            fromTable = "(SELECT t.*, COALESCE(m.name, 'UNASSIGNED') milestone, p.name project FROM " +
                    "(SELECT tests.*, runs.run_id test_run_id, runs.project_id, runs.name test_run, COALESCE(plans.name, 'UNASSIGNED') test_plan" +
                    " FROM (" + selectTests + ") tests INNER JOIN (" + selectTestRuns + ") runs ON tests.run_id=runs.id" +
                    " LEFT JOIN (" + selectTestPlans + ") plans on runs.plan_id=plans.id) t LEFT JOIN "
                    + "(" + selectMilestones + ") m" + " ON t.milestone_id=m.milestone_id AND" +
                    " t.integration_id=m.integration_id  INNER JOIN (" + selectProjects + ")" + " p " +
                    "ON t.integration_id=p.integration_id AND t.project_id=p.project_id ) final_table " + finalTableWhere;
        }
        String query = "SELECT * FROM " + fromTable + " ORDER BY " + sortByKey +
                " " + sortOrder.name() + " OFFSET :skip LIMIT :limit";
        log.info("sql = " + query);
        log.info("params = " + params);
        final List<DbTestRailsTest> tests = template.query(query, params, isTestCase ? DbTestRailsConverters.listTestCaseRowMapper(mapper) : DbTestRailsConverters.listRowMapper(mapper));
        String countSql = "SELECT count(*) FROM " + fromTable;
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(tests, count);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateForValues(String company, TestRailsTestsFilter filter, List<DbTestRailsCaseField> caseFields) {
        TestRailsTestsFilter.DISTINCT DISTINCT = filter.getDISTINCT();
        Validate.notNull(DISTINCT, "Across must be present for group by query");
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = createWhereClauseAndUpdateParamsForValues(params, filter);
        String groupBySql;
        String table = null;
        String whereClause = "";
        String testTable = TestRailsTestsFilter.CALCULATION.test_case_count.equals(filter.getCALCULATION()) ? TESTRAILS_TEST_CASES : TESTRAILS_TESTS;
        String key = DISTINCT.name();
        switch (DISTINCT) {
            case priority:
            case status:
            case type:
            case assignee:
                groupBySql = key;
                table = testTable;
                break;
            case custom_case_field:
                table = testTable;
                groupBySql = "as " + key;
                Optional<DbTestRailsCaseField> caseField = ListUtils.emptyIfNull(caseFields).stream().filter(field -> field.getSystemName().equals(filter.getCustomAcross())).findFirst();
                if(caseField.isEmpty()){
                    throw new RuntimeException("Failed to get values for " + filter.getCustomAcross() +  ", custom case field not present.");
                }
                CaseField.FieldType fieldType = CaseField.FieldType.fromString(caseField.get().getType());
                if(fieldType.equals(CaseField.FieldType.MULTI_SELECT)){
                    groupBySql = "jsonb_array_elements_text(custom_case_fields->'" + filter.getCustomAcross() + "')"  + groupBySql;
                } else if(fieldType.equals(CaseField.FieldType.CHECKBOX)) {
                    groupBySql = "coalesce(custom_case_fields->>'" + filter.getCustomAcross() + "', 'false') as custom_case_field";
                } else {
                    groupBySql = "custom_case_fields->>'" + filter.getCustomAcross() + "'" + groupBySql;
                }
                break;
            case test_plan:
                groupBySql = "name as " + key;
                table = TESTRAILS_PLANS;
                break;
            case test_run:
                groupBySql = "name as " + key;
                table = TESTRAILS_TEST_RUNS;
                break;
            case milestone:
                groupBySql = "name as " + key;
                table = TESTRAILS_MILESTONES;
                break;
            case project:
                groupBySql = "name as " + key;
                table = TESTRAILS_PROJECTS;
                break;
            default:
                throw new IllegalStateException("Unsupported across: " + DISTINCT);
        }
        if (CollectionUtils.isNotEmpty(conditions)) {
            whereClause = " WHERE " + String.join(" AND ", conditions);
        }
        String query = "SELECT COUNT(*) as ct, " + groupBySql + " FROM " + company + "." + table + whereClause + " GROUP BY " + key;
        log.info("sql = " + query);
        log.info("params = " + params);
        List<DbAggregationResult> dbAggregationResults = template.query(query, params, DbTestRailsConverters.aggRowMapper(mapper, key, filter.getCALCULATION()));
        return DbListResponse.of(dbAggregationResults, dbAggregationResults.size());
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   TestRailsTestsFilter filter,
                                                                   List<TestRailsTestsFilter.DISTINCT> stacks,
                                                                   String configTableKey) {
        TestRailsTestsFilter.DISTINCT ACROSS = filter.getDISTINCT();
        TestRailsTestsFilter.DISTINCT STACK = null;
        boolean isTestCase = TestRailsTestsFilter.CALCULATION.test_case_count.equals(filter.getCALCULATION());
        Validate.notNull(ACROSS, "Across must be present for group by query");
        if (stacks != null && stacks.size() > 0) {
            STACK = stacks.get(0);
        } else {
            STACK = TestRailsTestsFilter.DISTINCT.none;
        }
        final TestRailsTestsFilter.CALCULATION CALCULATION = MoreObjects.firstNonNull(
                filter.getCALCULATION(), TestRailsTestsFilter.CALCULATION.test_count);
        if (StringUtils.isNotEmpty(configTableKey)) {
            ACROSS = TestRailsTestsFilter.DISTINCT.none;
            STACK = TestRailsTestsFilter.DISTINCT.none;
        }
        String acrossLimit = "";
        if (filter.getAcrossLimit() != null && filter.getAcrossLimit() > 0) {
            acrossLimit = " LIMIT " + filter.getAcrossLimit();
        }
        Map<String, Object> params = new HashMap<>();
        String aggSql;
        String outerAggSql;
        String jsonbAgg;
        String orderBySql;
        String aggWhereClause = "";
        switch (CALCULATION) {
            case estimate:
                aggSql = " MIN(estimate) as mn, MAX(estimate) as mx, COUNT(id) as ct, PERCENTILE_DISC(0.5) " +
                        "WITHIN GROUP(ORDER BY estimate)";
                outerAggSql = " MIN(mn) as mn, MAX(mx) as mx, SUM(ct) as ct, PERCENTILE_DISC(0.5) " +
                        "WITHIN GROUP(ORDER BY percentile_disc) ";
                jsonbAgg = " 'total_tests', ct, 'min', mn, 'max', mx, 'mean', percentile_disc";
                orderBySql = " mx DESC ";
                break;
            case estimate_forecast:
                aggSql = " MIN(estimate_forecast) as mn, MAX(estimate_forecast) as mx, COUNT(id) as ct, PERCENTILE_DISC(0.5) " +
                        "WITHIN GROUP(ORDER BY estimate_forecast)";
                outerAggSql = " MIN(mn) as mn, MAX(mx) as mx, SUM(ct) as ct, PERCENTILE_DISC(0.5) " +
                        "WITHIN GROUP(ORDER BY percentile_disc) ";
                jsonbAgg = " 'total_tests', ct, 'min', mn, 'max', mx, 'mean', percentile_disc";
                orderBySql = " mx DESC ";
                break;
            case test_count:
            case test_case_count:
            default:
                aggSql = " COUNT(id) as ct ";
                outerAggSql = " SUM(ct) as ct ";
                jsonbAgg = " 'total_tests', ct";
                orderBySql = " ct DESC ";
                break;
        }
        String groupBySql;
        String across = ACROSS.name();
        String stack = STACK.name();
        switch (ACROSS) {
            case test_plan:
            case test_run:
            case milestone:
            case priority:
            case project:
            case type:
            case status:
            case assignee:
            case custom_case_field:
                groupBySql = " GROUP BY " + ACROSS.name();
                break;
            case trend:
                groupBySql = " GROUP BY trend";
                orderBySql = " trend ASC ";
                across = "trend";
                break;
            case none:
                groupBySql = "";
                across = "";
                break;
            default:
                throw new IllegalStateException("Unsupported across: " + ACROSS);
        }
        switch (STACK) {
            case test_plan:
            case test_run:
            case milestone:
            case priority:
            case project:
            case type:
            case status:
            case assignee:
                groupBySql += " ," + STACK.name();
                break;
            case custom_case_field:
                stack += "_stack";
                groupBySql += " ," + stack;
                break;
            case none:
                groupBySql += "";
                stack = "";
                break;
            default:
                throw new IllegalStateException("Unsupported stack: " + STACK);
        }
        final Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(company, params, filter.getProjects(),
                filter.getTestRuns(), filter.getMilestones(), filter.getTestPlans(), filter.getAssignees(),
                filter.getIntegrationIds(), filter.getStatuses(), filter.getPriorities(), filter.getTestTypes(), filter.getCustomCaseFields(), filter.getExcludeCustomCaseFields(), filter.getCreatedOnTimeRange(), filter.getCreatedAtTimeRange());
        String testsWhere = "";
        boolean needTestCaseTable = TestRailsTestsFilter.CALCULATION.test_case_count.equals(filter.getCALCULATION());
        boolean needProjectTable = needProjectTable(filter, ACROSS, STACK);
        boolean needMilestoneTable = needMilestoneTable(filter, ACROSS, STACK);
        boolean needTestPlanTable = needTestPlanTable(filter, ACROSS, STACK);
        boolean needTestRunTable = (!needTestCaseTable && needProjectTable) || needTestPlanTable || needTestRunTable(filter, ACROSS, STACK);
        if (CollectionUtils.isNotEmpty(conditions.get(TESTRAILS_TESTS))) {
            testsWhere = " WHERE " + String.join(" AND ", conditions.get(TESTRAILS_TESTS));
        }
        String testRunsWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(TESTRAILS_TEST_RUNS))) {
            testRunsWhere = " WHERE " + String.join(" AND ", conditions.get(TESTRAILS_TEST_RUNS));
        }
        String testProjectsWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(TESTRAILS_PROJECTS))) {
            testProjectsWhere = " WHERE " + String.join(" AND ", conditions.get(TESTRAILS_PROJECTS));
        }
        String finalTableWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(TESTRAILS_FINAL_TABLE))) {
            finalTableWhere = " WHERE " + String.join(" AND ", conditions.get(TESTRAILS_FINAL_TABLE));
        }
        String testCaseTableWhere = "";
        if(CollectionUtils.isNotEmpty(conditions.get(TESTRAILS_TEST_CASES))) {
            testCaseTableWhere = " WHERE " + String.join(" AND ", conditions.get(TESTRAILS_TEST_CASES));
        }
        String selectTests = "SELECT * FROM " + company + "." + TESTRAILS_TESTS + testsWhere;
        String selectTestRuns = "SELECT id, name, plan_id, project_id FROM " + company + "." + TESTRAILS_TEST_RUNS + testRunsWhere;
        String selectTestPlans = "SELECT id, name FROM " + company + "." + TESTRAILS_PLANS;
        String selectMilestones = "SELECT milestone_id, integration_id, name FROM " + company + "." + TESTRAILS_MILESTONES;
        String selectProjects = "SELECT project_id, integration_id, name FROM " + company + "." + TESTRAILS_PROJECTS + testProjectsWhere;
        String selectTestCases = "SELECT * FROM " + company + "." + TESTRAILS_TEST_CASES + testCaseTableWhere;
        List<String> outerSelects = new ArrayList<>();
        List<String> innerSelects = new ArrayList<>();
        outerSelects.add("t.*");
        innerSelects.add("tests.*");
        innerSelects.add("date_trunc('day', tests.created_on) as trend");
        if (needMilestoneTable) {
            outerSelects.add("COALESCE(m.name, 'UNASSIGNED') as milestone");
        }
        String customAcross = filter.getCustomAcross();
        String customStack = filter.getCustomStacks() != null && filter.getCustomStacks().size() > 0 ? filter.getCustomStacks().get(0) : null;
        if (ACROSS == TestRailsTestsFilter.DISTINCT.custom_case_field && customAcross != null) {
            List<DbTestRailsCaseField> caseFields = caseFieldDatabaseService.listByFilter(company, TestRailsCaseFieldFilter.builder().systemNames(List.of(customAcross)).build(), 0, 1).getRecords();
            if (caseFields.size() == 0) {
                throw new RuntimeException("Failed to get values of " + customAcross + " for across, custom case field not present.");
            }
            CaseField.FieldType fieldType = CaseField.FieldType.fromString(caseFields.get(0).getType());
            if (fieldType.equals(CaseField.FieldType.MULTI_SELECT)) {
                innerSelects.add("jsonb_array_elements_text(custom_case_fields->'" + customAcross + "') as custom_case_field");
            } else if(fieldType.equals(CaseField.FieldType.CHECKBOX)) {
                innerSelects.add("coalesce(custom_case_fields->>'" + customAcross + "', 'false') as custom_case_field");
            } else {
                innerSelects.add("custom_case_fields->>'" + customAcross + "' as custom_case_field");
            }
        }
        if (STACK == TestRailsTestsFilter.DISTINCT.custom_case_field && customStack != null) {
            List<DbTestRailsCaseField> caseFields = caseFieldDatabaseService.listByFilter(company, TestRailsCaseFieldFilter.builder().systemNames(List.of(customStack)).build(), 0, 1).getRecords();
            if (caseFields.size() == 0) {
                throw new RuntimeException("Failed to get values of " + customStack + " for stacking, custom case field not present.");
            }
            CaseField.FieldType fieldType = CaseField.FieldType.fromString(caseFields.get(0).getType());
            if (fieldType.equals(CaseField.FieldType.MULTI_SELECT)) {
                throw new RuntimeException("Stacking on type of multi select field '"+ customStack +"' is not supported.");
            } else if(fieldType.equals(CaseField.FieldType.CHECKBOX)) {
                innerSelects.add("coalesce(custom_case_fields->>'" + customStack + "', 'false') as custom_case_field_stack");
            } else {
                innerSelects.add("custom_case_fields->>'" + customStack + "' as custom_case_field_stack");
            }
        }
        if (needProjectTable) {
            outerSelects.add("p.name project");
        }
        if (needTestRunTable) {
            innerSelects.add("runs.name test_run");
            innerSelects.add("runs.project_id");
        }
        if (needTestPlanTable) {
            innerSelects.add("COALESCE(plans.name, 'UNASSIGNED') as test_plan");
        }
        final String fromTable = "(SELECT " + String.join(", ", outerSelects) + " FROM " +
                "(SELECT " + String.join(", ", innerSelects) + " FROM (" +
                (needTestCaseTable ? selectTestCases : selectTests ) + ") tests " +
                (needTestRunTable ? "INNER JOIN (" + selectTestRuns + ") runs ON tests.run_id=runs.id" : "") +
                (needTestPlanTable ? " LEFT JOIN (" + selectTestPlans + ") plans on runs.plan_id=plans.id" : "")
                + ") t " +
                (needMilestoneTable ? "LEFT JOIN (" + selectMilestones + ") m ON t.milestone_id=m.milestone_id AND t.integration_id=m.integration_id" : "") +
                (needProjectTable ? " INNER JOIN (" + selectProjects + ") p ON t.integration_id=p.integration_id AND t.project_id=p.project_id" : "") +
                ") final_table " + finalTableWhere;
        List<DbAggregationResult> dbAggregationResults;
        String query;
        if (StringUtils.isNotEmpty(configTableKey)) {
            query = "SELECT '" + configTableKey + "' AS config_key" + "," + aggSql + " FROM " + fromTable + aggWhereClause + " ORDER BY " + orderBySql + acrossLimit;
            across = "config_key";
        } else if (StringUtils.isNotEmpty(stack)) {
            String outerQuery = "SELECT " + across + ", " + outerAggSql + ", jsonb_agg(jsonb_build_object('key', stack, " + jsonbAgg + ")) as stacks FROM (";
            query = outerQuery + " SELECT " + aggSql + ", " + across + ", " + stack + " as stack FROM " + fromTable + aggWhereClause +
                    groupBySql + " ) outer_table GROUP BY " + across + " ORDER BY " + orderBySql + (across.equals("trend") ? "" : acrossLimit);
        } else {
            query = "SELECT " + aggSql + ", " + across + " FROM " + fromTable + aggWhereClause + groupBySql + " ORDER BY " + orderBySql + (across.equals("trend") ? "" : acrossLimit);
        }
        log.info("sql = " + query);
        log.info("params = " + params);
        dbAggregationResults = template.query(query, params, DbTestRailsConverters.aggRowMapper(mapper, across, CALCULATION));
        // For trend report, providing total count of each day
        if((TestRailsTestsFilter.CALCULATION.test_case_count.equals(CALCULATION) || TestRailsTestsFilter.CALCULATION.test_count.equals(CALCULATION)) && TestRailsTestsFilter.DISTINCT.trend.equals(ACROSS)) {
            List<DbAggregationResult> modifiedDbAggregationResults = new ArrayList<>();
            Long lastCount = 0L;
            for(int i=0; i<dbAggregationResults.size(); i++) {
                DbAggregationResult modifiedResult = dbAggregationResults.get(i);
                if(i == 0) {
                    lastCount = modifiedResult.getTotalTests();
                }
                else {
                    modifiedResult = modifiedResult.toBuilder()
                            .totalTests(lastCount + modifiedResult.getTotalTests()).build();
                    lastCount = modifiedResult.getTotalTests();
                }
                modifiedDbAggregationResults.add(modifiedResult);
            }
            long endTime = new Date().getTime() / 1000;
            endTime  = endTime - (endTime % 86400);
            List<DbAggregationResult> filledAggResults = fillRemainingDates(CollectionUtils.isEmpty(modifiedDbAggregationResults) ? endTime: Long.valueOf(modifiedDbAggregationResults.get(0).getKey()), endTime, modifiedDbAggregationResults);
            long skip = filter.getAcrossLimit() != null ? CollectionUtils.size(filledAggResults) - filter.getAcrossLimit() : 0;
            dbAggregationResults = filledAggResults.stream()
                    .skip(skip > 0 ? skip : 0)
                    .collect(Collectors.toList());
        }
        return DbListResponse.of(dbAggregationResults, dbAggregationResults.size());
    }
    private List<DbAggregationResult> fillRemainingDates(Long startTime, Long endTime, List<DbAggregationResult> dbAggregationResults) {
        LocalDate startDate = Instant.ofEpochSecond(startTime).atZone(ZoneId.of("UTC")).toLocalDate();
        LocalDate endDate = Instant.ofEpochSecond(endTime).atZone(ZoneId.of("UTC")).toLocalDate();
        List<LocalDate> allDates= startDate.datesUntil(endDate).collect(Collectors.toList());
        List<Long> epochList = allDates.stream().map(m->m.atStartOfDay(ZoneId.of("UTC")).toEpochSecond()).collect(Collectors.toList());
        if(!epochList.contains(endDate.toEpochDay()))
            epochList.add(endDate.atStartOfDay(ZoneId.of("UTC")).toEpochSecond());

        List<DbAggregationResult> filledAggResults = new ArrayList<>();
        Long lastCount = 0L;
        for (Long key: epochList) {
            lastCount = fillMissingDate(dbAggregationResults, key, filledAggResults, lastCount);
        }
        return filledAggResults;
    }

    private Long fillMissingDate(List<DbAggregationResult> dbAggregationResults, Long key, List<DbAggregationResult> filledAggResults, Long lastCount) {
        List<DbAggregationResult> matched = dbAggregationResults.stream()
                .filter(result -> Long.valueOf(result.getKey()) == key.longValue()).collect(Collectors.toList());
        DbAggregationResult.DbAggregationResultBuilder dbAggregationResultBuilder =  DbAggregationResult.builder();

        if(matched.isEmpty()) {
            dbAggregationResultBuilder.key(key.toString());
            dbAggregationResultBuilder.totalTests(lastCount);
        }
        else  {
            dbAggregationResultBuilder.key(matched.get(0).getKey());
            dbAggregationResultBuilder.totalTests(matched.get(0).getTotalTests());
            lastCount = matched.get(0).getTotalTests();
        }
        filledAggResults.add(dbAggregationResultBuilder.build());
        return lastCount;
    }

    public List<String> createWhereClauseAndUpdateParamsForValues(Map<String, Object> params, TestRailsTestsFilter filter) {
        List<String> conditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(filter.getMilestones())) {
            conditions.add("milestone IN (:milestones)");
            params.put("milestones", filter.getMilestones());
        }
        if (CollectionUtils.isNotEmpty(filter.getProjects())) {
            conditions.add("name IN (:projects)");
            params.put("projects", filter.getProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getTestRuns())) {
            conditions.add("name IN (:testruns)");
            params.put("testruns", filter.getTestRuns());
        }
        if (CollectionUtils.isNotEmpty(filter.getTestPlans())) {
            conditions.add("test_plan IN (:testplans)");
            params.put("testplans", filter.getTestPlans());
        }
        if (CollectionUtils.isNotEmpty(filter.getAssignees())) {
            conditions.add("assignee IN (:assignees)");
            params.put("assignees", filter.getAssignees());
        }
        if (CollectionUtils.isNotEmpty(filter.getStatuses())) {
            conditions.add("status IN (:statuses)");
            params.put("statuses", filter.getStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getPriorities())) {
            conditions.add("priority IN (:priorities)");
            params.put("priorities", filter.getPriorities());
        }
        if (CollectionUtils.isNotEmpty(filter.getTestTypes())) {
            conditions.add("type IN (:testTypes)");
            params.put("testTypes", filter.getTestTypes());
        }
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            conditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids", filter.getIntegrationIds().stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
        }
        return conditions;
    }

    protected Map<String, List<String>> createWhereClauseAndUpdateParams(String company,
                                                                         Map<String, Object> params,
                                                                         List<String> projects,
                                                                         List<String> testRuns,
                                                                         List<String> milestones,
                                                                         List<String> testPlans,
                                                                         List<String> assignees,
                                                                         List<String> integrationIds,
                                                                         List<String> statuses,
                                                                         List<String> priorities,
                                                                         List<String> testTypes,
                                                                         Map<String, Object> customCaseFields,
                                                                         Map<String, Object> excludeCustomCaseFields,
                                                                         ImmutablePair<Long, Long> createdOnTimeRange,
                                                                         ImmutablePair<Long, Long> createdAtTimeRange) {
        List<String> testConditions = new ArrayList<>();
        List<String> testResultConditions = new ArrayList<>();
        List<String> testRunsConditions = new ArrayList<>();
        List<String> projectConditions = new ArrayList<>();
        List<String> finalTableConditions = new ArrayList<>();
        List<String> testCaseConditions = new ArrayList<>();
        List<String> customCaseFieldConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(milestones)) {
            finalTableConditions.add("milestone IN (:milestones)");
            params.put("milestones", milestones);
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            projectConditions.add("name IN (:projects)");
            params.put("projects", projects);
        }
        if (CollectionUtils.isNotEmpty(testRuns)) {
            testRunsConditions.add("name IN (:testruns)");
            params.put("testruns", testRuns);
        }
        if (CollectionUtils.isNotEmpty(testPlans)) {
            finalTableConditions.add("test_plan IN (:testplans)");
            params.put("testplans", testPlans);
        }
        if (CollectionUtils.isNotEmpty(assignees)) {
            testConditions.add("assignee IN (:assignees)");
            params.put("assignees", assignees);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            testConditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids", integrationIds.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
            testResultConditions.add("integration_id IN (:test_result_integration_ids)");
            params.put("test_result_integration_ids", integrationIds.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
            testCaseConditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids", integrationIds.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(statuses)) {
            testConditions.add("status IN (:statuses)");
            params.put("statuses", statuses);
        }
        if (CollectionUtils.isNotEmpty(priorities)) {
            testConditions.add("priority IN (:priorities)");
            testCaseConditions.add("priority IN (:priorities)");
            params.put("priorities", priorities);
        }
        if (CollectionUtils.isNotEmpty(testTypes)) {
            testConditions.add("type IN (:testTypes)");
            testCaseConditions.add("type IN (:testTypes)");
            params.put("testTypes", testTypes);
        }
        if (createdOnTimeRange != null) {
            if (createdOnTimeRange.getLeft() != null) {
                testCaseConditions.add("created_on >= to_timestamp(:created_on_start)");
                testConditions.add("created_on >= to_timestamp(:created_on_start_for_test)");
                params.put("created_on_start", createdOnTimeRange.getLeft());
                params.put("created_on_start_for_test", createdOnTimeRange.getLeft());
            }
            if (createdOnTimeRange.getRight() != null) {
                testCaseConditions.add("created_on <= to_timestamp(:created_on_end)");
                testConditions.add("created_on <= to_timestamp(:created_on_end_for_test)");
                params.put("created_on_end", createdOnTimeRange.getRight());
                params.put("created_on_end_for_test", createdOnTimeRange.getRight());
            }
        }
        if (customCaseFields != null) {
            customCaseFieldConditionsBuilder.createCustomCaseFieldConditions(company, params, "", integrationIds, customCaseFields, customCaseFieldConditions, true);
        }
        if (excludeCustomCaseFields != null) {
            customCaseFieldConditionsBuilder.createCustomCaseFieldConditions(company, params, "", integrationIds, excludeCustomCaseFields, customCaseFieldConditions, false);
        }
        if (CollectionUtils.isNotEmpty(customCaseFieldConditions)){
            testConditions.addAll(customCaseFieldConditions);
            testCaseConditions.addAll(customCaseFieldConditions);
        }
        return Map.of(TESTRAILS_TESTS, testConditions, TESTRAILS_TEST_RUNS, testRunsConditions,
                TESTRAILS_PROJECTS, projectConditions, TESTRAILS_FINAL_TABLE, finalTableConditions,
                TESTRAILS_TEST_RESULTS, testResultConditions, TESTRAILS_TEST_CASES, testCaseConditions);
    }

    public static boolean needProjectTable(TestRailsTestsFilter filter, TestRailsTestsFilter.DISTINCT across, TestRailsTestsFilter.DISTINCT stack) {
        return CollectionUtils.isNotEmpty(filter.getProjects()) || (across != null && across.equals(TestRailsTestsFilter.DISTINCT.project)) || (stack != null && stack.equals(TestRailsTestsFilter.DISTINCT.project));
    }

    public static boolean needTestRunTable(TestRailsTestsFilter filter, TestRailsTestsFilter.DISTINCT across, TestRailsTestsFilter.DISTINCT stack) {
        return CollectionUtils.isNotEmpty(filter.getTestRuns()) || (across != null && across.equals(TestRailsTestsFilter.DISTINCT.test_run)) || (stack != null && stack.equals(TestRailsTestsFilter.DISTINCT.test_run));
    }

    public static boolean needTestPlanTable(TestRailsTestsFilter filter, TestRailsTestsFilter.DISTINCT across, TestRailsTestsFilter.DISTINCT stack) {
        return CollectionUtils.isNotEmpty(filter.getTestPlans()) || (across != null && across.equals(TestRailsTestsFilter.DISTINCT.test_plan)) || (stack != null && stack.equals(TestRailsTestsFilter.DISTINCT.test_plan));
    }

    public static boolean needMilestoneTable(TestRailsTestsFilter filter, TestRailsTestsFilter.DISTINCT across, TestRailsTestsFilter.DISTINCT stack) {
        return CollectionUtils.isNotEmpty(filter.getMilestones()) || (across != null && across.equals(TestRailsTestsFilter.DISTINCT.milestone)) || (stack != null && stack.equals(TestRailsTestsFilter.DISTINCT.milestone));
    }

    //Update is not supported as insert is implemented as an upsert
    @Override
    public Boolean update(String company, DbTestRailsTestPlan t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbTestRailsTestPlan> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbTestRailsTestPlan> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return template.update("DELETE FROM " + company + "." + TESTRAILS_PLANS + " WHERE id=:id",
                Map.of("id", UUID.fromString(id))) != 0;
    }

    public void createTempTables(String company, String integrationId, String currentTime) {
        List<String> tables = List.of(TESTRAILS_TEST_RUN_TEMP_TABLE, TESTRAILS_TEST_TEMP_TABLE, TESTRAILS_TEST_RESULT_TEMP_TABLE);
        tables.stream().forEach(tempTable-> {
            createTempTable(company, integrationId, currentTime, tempTable);
        });
    }

    public void createTempTable(String company, String integrationId, String currentTime, String tempTable) {
        String createSql = "CREATE TABLE IF NOT EXISTS " + company + "." + (tempTable + integrationId + "_" + currentTime) +
                " (id INTEGER NOT NULL)";
        template.getJdbcTemplate().execute(createSql);
        log.info("Successfully created temp table of test run: " + company + "." + (tempTable + integrationId + "_" + currentTime));
    }

    public void insertIntoTestRunTempTable(String company, String integrationId, String currentTime, List<Integer> ids) {
        String insertSql = "INSERT INTO " + company + "." + (TESTRAILS_TEST_RUN_TEMP_TABLE + integrationId + "_" + currentTime) + " VALUES(?)";
        template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            try (PreparedStatement testRunStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)){
                int count = 0;
                for (Integer id : ListUtils.emptyIfNull(ids)){
                    testRunStmt.setObject(1, id);
                    testRunStmt.addBatch();
                    count++;
                    if(count % BATCH_SIZE == 0){
                        testRunStmt.executeBatch();
                        count = 0;
                    }
                }
                if(count % BATCH_SIZE != 0){
                    testRunStmt.executeBatch();
                }
            }
            return true;
        }));
    }
    public void insertIntoTestTempTable(String company, String integrationId, String currentTime, List<Integer> ids) {
        String insertSql = "INSERT INTO " + company + "." + (TESTRAILS_TEST_TEMP_TABLE + integrationId + "_" + currentTime) + " VALUES(?)";
        template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            try (PreparedStatement testStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)){
                int count = 0;
                for (Integer id : ListUtils.emptyIfNull(ids)){
                    testStmt.setObject(1, id);
                    testStmt.addBatch();
                    count++;
                    if(count % BATCH_SIZE == 0){
                        testStmt.executeBatch();
                        count = 0;
                    }
                }
                if(count % BATCH_SIZE != 0){
                    testStmt.executeBatch();
                }
            }
            return true;
        }));
    }
    public void insertIntoTestResultTempTable(String company, String integrationId, String currentTime, List<Long> ids) {
        String insertSql = "INSERT INTO " + company + "." + (TESTRAILS_TEST_RESULT_TEMP_TABLE + integrationId + "_" + currentTime) + " VALUES(?)";
        template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            try (PreparedStatement testResultStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)){
                int count = 0;
                for (Long id : ListUtils.emptyIfNull(ids)){
                    testResultStmt.setObject(1, id);
                    testResultStmt.addBatch();
                    count++;
                    if(count % BATCH_SIZE == 0){
                        testResultStmt.executeBatch();
                        count = 0;
                    }
                }
                if(count % BATCH_SIZE != 0){
                    testResultStmt.executeBatch();
                }
            }
            return true;
        }));
    }
    public void dropTempTables(String company, String integrationId, String currentTime) {
        List<String> tables = List.of(TESTRAILS_TEST_RUN_TEMP_TABLE, TESTRAILS_TEST_TEMP_TABLE, TESTRAILS_TEST_RESULT_TEMP_TABLE);
        tables.forEach(tempTable-> dropTempTable(company, integrationId, currentTime, tempTable));
    }
    public void dropTempTable(String company, String integrationId, String currentTime, String tempTable) {
        String dropSql = "DROP TABLE IF EXISTS " + company + "." + (tempTable + integrationId + "_" + currentTime);
        template.getJdbcTemplate().execute(dropSql);
        log.debug("Successfully dropped temp table of test run: " + company + "." + (tempTable + integrationId + "_" + currentTime));
    }

    public int deleteTestRecords(String company, String integrationId, String currentTime) {
        int affectedCount = deleteTestRunRecord(company, integrationId, currentTime);
        affectedCount += deleteTestRecord(company, integrationId, currentTime);
        affectedCount += deleteTestResultRecord(company, integrationId, currentTime);
        return affectedCount;
    }

    public int deleteTestRunRecord(String company, String integrationId, String currentTime) {
        String deleteSql = "DELETE FROM " + company + "." + TESTRAILS_TEST_RUNS +
                " using " + company + "." + TESTRAILS_TEST_RUNS + " as tr" +
                " left join " + company + "." + (TESTRAILS_TEST_RUN_TEMP_TABLE + integrationId + "_" + currentTime) +
                " as temp on temp.id = tr.run_id" +
                " WHERE tr.id = " + company + "." + TESTRAILS_TEST_RUNS + "." + "id" +
                " AND tr.integration_id=:integration_id AND temp.id is null";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("integration_id", Integer.parseInt(integrationId));
        return template.update(deleteSql, params);
    }

    public int deleteTestRecord(String company, String integrationId, String currentTime) {
        String deleteSql = "DELETE FROM " + company + "." + TESTRAILS_TESTS +
                " using " + company + "." + TESTRAILS_TESTS + " as tr" +
                " left join " + company + "." + (TESTRAILS_TEST_TEMP_TABLE + integrationId + "_" + currentTime) +
                " as temp on temp.id = tr.test_id" +
                " WHERE tr.id = " + company + "." + TESTRAILS_TESTS + "." + "id" +
                " AND tr.integration_id=:integration_id AND temp.id is null";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("integration_id", Integer.parseInt(integrationId));
        return template.update(deleteSql, params);
    }

    public int deleteTestResultRecord(String company, String integrationId, String currentTime) {
        String deleteSql = "DELETE FROM " + company + "." + TESTRAILS_TEST_RESULTS +
                " using " + company + "." + TESTRAILS_TEST_RESULTS + " as tr" +
                " left join " + company + "." + (TESTRAILS_TEST_RESULT_TEMP_TABLE + integrationId + "_" + currentTime) +
                " as temp on temp.id = tr.result_id" +
                " WHERE tr.id = " + company + "." + TESTRAILS_TEST_RESULTS + "." + "id" +
                " AND tr.integration_id=:integration_id AND temp.id is null";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("integration_id", Integer.parseInt(integrationId));
        return template.update(deleteSql, params);
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        final List<String> ddlStmts = List.of(" CREATE TABLE IF NOT EXISTS " + company + "." + TESTRAILS_PLANS +
                        " (" +
                        " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE," +
                        " plan_id INTEGER NOT NULL," +
                        " milestone_id INTEGER," +
                        " project_id INTEGER," +
                        " name VARCHAR," +
                        " description VARCHAR," +
                        " assignee VARCHAR," +
                        " url VARCHAR," +
                        " created_by VARCHAR," +
                        " completed_on TIMESTAMP WITH TIME ZONE," +
                        " created_on TIMESTAMP WITH TIME ZONE," +
                        " is_completed BOOLEAN NOT NULL DEFAULT FALSE," +
                        " UNIQUE(plan_id, integration_id)" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + "." + TESTRAILS_TEST_RUNS +
                        "(" +
                        " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE," +
                        " run_id INTEGER NOT NULL," +
                        " project_id INTEGER NOT NULL," +
                        " plan_id UUID REFERENCES " +
                        company + "." + TESTRAILS_PLANS + "(id) ON DELETE CASCADE," +
                        " milestone_id INTEGER," +
                        " name VARCHAR," +
                        " description VARCHAR," +
                        " url VARCHAR," +
                        " assignee VARCHAR," +
                        " created_by VARCHAR," +
                        " completed_on TIMESTAMP WITH TIME ZONE," +
                        " created_at TIMESTAMP WITH TIME ZONE NOT NULL," +
                        " updated_at TIMESTAMP WITH TIME ZONE NOT NULL," +
                        " test_created_at TIMESTAMP WITH TIME ZONE," +
                        " test_updated_at TIMESTAMP WITH TIME ZONE," +
                        " UNIQUE(run_id, integration_id)" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + "." + TESTRAILS_TESTS +
                        " (" +
                        " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE," +
                        " test_id INTEGER NOT NULL," +
                        " run_id UUID NOT NULL REFERENCES " +
                        company + "." + TESTRAILS_TEST_RUNS + "(id) ON DELETE CASCADE," +
                        " case_id INTEGER," +
                        " milestone_id INTEGER," +
                        " title VARCHAR," +
                        " status VARCHAR," +
                        " type VARCHAR," +
                        " refs VARCHAR," +
                        " priority VARCHAR," +
                        " assignee VARCHAR," +
                        " estimate INTEGER," +
                        " estimate_forecast INTEGER," +
                        " custom_case_fields JSONB," +
                        " created_on TIMESTAMP WITH TIME ZONE," +
                        " created_at TIMESTAMP WITH TIME ZONE NOT NULL," +
                        " updated_at TIMESTAMP WITH TIME ZONE NOT NULL," +
                        " UNIQUE(test_id, integration_id)" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + "." + TESTRAILS_TEST_RESULTS +
                        " (" +
                        " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE," +
                        " result_id INTEGER NOT NULL," +
                        " test_id UUID NOT NULL REFERENCES " +
                        company + "." + TESTRAILS_TESTS + "(id) ON DELETE CASCADE," +
                        " status VARCHAR," +
                        " defects VARCHAR[]," +
                        " assignee VARCHAR," +
                        " creator VARCHAR," +
                        " created_on TIMESTAMP WITH TIME ZONE," +
                        " comment VARCHAR," +
                        " elapsed VARCHAR," +
                        " created_at TIMESTAMP WITH TIME ZONE NOT NULL," +
                        " updated_at TIMESTAMP WITH TIME ZONE NOT NULL," +
                        " UNIQUE(result_id, integration_id)" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_TESTS + "_integration_id_idx ON " + company + "." + TESTRAILS_TESTS + "(integration_id)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_TESTS + "_custom_case_fields_idx ON " + company + "." + TESTRAILS_TESTS + " USING GIN(custom_case_fields)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_PLANS + "_name_idx on " + company + "." + TESTRAILS_PLANS + "(name)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_TEST_RUNS + "_name_idx on " + company + "." + TESTRAILS_TEST_RUNS + "(name)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_TESTS + "_priority_idx on " + company + "." + TESTRAILS_TESTS + "(priority)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_TESTS + "_type_idx on " + company + "." + TESTRAILS_TESTS + "(type)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_TESTS + "_status_idx on " + company + "." + TESTRAILS_TESTS + "(status)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_TESTS + "_assignee_idx on " + company + "." + TESTRAILS_TESTS + "(assignee)");
        ddlStmts.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    private Timestamp getTimestamp(Date date) {
        return date != null ? Timestamp.from(date.toInstant()) : null;
    }
}