package io.levelops.integrations.testrails.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.utils.ListUtils;
import io.levelops.integrations.testrails.client.TestRailsClient;
import io.levelops.integrations.testrails.client.TestRailsClientException;
import io.levelops.integrations.testrails.models.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class can be used for enriching {@link List<Project>} and {@link List<TestPlan>}.
 * A {@link ForkJoinPool} is maintained for performing all
 * the enrichment tasks. The {@link ForkJoinPool} is shared across all the jobs.
 */
@Log4j2
public class TestRailsEnrichmentService {

    private final int forkThreshold;
    private final ForkJoinPool pool;
    public static final String UNASSIGNED = "_UNASSIGNED_";

    /**
     * all arg constructor
     *
     * @param threadCount   the number of threads for the {@link ForkJoinPool}
     * @param forkThreshold the max number of projects & test plans to be enriched
     *                      by each{@link EnrichProjectTask} & {@link EnrichTestPlanTask}
     */
    public TestRailsEnrichmentService(int threadCount, int forkThreshold) {
        this.forkThreshold = forkThreshold;
        this.pool = new ForkJoinPool(threadCount);
    }

    /**
     * call this method to enrich {@link Project}
     *
     * @param testRailsClient {@link TestRailsClient} used to make calls to TestRails
     * @param integrationKey  {@link IntegrationKey} for the job, used for logging purposes
     * @param projects        {@link List<Project>} to be enriched
     * @return {@link List<Project>} enriched tickets
     */
    public List<Project> enrichProjects(TestRailsClient testRailsClient, IntegrationKey integrationKey,
                                        List<Project> projects) {
        EnrichProjectTask enrichProjectTask = new EnrichProjectTask(testRailsClient, projects, forkThreshold);
        log.info("enrichProjects: started enriching {} projects for {}", projects.size(), integrationKey);
        return pool.invoke(enrichProjectTask);
    }

