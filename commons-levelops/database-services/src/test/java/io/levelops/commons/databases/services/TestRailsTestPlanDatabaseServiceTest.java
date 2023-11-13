package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.DbTestRailsConverters;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsCaseField;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsProject;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTest;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTestCase;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTestPlan;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTestRun;
import io.levelops.commons.databases.models.filters.TestRailsCaseFieldFilter;
import io.levelops.commons.databases.models.filters.TestRailsTestsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.testrails.models.CaseField;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.TestPlan;
import io.levelops.integrations.testrails.models.TestRailsTestCase;
import io.levelops.integrations.testrails.models.TestRun;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class TestRailsTestPlanDatabaseServiceTest {

    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private DataSource dataSource;
    private TestRailsProjectDatabaseService projectDatabaseService;
    private TestRailsTestPlanDatabaseService planDatabaseService;
    private TestRailsTestCaseDatabaseService testRailsTestCaseDatabaseService;
    @Mock
    private TestRailsCaseFieldDatabaseService caseFieldDatabaseService;
    private TestRailsCustomCaseFieldConditionsBuilder customFieldConditionsBuilder;
    private NamedParameterJdbcTemplate template;
    private Date currentTime;

    @Before
    public void setup() throws SQLException, IOException {
        MockitoAnnotations.initMocks(this);
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        template = new NamedParameterJdbcTemplate(dataSource);
        projectDatabaseService = new TestRailsProjectDatabaseService(dataSource);
        customFieldConditionsBuilder = new TestRailsCustomCaseFieldConditionsBuilder(caseFieldDatabaseService);
        planDatabaseService = new TestRailsTestPlanDatabaseService(DefaultObjectMapper.get(),dataSource, customFieldConditionsBuilder, caseFieldDatabaseService);
        testRailsTestCaseDatabaseService = new TestRailsTestCaseDatabaseService(DefaultObjectMapper.get(),dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("testrails")
                .name("testrails_test")
                .status("enabled")
                .build());
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
        projectDatabaseService.ensureTableExistence(COMPANY);
        planDatabaseService.ensureTableExistence(COMPANY);
        testRailsTestCaseDatabaseService.ensureTableExistence(COMPANY);
        final String testrailsProjects = ResourceUtils.getResourceAsString("json/databases/testrails-projects.json");
        final PaginatedResponse<Project> projects = OBJECT_MAPPER.readValue(testrailsProjects,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, Project.class));
        final String testPlans = ResourceUtils.getResourceAsString("json/databases/testrails-test-plans.json");
        final PaginatedResponse<TestPlan> plans = OBJECT_MAPPER.readValue(testPlans,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, TestPlan.class));
        final String testRuns = ResourceUtils.getResourceAsString("json/databases/testrails-test-runs.json");
        final PaginatedResponse<TestRun> runs = OBJECT_MAPPER.readValue(testRuns,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, TestRun.class));
        final List<DbTestRailsProject> dbTestRailsProjects = projects.getResponse().getRecords().stream()
                .map(project -> DbTestRailsProject.fromProject(project, INTEGRATION_ID))
                .collect(Collectors.toList());
        for (DbTestRailsProject dbTestRailsProject : dbTestRailsProjects) {
            final Optional<String> idOpt = projectDatabaseService.insertAndReturnId(COMPANY, dbTestRailsProject);
            if (idOpt.isEmpty()) {
                throw new RuntimeException("The project must exist: " + dbTestRailsProject);
            }
        }
        final List<DbTestRailsTestPlan> dbTestRailsTestPlans = plans.getResponse().getRecords().stream()
                .map(plan -> DbTestRailsTestPlan.fromTestPlan(plan, INTEGRATION_ID))
                .collect(Collectors.toList());
        for (DbTestRailsTestPlan dbTestRailsTestPlan : dbTestRailsTestPlans) {
            final Optional<String> idOpt = planDatabaseService.insertAndReturnId(COMPANY, dbTestRailsTestPlan);
            if (idOpt.isEmpty()) {
                throw new RuntimeException("The test plan must exist: " + dbTestRailsTestPlan);
            }
        }
        final List<DbTestRailsTestRun> dbTestRailsTestRuns = runs.getResponse().getRecords().stream()
                .map(run -> DbTestRailsTestRun.fromTestRun(run, INTEGRATION_ID, null))
                .collect(Collectors.toList());
        for (DbTestRailsTestRun dbTestRailsTestRun : dbTestRailsTestRuns) {
            final String id = planDatabaseService.insertTestRun(COMPANY, dbTestRailsTestRun);
            if (id.isEmpty()) {
                throw new RuntimeException("The test run must exist: " + dbTestRailsTestRun);
            }
        }
        final PaginatedResponse<TestRailsTestCase> testCases = ResourceUtils.getResourceAsObject("json/databases/testrails-test-cases.json",
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, TestRailsTestCase.class));
        final List<DbTestRailsTestCase> dbTestRailsTestCases = testCases.getResponse().getRecords().stream()
                .map(testCase -> DbTestRailsTestCase.fromTestCase(testCase, INTEGRATION_ID))
                .collect(Collectors.toList());
        testRailsTestCaseDatabaseService.insertBatch(COMPANY, dbTestRailsTestCases);
    }

    @Test
    public void testResultMapping() throws SQLException, IOException {
        final String testRuns = ResourceUtils.getResourceAsString("json/databases/testrails-test-runs-with-results.json");
        final PaginatedResponse<TestRun> runs = OBJECT_MAPPER.readValue(testRuns,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, TestRun.class));
        final List<DbTestRailsTestRun> dbTestRailsTestRuns = runs.getResponse().getRecords().stream()
                .map(run -> DbTestRailsTestRun.fromTestRun(run, INTEGRATION_ID, null))
                .collect(Collectors.toList());
        final String runId = planDatabaseService.insertTestRun(COMPANY, dbTestRailsTestRuns.get(0));
        String testSql = "SELECT * FROM " + COMPANY + ".testrails_tests where run_id in (:run_id)";

        Map<String, Integer> tests = template.query(testSql, Map.of("run_id", UUID.fromString(runId)), DbTestRailsConverters.listTestRowMapper(OBJECT_MAPPER)).stream().collect(Collectors.toMap(DbTestRailsTest::getId, DbTestRailsTest::getTestId));
        String testResultSql = "SELECT r.test_id as uuid, t.test_id as id  FROM " + COMPANY + ".testrails_tests t left join " + COMPANY + ".testrails_test_results r on r.test_id = t.id WHERE t.id in (:test_ids)";
        var testResultMapping = template.query(testResultSql, Map.of("test_ids", tests.keySet().stream().map(UUID::fromString).collect(Collectors.toList())), ((rs, rowCount) -> Map.of("uuid", rs.getString("uuid"), "id", rs.getInt("id")))).stream().collect(Collectors.toMap(m -> (String) m.get("uuid"), m -> (Integer) m.get("id")));
        for(var result: testResultMapping.entrySet()){
            String uuid = result.getKey();
            Assert.assertEquals(tests.get(uuid), result.getValue());
        }
        String deleteTestRun = "DELETE FROM " + COMPANY + ".testrails_test_runs WHERE id=:run_id";
        template.update(deleteTestRun, Map.of("run_id", UUID.fromString(runId)));
    }

    @Test
    public void testCaseFields() throws IOException, SQLException {
        final String testRuns = ResourceUtils.getResourceAsString("json/databases/testrails-test-runs-with-casefields.json");
        final PaginatedResponse<TestRun> runs = OBJECT_MAPPER.readValue(testRuns,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class,TestRun.class));
        final List<DbTestRailsTestRun> dbTestRailsTestRuns = runs.getResponse().getRecords().stream()
                .map(run -> DbTestRailsTestRun.fromTestRun(run, INTEGRATION_ID, null))
                .collect(Collectors.toList());
        final String runId = planDatabaseService.insertTestRun(COMPANY, dbTestRailsTestRuns.get(0));
        String testSql = "SELECT * FROM " + COMPANY + ".testrails_tests where run_id in (:run_id)";
        List<DbTestRailsTest> tests = template.query(testSql, Map.of("run_id", UUID.fromString(runId)), DbTestRailsConverters.listTestRowMapper(OBJECT_MAPPER));
        tests = tests.stream()
                .filter(test -> List.of(1019,1020,1021).contains(test.getTestId()))
                .collect(Collectors.toList());
        assertThat(tests.size()).isEqualTo(3);
        assertThat(tests.get(0).getCustomCaseFields().size()).isGreaterThan(0);
        assertThat(tests.get(0).getCustomCaseFields().containsKey("custom_automation_type")).isTrue();
        assertThat(tests.get(0).getCustomCaseFields().get("custom_automation_type") instanceof String).isTrue();

        assertThat(tests.get(0).getCustomCaseFields().containsKey("custom_url_field")).isTrue();
        assertThat(tests.get(0).getCustomCaseFields().get("custom_url_field") instanceof String).isTrue();

        assertThat(tests.get(0).getCustomCaseFields().containsKey("custom_user_field")).isTrue();
        assertThat(tests.get(0).getCustomCaseFields().get("custom_user_field") instanceof String).isTrue();

        assertThat(tests.get(0).getCustomCaseFields().containsKey("custom_integer_field")).isTrue();
        assertThat(tests.get(0).getCustomCaseFields().get("custom_integer_field") instanceof Integer).isTrue();

        assertThat(tests.get(0).getCustomCaseFields().containsKey("custom_date_field")).isTrue();
        assertThat(tests.get(0).getCustomCaseFields().get("custom_date_field") instanceof String).isTrue();

        assertThat(tests.get(0).getCustomCaseFields().containsKey("custom_checkout_field")).isTrue();
        assertThat(tests.get(0).getCustomCaseFields().get("custom_checkout_field") instanceof Boolean).isTrue();

        assertThat(tests.get(0).getCustomCaseFields().containsKey("custom_multi_select_field")).isTrue();
        assertThat(tests.get(0).getCustomCaseFields().get("custom_multi_select_field") instanceof List).isTrue();

        List<DbTestRailsTest> drilldownList = planDatabaseService.list(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .build(), Collections.emptyMap(), 0, 100).getRecords();
        var caseFieldsInserted = drilldownList.stream().map(DbTestRailsTest::getCustomCaseFields).filter(MapUtils::isNotEmpty).collect(Collectors.toList());
        assertThat(caseFieldsInserted.size()).isEqualTo(1);
        assertThat(caseFieldsInserted.get(0).keySet().size()).isEqualTo(11);

        // test filter values for custom case fields
        final String testrailsCaseFields = ResourceUtils.getResourceAsString("json/databases/testrails-case-fields.json");
        final PaginatedResponse<CaseField> caseFields = OBJECT_MAPPER.readValue(testrailsCaseFields,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, CaseField.class));
        final List<DbTestRailsCaseField> dbTestRailsCaseFields = caseFields.getResponse().getRecords().stream()
                .map(caseField -> DbTestRailsCaseField.fromCaseField(caseField, INTEGRATION_ID))
                .collect(Collectors.toList());
        when(caseFieldDatabaseService.listByFilter(anyString(), any(), anyInt(), anyInt())).thenReturn(DbListResponse.of(dbTestRailsCaseFields, dbTestRailsCaseFields.size()));
        DbTestRailsCaseField multiSelectField = dbTestRailsCaseFields.stream().filter(caseField -> caseField.getType() == "MULTI_SELECT").collect(Collectors.toList()).get(0);
        when(caseFieldDatabaseService.listByFilter(anyString(), eq(TestRailsCaseFieldFilter.builder().systemNames(List.of("custom_multi_select_field")).build()), anyInt(), anyInt())).thenReturn(DbListResponse.of(List.of(multiSelectField), 1));
        TestRailsTestsFilter filter = TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_count)
                .DISTINCT(TestRailsTestsFilter.DISTINCT.custom_case_field)
                .build();
        DbListResponse<DbAggregationResult> dbAggregationResult = planDatabaseService.groupByAndCalculateForValues(COMPANY, filter.toBuilder().customAcross("custom_url_field").build(), dbTestRailsCaseFields);
        Optional<Long> count = getSumOfCount(dbAggregationResult);
        assertThat(count).isPresent();
        assertThat(dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).count()).isEqualTo(1);
        assertThat(count.get()).isEqualTo(1);

        dbAggregationResult = planDatabaseService.groupByAndCalculateForValues(COMPANY, filter.toBuilder().customAcross("custom_multi_select_field").build(), dbTestRailsCaseFields);
        count = getSumOfCount(dbAggregationResult);
        assertThat(count).isPresent();
        assertThat(dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).count()).isEqualTo(2);
        assertThat(count.get()).isEqualTo(2);

        dbAggregationResult = planDatabaseService.groupByAndCalculateForValues(COMPANY, filter.toBuilder().customAcross("custom_integer_field").build(), dbTestRailsCaseFields);
        count = getSumOfCount(dbAggregationResult);
        assertThat(count).isPresent();
        assertThat(dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).count()).isEqualTo(1);
        assertThat(count.get()).isEqualTo(1);

        dbAggregationResult = planDatabaseService.groupByAndCalculateForValues(COMPANY, filter.toBuilder().CALCULATION(TestRailsTestsFilter.CALCULATION.test_case_count).customAcross("custom_integer_field").build(), dbTestRailsCaseFields);
        count = getSumOfCount(dbAggregationResult);
        assertThat(count).isPresent();
        assertThat(dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).count()).isEqualTo(4);
        assertThat(count.get()).isEqualTo(5);

        //test across and stacks for custom case fields
        dbAggregationResult = planDatabaseService.groupByAndCalculate(COMPANY, filter.toBuilder().customAcross("custom_automation_type").build(), List.of(), null);
        count = getSumOfCount(dbAggregationResult);
        assertThat(count).isPresent();
        assertThat(dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).count()).isEqualTo(1);
        assertThat(count.get()).isEqualTo(1);

        dbAggregationResult = planDatabaseService.groupByAndCalculate(COMPANY, filter.toBuilder().CALCULATION(TestRailsTestsFilter.CALCULATION.test_case_count).customAcross("custom_automation_type").build(), List.of(), null);
        count = getSumOfCount(dbAggregationResult);
        assertThat(count).isPresent();
        assertThat(dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).count()).isEqualTo(2);
        assertThat(count.get()).isEqualTo(14);

        dbAggregationResult = planDatabaseService.groupByAndCalculate(COMPANY, filter.toBuilder().customAcross("custom_multi_select_field").build(), List.of(), null);
        count = getSumOfCount(dbAggregationResult);
        assertThat(count).isPresent();
        assertThat(dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).count()).isEqualTo(2);
        assertThat(count.get()).isEqualTo(2);

        dbAggregationResult = planDatabaseService.groupByAndCalculate(COMPANY, filter.toBuilder().customAcross("custom_automation_type").customStacks(List.of("custom_integer_field")).build(), List.of(TestRailsTestsFilter.DISTINCT.custom_case_field), null);
        count = getSumOfCount(dbAggregationResult);
        List<DbAggregationResult> stacks = dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).map(DbAggregationResult::getStacks).collect(Collectors.toList()).get(0);
        assertThat(count).isPresent();
        assertThat(dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).count()).isEqualTo(1);
        assertThat(count.get()).isEqualTo(1);
        assertThat(stacks.size()).isEqualTo(1);
        for(var stack: stacks){
            assertThat(stack.getTotalTests()).isEqualTo(1);
        }

        dbAggregationResult = planDatabaseService.groupByAndCalculate(COMPANY, filter.toBuilder().CALCULATION(TestRailsTestsFilter.CALCULATION.test_case_count).customAcross("custom_automation_type").customStacks(List.of("custom_integer_field")).build(), List.of(TestRailsTestsFilter.DISTINCT.custom_case_field), null);
        count = getSumOfCount(dbAggregationResult);
        stacks = dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).map(DbAggregationResult::getStacks).collect(Collectors.toList()).get(0);
        assertThat(count).isPresent();
        assertThat(dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).count()).isEqualTo(2);
        assertThat(count.get()).isEqualTo(14);
        assertThat(stacks.size()).isEqualTo(3);

        //test across and stacks for custom case fields of type CHECKBOX
        List<DbTestRailsCaseField> checkboxField = dbTestRailsCaseFields.stream().filter(field -> "custom_checkbox_field".equals(field.getSystemName())).collect(Collectors.toList());
        when(caseFieldDatabaseService.listByFilter(anyString(), any(), anyInt(), anyInt())).thenReturn(DbListResponse.of(checkboxField, checkboxField.size()));
        dbAggregationResult = planDatabaseService.groupByAndCalculate(COMPANY, filter.toBuilder()
                .DISTINCT(TestRailsTestsFilter.DISTINCT.fromString("custom_checkbox_field"))
                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_case_count)
                .customAcross("custom_checkbox_field")
                .customStacks(List.of("custom_checkbox_field"))
                .build(), List.of(TestRailsTestsFilter.DISTINCT.custom_case_field), null);
        count = getSumOfCount(dbAggregationResult);
        stacks = dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).map(DbAggregationResult::getStacks).collect(Collectors.toList()).get(0);
        assertThat(count).isPresent();
        assertThat(dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).count()).isEqualTo(2);
        assertThat(count.get()).isEqualTo(14);
        assertThat(stacks.size()).isEqualTo(1);

        //test across and stacks for custom case fields of type CHECKBOX with filter of custom checkbox field
        dbAggregationResult = planDatabaseService.groupByAndCalculate(COMPANY, filter.toBuilder()
                .DISTINCT(TestRailsTestsFilter.DISTINCT.fromString("custom_checkbox_field"))
                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_case_count)
                .customCaseFields(Map.of("custom_checkbox_field", "true"))
                .customAcross("custom_checkbox_field")
                .customStacks(List.of("custom_checkbox_field"))
                .build(), List.of(TestRailsTestsFilter.DISTINCT.custom_case_field), null);
        count = getSumOfCount(dbAggregationResult);
        stacks = dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).map(DbAggregationResult::getStacks).collect(Collectors.toList()).get(0);
        assertThat(count).isPresent();
        assertThat(dbAggregationResult.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).count()).isEqualTo(1);
        assertThat(count.get()).isEqualTo(4);
        assertThat(stacks.size()).isEqualTo(1);
    }

    @Test
    public void testGroupByAndCalculateForTestcases() throws IOException {
        DbListResponse<DbAggregationResult> response = planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.project)
                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_case_count)
                .build(), null, null);
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getRecords().get(0).getTotalTests()).isEqualTo(14);
        response = planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.type)
                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_case_count)
                .build(), null, null);
        assertThat(response.getTotalCount()).isEqualTo(3);
        var totalTestCount = response.getRecords().stream().map(DbAggregationResult::getTotalTests).reduce((a,b) -> a + b);
        assertThat(totalTestCount).isNotEmpty();
        assertThat(totalTestCount.get()).isEqualTo(14);
        response = planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.trend)
                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_case_count)
                .build(), null, null);
        assertThat(response).isNotNull();
        assertThat(response.getRecords()).isNotEmpty();
        DbAggregationResult lastResult = response.getRecords().get(response.getRecords().size()-1);
        assertThat(lastResult.getTotalTests()).isEqualTo(14);
    }
    private Optional<Long> getSumOfCount(DbListResponse<DbAggregationResult> result){
        return result.getRecords().stream().filter(record -> Objects.nonNull(record.getKey())).map(DbAggregationResult::getTotalTests).reduce(Long::sum);
    }
    @Test
    public void test() throws SQLException {
        // test sorting
        assertThat(planDatabaseService.list(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .build(), Map.of("estimate", SortingOrder.DESC), 0, 100)
                .getRecords().get(0).getEstimate()).isEqualTo(300);
        //1691107200 1691193599
        assertThat(planDatabaseService.list(COMPANY, TestRailsTestsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .CALCULATION(TestRailsTestsFilter.CALCULATION.test_case_count)
                        .createdOnTimeRange(ImmutablePair.of(1690934400L,1691625600L))
                        .build(), Map.of("created_at", SortingOrder.DESC), 0, 100)
                .getRecords().size()).isEqualTo(14);
        //test list
        assertThat(planDatabaseService.list(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(49);
        assertThat(planDatabaseService.list(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .milestones(List.of("Release 1.0"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(4);
        assertThat(planDatabaseService.list(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of("2"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(0);
        assertThat(planDatabaseService.list(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .statuses(List.of("untested"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(49);
        assertThat(planDatabaseService.list(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .testPlans(List.of("UNASSIGNED"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(45);
        assertThat(planDatabaseService.list(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .milestones(List.of("UNASSIGNED"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(45);

        // test defects
        DbListResponse<DbTestRailsTest> data = planDatabaseService.list(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .build(), Collections.emptyMap(), 0, 100);
        List<String> defects = data.getRecords().stream().map(DbTestRailsTest::getDefects).filter(Objects::nonNull).flatMap(List::stream).collect(Collectors.toList());
        assertThat(data.getTotalCount()).isEqualTo(49);
        assertThat(defects.size()).isEqualTo(3);
        assertThat(defects).contains("DUM-1");
        assertThat(defects).contains("DUM-2");
        assertThat(defects).contains("DUM-3");

        // test aggregation
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.assignee)
                .build(), null, null).getTotalCount()).isEqualTo(2);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.project)
                .build(), null, null).getTotalCount()).isEqualTo(1);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.type)
                .build(), null, null).getTotalCount()).isEqualTo(12);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .acrossLimit(10)
                .DISTINCT(TestRailsTestsFilter.DISTINCT.type)
                .build(), null, null).getTotalCount()).isEqualTo(10);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.milestone)
                .build(), null, null).getTotalCount()).isEqualTo(2);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.test_run)
                .build(), null, null).getTotalCount()).isEqualTo(2);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .testPlans(List.of("UNASSIGNED"))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.test_run)
                .build(), null, null).getTotalCount()).isEqualTo(1);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .milestones(List.of("UNASSIGNED"))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.test_run)
                .build(), null, null).getTotalCount()).isEqualTo(1);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.test_run)
                .CALCULATION(TestRailsTestsFilter.CALCULATION.estimate)
                .build(), null, null).getRecords().stream()
                .map(DbAggregationResult::getMax)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(300);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .DISTINCT(TestRailsTestsFilter.DISTINCT.trend)
                        .CALCULATION(TestRailsTestsFilter.CALCULATION.estimate)
                        .build(), null, null).getRecords().stream()
                .map(DbAggregationResult::getMax)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(300);
    }

    @Test
    public void testValues(){
        assertThat(planDatabaseService.groupByAndCalculateForValues(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.project)
                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_count)
                .build(), List.of()).getTotalCount()).isEqualTo(2);
        assertThat(planDatabaseService.groupByAndCalculateForValues(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.milestone)
                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_count)
                .build(), List.of()).getTotalCount()).isEqualTo(4);
        assertThat(planDatabaseService.groupByAndCalculateForValues(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.test_run)
                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_count)
                .build(), List.of()).getTotalCount()).isEqualTo(3);
        assertThat(planDatabaseService.groupByAndCalculateForValues(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.test_plan)
                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_count)
                .build(), List.of()).getTotalCount()).isEqualTo(2);
        assertThat(planDatabaseService.groupByAndCalculateForValues(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.assignee)
                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_count)
                .build(), List.of()).getTotalCount()).isEqualTo(2);
        assertThat(planDatabaseService.groupByAndCalculateForValues(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.type)
                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_count)
                .build(), List.of()).getTotalCount()).isEqualTo(12);
    }
    @Test
    public void testValuesForTestCase(){
        assertThat(planDatabaseService.groupByAndCalculateForValues(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.project)
                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_case_count)
                .build(), List.of()).getTotalCount()).isEqualTo(2);
        assertThat(planDatabaseService.groupByAndCalculateForValues(COMPANY, TestRailsTestsFilter.builder()
                .integrationIds(List.of(INTEGRATION_ID))
                .DISTINCT(TestRailsTestsFilter.DISTINCT.type)
                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_case_count)
                .build(), List.of()).getTotalCount()).isEqualTo(3);
    }

    @Test
    public void testStacking() throws SQLException {
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .DISTINCT(TestRailsTestsFilter.DISTINCT.assignee)
                        .build(),
                List.of(TestRailsTestsFilter.DISTINCT.assignee), null)
                .getTotalCount()).isEqualTo(2);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .DISTINCT(TestRailsTestsFilter.DISTINCT.project)
                        .build(),
                List.of(TestRailsTestsFilter.DISTINCT.project), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                                .integrationIds(List.of(INTEGRATION_ID))
                                .DISTINCT(TestRailsTestsFilter.DISTINCT.project)
                                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_case_count)
                                .build(),
                        List.of(TestRailsTestsFilter.DISTINCT.project), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .DISTINCT(TestRailsTestsFilter.DISTINCT.milestone)
                        .build(),
                List.of(TestRailsTestsFilter.DISTINCT.milestone), null)
                .getTotalCount()).isEqualTo(2);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .DISTINCT(TestRailsTestsFilter.DISTINCT.test_run)
                        .build(),
                List.of(TestRailsTestsFilter.DISTINCT.test_run), null)
                .getTotalCount()).isEqualTo(2);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .DISTINCT(TestRailsTestsFilter.DISTINCT.status)
                        .build(),
                List.of(TestRailsTestsFilter.DISTINCT.status), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .DISTINCT(TestRailsTestsFilter.DISTINCT.priority)
                        .build(),
                List.of(TestRailsTestsFilter.DISTINCT.priority), null)
                .getTotalCount()).isEqualTo(5);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                                .integrationIds(List.of(INTEGRATION_ID))
                                .DISTINCT(TestRailsTestsFilter.DISTINCT.priority)
                                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_case_count)
                                .build(),
                        List.of(TestRailsTestsFilter.DISTINCT.priority), null)
                .getTotalCount()).isEqualTo(3);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .DISTINCT(TestRailsTestsFilter.DISTINCT.test_run)
                        .CALCULATION(TestRailsTestsFilter.CALCULATION.estimate)
                        .build(),
                List.of(TestRailsTestsFilter.DISTINCT.test_run), null)
                .getRecords().stream()
                .map(DbAggregationResult::getMax)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseThrow()).isEqualTo(300);
        assertThat(planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .DISTINCT(TestRailsTestsFilter.DISTINCT.type)
                        .build(),
                List.of(TestRailsTestsFilter.DISTINCT.milestone), null)
                .getTotalCount()).isEqualTo(12);
        var response = planDatabaseService.groupByAndCalculate(COMPANY, TestRailsTestsFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .DISTINCT(TestRailsTestsFilter.DISTINCT.type)
                        .CALCULATION(TestRailsTestsFilter.CALCULATION.test_case_count)
                        .build(),
                List.of(TestRailsTestsFilter.DISTINCT.priority), null);
        assertThat(response.getTotalCount()).isEqualTo(3);
        assertThat(getSumOfCount(response)).isNotEmpty();
        assertThat(getSumOfCount(response).get()).isEqualTo(14);
    }

    @Test
    public void testNeedTables(){
        // test project table
        Assert.assertTrue(TestRailsTestPlanDatabaseService.needProjectTable(TestRailsTestsFilter.builder().DISTINCT(TestRailsTestsFilter.DISTINCT.project).build(), TestRailsTestsFilter.DISTINCT.project, null));
        Assert.assertTrue(TestRailsTestPlanDatabaseService.needProjectTable(TestRailsTestsFilter.builder().projects(List.of("project-1")).build(), null, null));
        Assert.assertFalse(TestRailsTestPlanDatabaseService.needProjectTable(TestRailsTestsFilter.builder().DISTINCT(TestRailsTestsFilter.DISTINCT.test_plan).testPlans(List.of("test-plan-1")).build(), TestRailsTestsFilter.DISTINCT.test_plan, null));

        // test test-plan table
        Assert.assertTrue(TestRailsTestPlanDatabaseService.needTestPlanTable(TestRailsTestsFilter.builder().DISTINCT(TestRailsTestsFilter.DISTINCT.test_plan).build(), TestRailsTestsFilter.DISTINCT.test_plan, null));
        Assert.assertTrue(TestRailsTestPlanDatabaseService.needTestPlanTable(TestRailsTestsFilter.builder().testPlans(List.of("test-plan-1")).build(), null, null));
        Assert.assertFalse(TestRailsTestPlanDatabaseService.needTestPlanTable(TestRailsTestsFilter.builder().DISTINCT(TestRailsTestsFilter.DISTINCT.project).projects(List.of("project-1")).build(), TestRailsTestsFilter.DISTINCT.project, null));

        // test milestone table
        Assert.assertTrue(TestRailsTestPlanDatabaseService.needMilestoneTable(TestRailsTestsFilter.builder().DISTINCT(TestRailsTestsFilter.DISTINCT.milestone).build(), TestRailsTestsFilter.DISTINCT.milestone, null));
        Assert.assertTrue(TestRailsTestPlanDatabaseService.needMilestoneTable(TestRailsTestsFilter.builder().milestones(List.of("milestone-1")).build(), null, null));
        Assert.assertFalse(TestRailsTestPlanDatabaseService.needMilestoneTable(TestRailsTestsFilter.builder().DISTINCT(TestRailsTestsFilter.DISTINCT.project).projects(List.of("project-1")).build(), TestRailsTestsFilter.DISTINCT.project, null));

        // test test-run table
        Assert.assertTrue(TestRailsTestPlanDatabaseService.needTestRunTable(TestRailsTestsFilter.builder().DISTINCT(TestRailsTestsFilter.DISTINCT.test_run).build(), TestRailsTestsFilter.DISTINCT.test_run, null));
        Assert.assertTrue(TestRailsTestPlanDatabaseService.needTestRunTable(TestRailsTestsFilter.builder().testRuns(List.of("test-run-1")).build(), null, null));
        Assert.assertFalse(TestRailsTestPlanDatabaseService.needTestRunTable(TestRailsTestsFilter.builder().DISTINCT(TestRailsTestsFilter.DISTINCT.project).projects(List.of("project-1")).build(), TestRailsTestsFilter.DISTINCT.project, null));
    }
}