    /**
     * call this method to enrich {@link TestRailsTestSuite}
     *
     * @param testRailsClient {@link TestRailsClient} used to make calls to TestRails
     * @param integrationKey  {@link IntegrationKey} for the job, used for logging purposes
     * @param testSuites        {@link List<TestRailsTestSuite>} to be enriched
     * @param projectId {@link Integer} as a project Id
     * @return {@link List<TestRailsTestSuite>} enriched test suites
     */
    public List<TestRailsTestSuite> enrichTestSuites(TestRailsClient testRailsClient, IntegrationKey integrationKey,
                                        List<TestRailsTestSuite> testSuites, Integer projectId) throws TestRailsClientException {
        Map<Integer, User> usersById = testRailsClient.getUsersByProjectId(projectId).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (oldUserEntry, newUserEntry) -> oldUserEntry));
        Map<Integer, Test.CaseType> caseTypesById = testRailsClient.getCaseTypes().stream()
                .collect(Collectors.toMap(Test.CaseType::getId, Function.identity()));
        Map<Integer, Test.Priority> prioritiesById = testRailsClient.getPriorities().stream()
                .collect(Collectors.toMap(Test.Priority::getId, Function.identity()));
        Map<String, CaseField> caseFieldsBySystemName = testRailsClient.getCaseFields().stream()
                .collect(Collectors.toMap(CaseField::getSystemName, Function.identity()));
        EnrichTestSuiteTask enrichTestSuiteTask = new EnrichTestSuiteTask(testRailsClient, usersById, caseTypesById,
                prioritiesById, caseFieldsBySystemName, testSuites, forkThreshold);
        log.info("enrichTestSuites: started enriching {} projects for {}", testSuites.size(), integrationKey);
        return pool.invoke(enrichTestSuiteTask);
    }

    /**
     * call this method to enrich {@link TestPlan}
     *
     * @param testRailsClient {@link TestRailsClient} used to make calls to TestRails
     * @param integrationKey  {@link IntegrationKey} for the job, used for logging purposes
     * @param testPlans       {@link List<TestPlan>} to be enriched
     * @param projectId
     * @return {@link List<TestPlan>} enriched tickets
     */
    public List<TestPlan> enrichTestPlans(TestRailsClient testRailsClient, IntegrationKey integrationKey,
                                          List<TestPlan> testPlans, Integer projectId) throws TestRailsClientException {
        Map<Integer, User> usersById = testRailsClient.getUsersByProjectId(projectId).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (oldUserEntry, newUserEntry) -> oldUserEntry));
        Map<Integer, Test.Status> statusesById = testRailsClient.getStatuses().stream()
                .collect(Collectors.toMap(Test.Status::getId, Function.identity()));
        Map<Integer, Test.CaseType> caseTypesById = testRailsClient.getCaseTypes().stream()
                .collect(Collectors.toMap(Test.CaseType::getId, Function.identity()));
        Map<Integer, Test.Priority> prioritiesById = testRailsClient.getPriorities().stream()
                .collect(Collectors.toMap(Test.Priority::getId, Function.identity()));
        Map<String, CaseField> caseFieldsBySystemName = testRailsClient.getCaseFields().stream()
                .collect(Collectors.toMap(CaseField::getSystemName, Function.identity()));
        EnrichTestPlanTask enrichTestPlanTask = new EnrichTestPlanTask(testRailsClient, testPlans,
                usersById, statusesById, caseTypesById, prioritiesById, caseFieldsBySystemName, forkThreshold);
        log.info("enrichTestPlans: started enriching {} testPlans for {}", testPlans.size(), integrationKey);
        return pool.invoke(enrichTestPlanTask);
    }

    /**
     * call this method to enrich {@link TestRun}
     *
     * @param testRailsClient {@link TestRailsClient} used to make calls to TestRails
     * @param integrationKey  {@link IntegrationKey} for the job, used for logging purposes
     * @param testRuns       {@link List<TestRun>} to be enriched
     * @return {@link List<TestRun>} enriched test runs
     */
    public List<TestRun> enrichTestRuns(TestRailsClient testRailsClient, IntegrationKey integrationKey,
                                          List<TestRun> testRuns, Integer projectId) throws TestRailsClientException {
        Map<Integer, User> usersById = testRailsClient.getUsersByProjectId(projectId).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (oldUserEntry, newUserEntry) -> oldUserEntry));
        Map<Integer, Test.Status> statusesById = testRailsClient.getStatuses().stream()
                .collect(Collectors.toMap(Test.Status::getId, Function.identity()));
        Map<Integer, Test.CaseType> caseTypesById = testRailsClient.getCaseTypes().stream()
                .collect(Collectors.toMap(Test.CaseType::getId, Function.identity()));
        Map<Integer, Test.Priority> prioritiesById = testRailsClient.getPriorities().stream()
                .collect(Collectors.toMap(Test.Priority::getId, Function.identity()));
        Map<String, CaseField> caseFieldsBySystemName = testRailsClient.getCaseFields().stream()
                .collect(Collectors.toMap(CaseField::getSystemName, Function.identity()));
        EnrichTestRunTask enrichTestRunTask = new EnrichTestRunTask(testRailsClient, testRuns,
                usersById, statusesById, caseTypesById, prioritiesById, caseFieldsBySystemName, forkThreshold);
        log.info("enrichTestRuns: started enriching {} testRuns for {}", testRuns.size(), integrationKey);
        return pool.invoke(enrichTestRunTask);
    }


    public static void sanitizeCustomCaseFields(Map<String, Object> customCaseFields, int projectId,
                                                        Map<String, CaseField> caseFields, Map<Integer, User> users, BiConsumer<String, Object> dynamicCustomFields) {
        customCaseFields.forEach((key, value) -> {
            if (caseFields.containsKey(key)) {
                CaseField.FieldType fieldType = caseFields.get(key).getType();
                if (fieldType == CaseField.FieldType.USER) {
                    dynamicCustomFields.accept(key, Optional.ofNullable(users.get((int)value))
                            .map(User::getEmail).orElse(UNASSIGNED));
                } else if (fieldType == CaseField.FieldType.MULTI_SELECT) {
                    for (CaseField.FieldConfig config : caseFields.get(key).getConfigs()) {
                        if ((config.getContext().getIsGlobal() && ListUtils.emptyIfNull(config.getContext().getProjectIds()).isEmpty()) || config.getContext().getProjectIds().contains(projectId)) {
                            Map<Integer, String> items = config.getOptions().getItemsMap();
                            List<String> values = new ArrayList<>();
                            for (int each : (List<Integer>) value) {
                                values.add(items.get(each));
                            }
                            dynamicCustomFields.accept(key, values);
                        }
                    }
                } else if (fieldType == CaseField.FieldType.DROPDOWN) {
                    boolean isNotMapped = true;
                    for (CaseField.FieldConfig config : caseFields.get(key).getConfigs()) {
                        if (config.getContext().getIsGlobal() || CollectionUtils.emptyIfNull(config.getContext().getProjectIds()).contains(projectId)) {
                            Map<Integer, String> items = config.getOptions().getItemsMap();
                            dynamicCustomFields.accept(key, items.get((int)value));
                            isNotMapped = false;
                        }
                    }
                    if (isNotMapped){
                        dynamicCustomFields.accept(key, String.valueOf(value));
                    }
                }
            }
        });
    }

    /**
     * Implementation of the {@link RecursiveTask} for enriching a {@link List<Project>}
     */
    @Log4j2
    static class EnrichProjectTask extends RecursiveTask<List<Project>> {

        private final TestRailsClient testRailsClient;
        private final List<Project> projects;
        private final int forkThreshold;

        /**
         * all arg constructor
         *
         * @param testRailsClient {@link TestRailsClient} used to make calls to TestRails
         * @param projects        {@link List<Project>} to be enriched
         * @param forkThreshold   {@link IntegrationKey} for the job, used for logging purposes
         */
        public EnrichProjectTask(TestRailsClient testRailsClient, List<Project> projects, int forkThreshold) {
            this.testRailsClient = testRailsClient;
            this.projects = projects;
            this.forkThreshold = forkThreshold;
        }

        /**
         * The computation to be performed by this task.
         * Forks the task if {@link EnrichProjectTask#projects} has more than
         * {@link EnrichProjectTask#forkThreshold} projects
         *
         * @return {@link List<Project>} enriched projects
         */
        @Override
        protected List<Project> compute() {
            if (projects.size() > forkThreshold) {
                return computeInSubTask();
            } else {
                return enrichProjects();
            }
        }

        /**
         * Creates and executes {@link EnrichProjectTask} sub-tasks and
         * then joins the results returned from the sub-tasks
         *
         * @return {@link List<Project>} enriched projects
         */
        private List<Project> computeInSubTask() {
            int size = projects.size();
            EnrichProjectTask enrichProjectSubTask1 = new EnrichProjectTask(testRailsClient,
                    projects.subList(0, size / 2), forkThreshold);
            EnrichProjectTask enrichProjectSubTask2 = new EnrichProjectTask(testRailsClient,
                    projects.subList(size / 2, size), forkThreshold);
            enrichProjectSubTask1.fork();
            enrichProjectSubTask2.fork();
            ArrayList<Project> enrichedProjects = new ArrayList<>(enrichProjectSubTask1.join());
            enrichedProjects.addAll(enrichProjectSubTask2.join());
            return enrichedProjects;
        }

        /**
         * Enriches each {@link Project}
         *
         * @return {@link List<Project>} enriched projects
         */
        private List<Project> enrichProjects() {
            List<Project> enrichedProjects = this.projects
                    .stream()
                    .map(this::enrichProject)
                    .collect(Collectors.toList());
            log.debug("process: enriched {} projects", enrichedProjects.size());
            return enrichedProjects;
        }

        /**
         * Enriches {@code project} with {@link List<Milestone>}
         *
         * @param project the {@link Project} to be enriched
         * @return {@link Project} the enriched project
         */
        private Project enrichProject(Project project) {
            try {
                Integer id = project.getId();
                List<Milestone> milestones = testRailsClient.getMilestones(id)
                        .collect(Collectors.toList());
                return project.toBuilder()
                        .milestones(milestones)
                        .build();
            } catch (TestRailsClientException e) {
                log.error("process: encountered client exception while enriching project "
                        + e.getMessage(), e);
                return project;
            }
        }
    }

    /**
     * Implementation of the {@link RecursiveTask} for enriching a {@link List<Project>}
     */
    @Log4j2
    static class EnrichTestSuiteTask extends RecursiveTask<List<TestRailsTestSuite>> {

        private final TestRailsClient testRailsClient;
        private final Map<Integer, User> users;
        private final Map<Integer, Test.CaseType> caseTypes;
        private final Map<Integer, Test.Priority> priorities;
        private final Map<String, CaseField> caseFields;
        private final List<TestRailsTestSuite> testSuites;
        private final int forkThreshold;

        /**
         * all arg constructor
         *
         * @param testRailsClient {@link TestRailsClient} used to make calls to TestRails
         * @param users           {@link List<User>} to be used to enrich testcase
         * @param caseTypes       {@link List<Test.CaseType>} to be used to enrich corresponding testcases
         * @param priorities      {@link List<Test.Priority>} to be used to enrich corresponding testcases
         * @param testSuites        {@link List<TestRailsTestSuite>} to be enriched
         * @param forkThreshold   {@link IntegrationKey} for the job, used for logging purposes
         */
        public EnrichTestSuiteTask(TestRailsClient testRailsClient,
                                   Map<Integer, User> users,
                                   Map<Integer, Test.CaseType> caseTypes,
                                   Map<Integer, Test.Priority> priorities,
                                   Map<String, CaseField> caseFields,
                                   List<TestRailsTestSuite> testSuites,
                                   int forkThreshold) {
            this.testRailsClient = testRailsClient;
            this.users = users;
            this.caseTypes = caseTypes;
            this.priorities = priorities;
            this.caseFields = caseFields;
            this.testSuites = testSuites;
            this.forkThreshold = forkThreshold;
        }

        /**
         * The computation to be performed by this task.
         * Forks the task if {@link EnrichTestSuiteTask#testSuites} has more than
         * {@link EnrichTestSuiteTask#forkThreshold} test suites
         *
         * @return {@link List<TestRailsTestSuite>} enriched test suites
         */
        @Override
        protected List<TestRailsTestSuite> compute() {
            if (testSuites.size() > forkThreshold) {
                return computeInSubTask();
            } else {
                return enrichTestSuites();
            }
        }

        /**
         * Creates and executes {@link EnrichTestSuiteTask} sub-tasks and
         * then joins the results returned from the sub-tasks
         *
         * @return {@link List<TestRailsTestSuite>} enriched testsuites
         */
        private List<TestRailsTestSuite> computeInSubTask() {
            int size = testSuites.size();
            EnrichTestSuiteTask enrichTestSuiteTask1 = new EnrichTestSuiteTask(testRailsClient,
                    users, caseTypes, priorities, caseFields, testSuites.subList(0, size / 2), forkThreshold);
            EnrichTestSuiteTask enrichTestSuiteTask2 = new EnrichTestSuiteTask(testRailsClient,
                    users, caseTypes, priorities, caseFields, testSuites.subList(size / 2, size), forkThreshold);
            enrichTestSuiteTask1.fork();
            enrichTestSuiteTask2.fork();
            ArrayList<TestRailsTestSuite> enrichedTestSuites = new ArrayList<>(enrichTestSuiteTask1.join());
            enrichedTestSuites.addAll(enrichTestSuiteTask2.join());
            return enrichedTestSuites;
        }

        /**
         * Enriches each {@link TestRailsTestSuite}
         *
         * @return {@link List<TestRailsTestSuite>} enriched test suites
         */
        private List<TestRailsTestSuite> enrichTestSuites() {
            List<TestRailsTestSuite> enrichedProjects = this.testSuites
                    .stream()
                    .map(this::enrichTestSuite)
                    .collect(Collectors.toList());
            log.debug("process: enriched {} projects", enrichedProjects.size());
            return enrichedProjects;
        }

        /**
         * Enriches {@code test suite} with {@link List<TestRailsTestSuite>}
         *
         * @param testSuite the {@link TestRailsTestSuite} to be enriched
         * @return {@link List<TestRailsTestSuite>} the enriched test suites
         */
        private TestRailsTestSuite enrichTestSuite(TestRailsTestSuite testSuite) {
            try {
                return testSuite.toBuilder()
                        .testCases(testRailsClient.getTestCases(testSuite.getProjectId(), testSuite.getId())
                                .map(testCase -> {
                                    sanitizeCustomCaseFields(testCase.getDynamicCustomFields(), testSuite.getProjectId(),
                                            caseFields, users, testCase::setDynamicCustomFields);
                                    return testCase.toBuilder()
                                            .customCaseFields(testCase.getDynamicCustomFields())
                                            .projectId(testSuite.getProjectId())
                                            .createdByUser(Optional.ofNullable(users.get(testCase.getCreatedBy())).map(User::getEmail).orElse(null))
                                            .updatedByUser(Optional.ofNullable(users.get(testCase.getUpdatedBy())).map(User::getEmail).orElse(null))
                                            .type(caseTypes.get(testCase.getTypeId()).getName())
                                            .priority(priorities.get(testCase.getPriorityId()).getName())
                                            .build();
                                })
                                .collect(Collectors.toList()))
                        .build();
            } catch (TestRailsClientException e) {
                log.error("process: encountered client exception while enriching project "
                        + e.getMessage(), e);
                return testSuite;
            }
        }
    }

    /**
     * Implementation of the {@link RecursiveTask} for enriching a {@link List<TestPlan>}
     */
    @Log4j2
    static class EnrichTestPlanTask extends RecursiveTask<List<TestPlan>> {

        private final TestRailsClient testRailsClient;
        private final List<TestPlan> testPlans;
        private final Map<Integer, User> users;
        private final Map<Integer, Test.Status> statuses;
        private final Map<Integer, Test.CaseType> caseTypes;
        private final Map<Integer, Test.Priority> priorities;
        private final Map<String, CaseField> caseFields;
        private final int forkThreshold;

        /**
         * all arg constructor
         *
         * @param testRailsClient {@link TestRailsClient} used to make calls to TestRails
         * @param testPlans       {@link List<TestPlan>} to be enriched
         * @param users           {@link List<User>} to be used to enrich test plans
         * @param statuses        {@link List<Test.Status>} to be used to enrich corresponding tests
         * @param caseTypes       {@link List<Test.CaseType>} to be used to enrich corresponding tests
         * @param priorities      {@link List<Test.Priority>} to be used to enrich corresponding tests
         * @param forkThreshold   {@link IntegrationKey} for the job, used for logging purposes
         */
        EnrichTestPlanTask(TestRailsClient testRailsClient, List<TestPlan> testPlans, Map<Integer, User> users,
                           Map<Integer, Test.Status> statuses, Map<Integer, Test.CaseType> caseTypes,
                           Map<Integer, Test.Priority> priorities, Map<String, CaseField> caseFields, int forkThreshold) {
            this.testRailsClient = testRailsClient;
            this.testPlans = testPlans;
            this.users = users;
            this.statuses = statuses;
            this.caseTypes = caseTypes;
            this.priorities = priorities;
            this.caseFields = caseFields;
            this.forkThreshold = forkThreshold;
        }

        /**
         * The computation to be performed by this task.
         * Forks the task if {@link EnrichTestPlanTask#testPlans} has more than
         * {@link EnrichTestPlanTask#forkThreshold} test plans
         *
         * @return {@link List<TestPlan>} enriched test plans
         */
        @Override
        protected List<TestPlan> compute() {
            if (testPlans.size() > forkThreshold)
                return computeInSubTask();
            else
                return enrichTestPlans();
        }

        /**
         * Creates and executes {@link EnrichTestPlanTask} sub-tasks and
         * then joins the results returned from the sub-tasks
         *
         * @return {@link List<TestPlan>} enriched test plans
         */
        private List<TestPlan> computeInSubTask() {
            int size = testPlans.size();
            EnrichTestPlanTask enrichTestPlanTask1 = new EnrichTestPlanTask(testRailsClient,
                    testPlans.subList(0, size / 2), users, statuses, caseTypes, priorities, caseFields, forkThreshold);
            EnrichTestPlanTask enrichTestPlanTask2 = new EnrichTestPlanTask(testRailsClient,
                    testPlans.subList(size / 2, size), users, statuses, caseTypes, priorities, caseFields, forkThreshold);
            enrichTestPlanTask1.fork();
            enrichTestPlanTask2.fork();
            ArrayList<TestPlan> enrichedTestPlans = new ArrayList<>(enrichTestPlanTask1.join());
            enrichedTestPlans.addAll(enrichTestPlanTask2.join());
            return enrichedTestPlans;
        }

        /**
         * Enriches each {@link TestPlan}
         *
         * @return {@link List<TestPlan>} enriched test plans
         */
        private List<TestPlan> enrichTestPlans() {
            List<TestPlan> enrichedTestPlans = this.testPlans
                    .stream()
                    .map(this::enrichTestPlan)
                    .collect(Collectors.toList());
            log.debug("process: enriched {} test plans", enrichedTestPlans.size());
            return enrichedTestPlans;
        }

        /**
         * Enriches {@code ticket} with {@link List<TestRun>}
         *
         * @param testPlan the {@link TestPlan} to be enriched
         * @return {@link TestPlan} the enriched test plan
         */
        private TestPlan enrichTestPlan(TestPlan testPlan) {
            ArrayList<TestRun> enrichedTestRuns = new ArrayList<>();
            try {
                final TestPlan.Entry[] entries = testRailsClient.getPlan(testPlan.getId()).getEntries();
                if (entries != null && entries.length != 0) {
                    for (TestPlan.Entry entry : entries) {
                        final TestRun[] testRuns = entry.getRuns();
                        if (testRuns != null && testRuns.length != 0) {
                            List<TestRun> testRunEntries = Arrays.stream(testRuns)
                                    .collect(Collectors.toList());
                            List<TestRun> enrichedTestRunsForEachEntry = testRunEntries
                                    .stream()
                                    .map(this::parseAndEnrichTestRun)
                                    .collect(Collectors.toList());
                            enrichedTestRuns.addAll(enrichedTestRunsForEachEntry);
                        }
                    }
                }
                return testPlan.toBuilder()
                        .testRuns(enrichedTestRuns)
                        .build();
            } catch (TestRailsClientException e) {
                log.error("process: encountered client exception while fetching entries in test plan "
                        + e.getMessage(), e);
                return testPlan.toBuilder()
                        .creator(users.get(testPlan.getCreatedBy()))
                        .assignee(users.get(testPlan.getAssignedtoId()))
                        .statusCounts(testPlan.getStatusCounts())
                        .build();
            }
        }

        /**
         * Enriches {@code testRun} with {@link List<Test>}, {@link Test.Status}
         *
         * @param testRun the {@link TestRun} to be enriched
         * @return {@link TestRun} the enriched test run
         */
        private TestRun parseAndEnrichTestRun(TestRun testRun) {
            List<Test> tests;
            try {
                List<Test.Result> testResults = enrichTestResults(testRun.getId());
                tests = testRailsClient.getTests(testRun.getId())
                        .map(test -> {
                            sanitizeCustomCaseFields(test.getDynamicCustomFields(), testRun.getProjectId(),
                                    caseFields, users, test::setDynamicCustomFields);
                            List<Test.Result> results = testResults.stream().filter(testResult -> Objects.equals(testResult.getTestId(), test.getId())).collect(Collectors.toList());
                            Test.Result firstResult = CollectionUtils.isNotEmpty(results) ? results.get(results.size() - 1) : null;
                            return test.toBuilder()
                                    .createdOn(firstResult!= null ? firstResult.getCreatedOn() : testRun.getCreatedOn())
                                    .customCaseFields(test.getDynamicCustomFields())
                                    .status(statuses.get(test.getStatusId()).getLabel())
                                    .assignee(users.get(test.getAssignedToId()))
                                    .results(results)
                                    .type(caseTypes.get(test.getTypeId()).getName())
                                    .priority(priorities.get(test.getPriorityId()).getName())
                                    .build();
                        })
                        .collect(Collectors.toList());
            } catch (TestRailsClientException e) {
                log.error("process: encountered client exception while enriching test run "
                        + e.getMessage(), e);
                return testRun;
            }
            return testRun.toBuilder()
                    .tests(tests)
                    .assignee(users.get(testRun.getAssignedToId()))
                    .creator(users.get(testRun.getCreatedBy()))
                    .statusCounts(testRun.getStatusCounts())
                    .build();
        }

        /**
         * Enriches {@code ticket} with {@link List<Test.Result>}
         *
         * @param testRunId the {@link Test.Result} to be enriched
         * @return {@link Test} the enriched test result
         */
        private List<Test.Result> enrichTestResults(int testRunId){
            try{
                return testRailsClient.getResults(testRunId)
                        .map(testResult -> testResult.toBuilder()
                                .assignee(users.get(testResult.getAssignedToId()))
                                .status(Optional.ofNullable(statuses.get(testResult.getStatusId())).map(Test.Status::getLabel).orElse(null))
                                .creator(users.get(testResult.getCreatedBy()))
                                .build()).collect(Collectors.toList());
            }
            catch (TestRailsClientException e){
                log.error("process: encountered client exception while enriching test results "
                        + e.getMessage(), e);
            }
            return List.of();
        }
    }

    /**
     * Implementation of the {@link RecursiveTask} for enriching a {@link List<TestRun>}
     */
    @Log4j2
    static class EnrichTestRunTask extends RecursiveTask<List<TestRun>> {

        private final TestRailsClient testRailsClient;
        private final List<TestRun> testRuns;
        private final Map<Integer, User> users;
        private final Map<Integer, Test.Status> statuses;
        private final Map<Integer, Test.CaseType> caseTypes;
        private final Map<Integer, Test.Priority> priorities;
        private final Map<String, CaseField> caseFields;
        private final int forkThreshold;

        /**
         * all arg constructor
         *
         * @param testRailsClient {@link TestRailsClient} used to make calls to TestRails
         * @param testRuns        {@link List<TestRun>} to be enriched
         * @param users           {@link List<User>} to be used to enrich test runs
         * @param statuses        {@link List<Test.Status>} to be used to enrich corresponding tests
         * @param caseTypes       {@link List<Test.CaseType>} to be used to enrich corresponding tests
         * @param priorities      {@link List<Test.Priority>} to be used to enrich corresponding tests
         * @param forkThreshold   {@link IntegrationKey} for the job, used for logging purposes
         */
        EnrichTestRunTask(TestRailsClient testRailsClient, List<TestRun> testRuns, Map<Integer, User> users,
                           Map<Integer, Test.Status> statuses, Map<Integer, Test.CaseType> caseTypes,
                           Map<Integer, Test.Priority> priorities, Map<String, CaseField> caseFields, int forkThreshold) {
            this.testRailsClient = testRailsClient;
            this.testRuns = testRuns;
            this.users = users;
            this.statuses = statuses;
            this.caseTypes = caseTypes;
            this.priorities = priorities;
            this.caseFields = caseFields;
            this.forkThreshold = forkThreshold;
        }

        /**
         * The computation to be performed by this task.
         * Forks the task if {@link EnrichTestRunTask#testRuns} has more than
         * {@link EnrichTestRunTask#forkThreshold} test runs
         *
         * @return {@link List<TestRun>} enriched test runs
         */
        @Override
        protected List<TestRun> compute() {
            if (testRuns.size() > forkThreshold)
                return computeInSubTask();
            else
                return enrichTestRuns();
        }

        /**
         * Creates and executes {@link EnrichTestRunTask} sub-tasks and
         * then joins the results returned from the sub-tasks
         *
         * @return {@link List<TestRun>} enriched test runs
         */
        private List<TestRun> computeInSubTask() {
            int size = testRuns.size();
            EnrichTestRunTask enrichTestRunTask1 = new EnrichTestRunTask(testRailsClient,
                    testRuns.subList(0, size / 2), users, statuses, caseTypes, priorities, caseFields, forkThreshold);
            EnrichTestRunTask enrichTestRunTask2 = new EnrichTestRunTask(testRailsClient,
                    testRuns.subList(size / 2, size), users, statuses, caseTypes, priorities, caseFields, forkThreshold);
            enrichTestRunTask1.fork();
            enrichTestRunTask2.fork();
            ArrayList<TestRun> enrichedTestRuns = new ArrayList<>(enrichTestRunTask1.join());
            enrichedTestRuns.addAll(enrichTestRunTask2.join());
            return enrichedTestRuns;
        }

        /**
         * Enriches each {@link TestRun}
         *
         * @return {@link List<TestRun>} enriched test runs
         */
        private List<TestRun> enrichTestRuns() {
            List<TestRun> enrichedTestRuns = this.testRuns
                    .stream()
                    .map(this::enrichTestRun)
                    .collect(Collectors.toList());
            log.debug("process: enriched {} test runs", enrichedTestRuns.size());
            return enrichedTestRuns;
        }

        /**
         * Enriches {@code ticket} with {@link List<TestRun>}
         *
         * @param testRun the {@link TestRun} to be enriched
         * @return {@link TestRun} the enriched test run
         */
        private TestRun enrichTestRun(TestRun testRun) {
            List<Test> tests;
            try {
                List<Test.Result> testResults = enrichTestResults(testRun.getId());
                tests = testRailsClient.getTests(testRun.getId())
                        .map(test -> {
                            sanitizeCustomCaseFields(test.getDynamicCustomFields(), testRun.getProjectId(),
                                    caseFields, users, test::setDynamicCustomFields);
                            List<Test.Result> results = testResults.stream().filter(testResult -> Objects.equals(testResult.getTestId(), test.getId())).collect(Collectors.toList());
                            Test.Result firstResult = CollectionUtils.isNotEmpty(results) ? results.get(results.size() - 1) : null;
                            return test.toBuilder()
                                    .createdOn(firstResult != null ? firstResult.getCreatedOn() : testRun.getCreatedOn())
                                    .customCaseFields(test.getDynamicCustomFields())
                                    .status(statuses.get(test.getStatusId()).getLabel())
                                    .assignee(users.get(test.getAssignedToId()))
                                    .results(results)
                                    .type(caseTypes.get(test.getTypeId()).getName())
                                    .priority(priorities.get(test.getPriorityId()).getName())
                                    .build();
                        })
                        .collect(Collectors.toList());
            } catch (TestRailsClientException e) {
                log.error("process: encountered client exception while enriching test run "
                        + e.getMessage(), e);
                return testRun;
            }
            return testRun.toBuilder()
                    .tests(tests)
                    .assignee(users.get(testRun.getAssignedToId()))
                    .creator(users.get(testRun.getCreatedBy()))
                    .statusCounts(testRun.getStatusCounts())
                    .build();
        }

        /**
         * Enriches {@code ticket} with {@link List<Test.Result>}
         *
         * @param testRunId the {@link Test.Result} to be enriched
         * @return {@link Test} the enriched test result
         */
        private List<Test.Result> enrichTestResults(int testRunId){
            try{
                return testRailsClient.getResults(testRunId)
                        .map(testResult -> testResult.toBuilder()
                                .assignee(users.get(testResult.getAssignedToId()))
                                .status(Optional.ofNullable(statuses.get(testResult.getStatusId())).map(Test.Status::getLabel).orElse(null))
                                .creator(users.get(testResult.getCreatedBy()))
                                .build()).collect(Collectors.toList());
            }
            catch (TestRailsClientException e){
                log.error("process: encountered client exception while enriching test results "
                        + e.getMessage(), e);
            }
            return List.of();
        }
    }
}
