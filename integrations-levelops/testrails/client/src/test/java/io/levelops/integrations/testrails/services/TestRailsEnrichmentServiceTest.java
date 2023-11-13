package io.levelops.integrations.testrails.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.testrails.client.TestRailsClient;
import io.levelops.integrations.testrails.client.TestRailsClientException;
import io.levelops.integrations.testrails.models.CaseField;
import io.levelops.integrations.testrails.models.Milestone;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.TestPlan;
import io.levelops.integrations.testrails.models.TestRailsTestCase;
import io.levelops.integrations.testrails.models.TestRailsTestSuite;
import io.levelops.integrations.testrails.models.TestRun;
import io.levelops.integrations.testrails.models.User;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class TestRailsEnrichmentServiceTest {

    private TestRailsClient testRailsClient;
    private TestRailsEnrichmentService enrichmentService;
    private List<TestRailsTestCase> testCasesList1;
    private List<TestRailsTestCase> testCasesList2;
    private List<TestRailsTestSuite> testSuites;
    private Date currentTime;

    @Before
    public void setup() throws TestRailsClientException, JsonProcessingException {
        testRailsClient = Mockito.mock(TestRailsClient.class);
        enrichmentService = new TestRailsEnrichmentService(2, 1);
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);

        when(testRailsClient.getMilestones(anyInt()))
                .thenReturn(Stream.of(Milestone.builder().build()));
        TestRailsTestCase testCase1 = TestRailsTestCase.builder()
                .id(1)
                .title("Test Case 1: TestSuite-1")
                .createdBy(1)
                .updatedBy(1)
                .priorityId(1)
                .typeId(1)
                .suiteId(1)
                .build();
        TestRailsTestCase testCase2 = TestRailsTestCase.builder()
                .id(2)
                .title("Test Case 2: TestSuite-1")
                .createdBy(1)
                .updatedBy(1)
                .priorityId(1)
                .typeId(1)
                .suiteId(1)
                .build();
        testCase1.setDynamicCustomFields("custom_user_field", 1);
        testCase1.setDynamicCustomFields("custom_automation_type", 0);
        testCase1.setDynamicCustomFields("custom_multiselect_field", List.of(1,2));
        testCase1.setDynamicCustomFields("custom_dropdown_field", 0);
        testCase2.setDynamicCustomFields("custom_user_field", 0);
        testCase2.setDynamicCustomFields("custom_automation_type", 1);
        testCase2.setDynamicCustomFields("custom_multiselect_field", List.of(1,3));
        TestRailsTestCase testcase1 = TestRailsTestCase.builder()
                .id(1)
                .title("Test Case 1: TestSuite-1")
                .createdBy(1)
                .updatedBy(1)
                .priorityId(1)
                .typeId(1)
                .suiteId(1)
                .build();
        TestRailsTestCase testcase2 = TestRailsTestCase.builder()
                .id(2)
                .title("Test Case 2: TestSuite-1")
                .createdBy(1)
                .updatedBy(1)
                .priorityId(1)
                .typeId(1)
                .suiteId(1)
                .build();
        testcase1.setDynamicCustomFields("custom_user_field", 1);
        testcase1.setDynamicCustomFields("custom_automation_type", 0);
        testcase1.setDynamicCustomFields("custom_multiselect_field", List.of(1,2));
        testcase2.setDynamicCustomFields("custom_user_field", 0);
        testcase2.setDynamicCustomFields("custom_automation_type", 1);
        testcase2.setDynamicCustomFields("custom_multiselect_field", List.of(1,3));
        testCasesList1 = List.of(testCase1,testCase2);
        testCasesList2 = List.of(testcase1,testcase2);
        testSuites = List.of(TestRailsTestSuite.builder()
                        .id(1)
                        .name("Master")
                        .projectId(1)
                        .isMaster(true)
                        .build(),
                TestRailsTestSuite.builder()
                        .id(2)
                        .name("TestSuite-1")
                        .projectId(1)
                        .isMaster(true)
                        .build());
        when(testRailsClient.getTestCases(anyInt(), eq(1)))
                .thenReturn(testCasesList1.stream());
        when(testRailsClient.getTestCases(anyInt(), eq(2)))
                .thenReturn(testCasesList2.stream());
        when(testRailsClient.getTestSuites(anyInt()))
                .thenReturn(testSuites);
        when(testRailsClient.getPlan(eq(2)))
                .thenReturn(TestPlan.builder()
                        .entries(Arrays.array(
                                TestPlan.Entry.builder()
                                        .runs(Arrays.array(
                                                TestRun.builder()
                                                        .id(1)
                                                        .projectId(1)
                                                        .build()))
                                        .build()))
                        .build());
        io.levelops.integrations.testrails.models.Test test1 = io.levelops.integrations.testrails.models.Test.builder()
                        .id(1)
                        .assignedToId(1)
                        .runId(1)
                        .caseId(1)
                        .priorityId(1)
                        .typeId(1)
                        .statusId(1)
                        .build();
        test1.setDynamicCustomFields("custom_multiselect_field", List.of(2));
        test1.setDynamicCustomFields("custom_automation_type", 1);
        test1.setDynamicCustomFields("custom_user_field", 1);
        io.levelops.integrations.testrails.models.Test test2 = io.levelops.integrations.testrails.models.Test.builder()
                .id(1)
                .assignedToId(0)
                .runId(1)
                .caseId(1)
                .priorityId(1)
                .typeId(1)
                .statusId(1)
                .build();
        test2.setDynamicCustomFields("custom_multiselect_field", List.of(2, 3));
        test2.setDynamicCustomFields("custom_automation_type", 0);
        test2.setDynamicCustomFields("custom_user_field", 0);
        when(testRailsClient.getTests(eq(1)))
                .thenReturn(Stream.of(test1, test2));
        when(testRailsClient.getUsersByProjectId(anyInt()))
                .thenReturn(List.of(User.builder().id(0).email("testUser@test.com").build(), User.builder().id(1).name("testUser").build(),User.builder().id(2).role("role-1").build(),User.builder().id(2).role("role-2").build()));
        when(testRailsClient.getStatuses())
                .thenReturn(List.of(io.levelops.integrations.testrails.models.Test.Status.builder()
                        .id(1)
                        .name("passed")
                        .label("Passed")
                        .build()));
        when(testRailsClient.getCaseTypes())
                .thenReturn(List.of(io.levelops.integrations.testrails.models.Test.CaseType.builder()
                        .id(1)
                        .name("functional")
                        .build()));
        when(testRailsClient.getPriorities())
                .thenReturn(List.of(io.levelops.integrations.testrails.models.Test.Priority.builder()
                        .id(1)
                        .name("high")
                        .build()));
        when(testRailsClient.getResults(anyInt()))
                .thenReturn(Stream.of(io.levelops.integrations.testrails.models.Test.Result.builder()
                                .id(1L)
                                .testId(1)
                                .statusId(1)
                                .assignedToId(1)
                                .createdBy(1)
                                .createdOn(currentTime.toInstant().getEpochSecond())
                                .comment("comment")
                                .defects("DF-1,DF-2")
                                .build(),
                        io.levelops.integrations.testrails.models.Test.Result.builder()
                                .id(1L)
                                .testId(1)
                                .assignedToId(1)
                                .createdBy(1)
                                .createdOn(currentTime.toInstant().getEpochSecond())
                                .build()));
        when(testRailsClient.getCaseFields())
                .thenReturn(List.of(
                        CaseField.builder()
                                .id(22L)
                                .isActive(true)
                                .typeId(12)
                                .systemName("custom_multiselect_field")
                                .configs(List.of(
                                            CaseField.FieldConfig.builder()
                                                    .context(CaseField.FieldConfig.ConfigContext.builder()
                                                            .isGlobal(false)
                                                            .projectIds(List.of(1))
                                                            .build())
                                                    .options(CaseField.FieldConfig.ConfigOption.builder()
                                                            .items("1, item1\n2, item2\n3, item3")
                                                            .build())
                                                    .build(),
                                            CaseField.FieldConfig.builder()
                                                    .context(CaseField.FieldConfig.ConfigContext.builder()
                                                            .isGlobal(false)
                                                            .projectIds(List.of(2))
                                                            .build())
                                                    .options(CaseField.FieldConfig.ConfigOption.builder()
                                                            .items("2, element2\n3, element3")
                                                            .build())
                                                    .build()
                                        ))
                                .build(),
                        CaseField.builder()
                                .id(27L)
                                .isActive(true)
                                .typeId(6)
                                .systemName("custom_automation_type")
                                .configs(List.of(
                                        CaseField.FieldConfig.builder()
                                                .context(CaseField.FieldConfig.ConfigContext.builder()
                                                        .isGlobal(true)
                                                        .build())
                                                .options(CaseField.FieldConfig.ConfigOption.builder()
                                                        .items("0, None\n1, Ranorex")
                                                        .build())
                                                .build()))
                                .build(),
                        CaseField.builder()
                                .id(21L)
                                .isActive(true)
                                .typeId(7).systemName("custom_user_field")
                                .build(),
                        CaseField.builder()
                                .id(10L)
                                .isActive(true)
                                .typeId(6)
                                .systemName("custom_dropdown_field")
                                .configs(List.of(
                                        CaseField.FieldConfig.builder()
                                                .context(CaseField.FieldConfig.ConfigContext.builder()
                                                        .isGlobal(true)
                                                        .projectIds(List.of(2))
                                                        .build())
                                                .options(CaseField.FieldConfig.ConfigOption.builder()
                                                        .items("0, no\n1, yes")
                                                        .build())
                                                .build()))
                                .build())
                );
    }

    @Test
    public void enrichProject() {
        List<Project> projects = enrichmentService.enrichProjects(testRailsClient,
                IntegrationKey.builder().build(), List.of(Project.builder().id(1).build()));
        assertThat(projects).isNotNull();
        assertThat(projects).hasSize(1);
        Project project = projects.get(0);
        assertThat(project.getMilestones()).hasSize(1);
    }

    @Test
    public void enrichTestPlan() throws TestRailsClientException {
        List<TestPlan> testPlans = enrichmentService.enrichTestPlans(testRailsClient,
                IntegrationKey.builder().build(), List.of(TestPlan.builder().id(2)
                        .entries(Arrays.array(TestPlan.Entry.builder().build())).build()), 1);
        assertThat(testPlans).isNotNull();
        assertThat(testPlans).hasSize(1);
        TestPlan testPlan = testPlans.get(0);
        assertThat(testPlan.getEntries()).hasSize(1);
        List<TestRun> testRuns = testPlan.getTestRuns();
        assertThat(testRuns.get(0).getTests()).hasSize(2);
        assertThat(testRuns.get(0).getTests().get(0).getPriority()).isNotNull();
        assertThat(testRuns.get(0).getTests().get(0).getStatus()).isNotNull();
        assertThat(testRuns.get(0).getTests().get(0).getAssignee()).isNotNull();
        assertThat(testRuns.get(0).getTests().get(0).getType()).isNotNull();
        assertThat(testRuns.get(0).getTests().get(0).getCreatedOn()).isNotNull();
        assertThat(testRuns.get(0).getTests().get(0).getResults().get(0).getTestId()).isNotNull();
        assertThat(testRuns.get(0).getTests().get(0).getResults().get(0).getComment()).isNotNull();
        assertThat(testRuns.get(0).getTests().get(0).getResults().get(0).getDefects()).isNotEmpty();
        assertThat(testRuns.get(0).getTests().get(0).getResults().get(0).getStatus()).isNotNull();
        assertThat(testRuns.get(0).getTests().get(0).getResults().get(0).getAssignee()).isNotNull();
        assertThat(testRuns.get(0).getTests().get(0).getResults().get(0).getCreator()).isNotNull();
        assertThat(testRuns.get(0).getTests().get(0).getResults().get(1).getStatus()).isNull();
    }

    @Test
    public void enrichTestRun() throws TestRailsClientException {
        List<TestRun> testRuns = enrichmentService.enrichTestRuns(testRailsClient,
                IntegrationKey.builder().build(), List.of(TestRun.builder().id(1).projectId(1).build()), 1);
        assertThat(testRuns).isNotNull();
        assertThat(testRuns).hasSize(1);
        TestRun testRun = testRuns.get(0);
        assertThat(testRun.getTests()).hasSize(2);
        assertThat(testRun.getTests().get(0).getPriority()).isNotNull();
        assertThat(testRun.getTests().get(0).getStatus()).isNotNull();
        assertThat(testRun.getTests().get(0).getAssignee()).isNotNull();
        assertThat(testRun.getTests().get(0).getType()).isNotNull();
        assertThat(testRun.getTests().get(0).getCreatedOn()).isNotNull();
        assertThat(testRun.getTests().get(0).getResults().get(0).getTestId()).isNotNull();
        assertThat(testRun.getTests().get(0).getResults().get(0).getComment()).isNotNull();
        assertThat(testRun.getTests().get(0).getResults().get(0).getDefects()).isNotEmpty();
        assertThat(testRun.getTests().get(0).getResults().get(0).getStatus()).isNotNull();
        assertThat(testRun.getTests().get(0).getResults().get(0).getAssignee()).isNotNull();
        assertThat(testRun.getTests().get(0).getResults().get(0).getCreator()).isNotNull();
        assertThat(testRuns.get(0).getTests().get(0).getResults().get(1).getStatus()).isNull();
    }

    @Test
    public void enrichTestRunsWithCustomCaseFields() throws TestRailsClientException {
        List<TestRun> testRuns = enrichmentService.enrichTestRuns(testRailsClient,
                IntegrationKey.builder().build(), List.of(TestRun.builder().id(1).projectId(1).build()), 1);
        TestRun testRun = testRuns.get(0);
        assertThat(testRun.getTests()).hasSize(2);
        assertThat(testRun.getTests().get(0).getCustomCaseFields()).hasSize(3);
        assertThat(testRun.getTests().get(0).getCustomCaseFields().get("custom_multiselect_field")).isEqualTo(List.of("item2"));
        assertThat(testRun.getTests().get(0).getCustomCaseFields().get("custom_user_field")).isEqualTo("_UNASSIGNED_");
        assertThat(testRun.getTests().get(0).getCustomCaseFields().get("custom_automation_type")).isEqualTo("Ranorex");
        assertThat(testRun.getTests().get(1).getCustomCaseFields().get("custom_multiselect_field")).isEqualTo(List.of("item2", "item3"));
        assertThat(testRun.getTests().get(1).getCustomCaseFields().get("custom_user_field")).isEqualTo("testUser@test.com");
        assertThat(testRun.getTests().get(1).getCustomCaseFields().get("custom_automation_type")).isEqualTo("None");
    }

    @Test
    public void enrichTestRunsWithCustomCaseFieldsWithDiffProjectId() throws TestRailsClientException {
        List<TestRun> testRuns = enrichmentService.enrichTestRuns(testRailsClient,
                IntegrationKey.builder().build(), List.of(TestRun.builder().id(1).projectId(2).build()), 1);
        TestRun testRun = testRuns.get(0);
        assertThat(testRun.getTests()).hasSize(2);
        assertThat(testRun.getTests().get(0).getCustomCaseFields()).hasSize(3);
        assertThat(testRun.getTests().get(0).getCustomCaseFields().get("custom_multiselect_field")).isEqualTo(List.of("element2"));
        assertThat(testRun.getTests().get(0).getCustomCaseFields().get("custom_user_field")).isEqualTo("_UNASSIGNED_");
        assertThat(testRun.getTests().get(0).getCustomCaseFields().get("custom_automation_type")).isEqualTo("Ranorex");
        assertThat(testRun.getTests().get(1).getCustomCaseFields().get("custom_multiselect_field")).isEqualTo(List.of("element2", "element3"));
        assertThat(testRun.getTests().get(1).getCustomCaseFields().get("custom_user_field")).isEqualTo("testUser@test.com");
        assertThat(testRun.getTests().get(1).getCustomCaseFields().get("custom_automation_type")).isEqualTo("None");
    }

    @Test
    public void enrichTestCase() throws TestRailsClientException {
        List<TestRailsTestSuite> testSuiteList = enrichmentService.enrichTestSuites(testRailsClient,
                IntegrationKey.builder().build(), testSuites, 1);

        // Testing the forked task & sub-task creation
        assertThat(testSuiteList).isNotNull();
        assertThat(testSuiteList).hasSize(2);

        // Testing Sanitization of CustomCaseFields
        TestRailsTestSuite testSuite = testSuiteList.get(0);
        Assert.assertNotNull(testSuite.getTestCases());
        Assert.assertTrue(testSuite.getTestCases().stream().allMatch(testCase -> testCase.getCustomCaseFields() != null));
        Assert.assertEquals(List.of("item1", "item2"), testSuite.getTestCases().get(0)
                .getCustomCaseFields().get("custom_multiselect_field"));
        Assert.assertEquals("None", testSuite.getTestCases().get(0).getCustomCaseFields().get("custom_automation_type"));
        Assert.assertEquals("_UNASSIGNED_", testSuite.getTestCases().get(0).getCustomCaseFields().get("custom_user_field"));
        Assert.assertEquals(List.of("item1", "item3"), testSuite.getTestCases().get(1)
                .getCustomCaseFields().get("custom_multiselect_field"));
        Assert.assertEquals("Ranorex", testSuite.getTestCases().get(1).getCustomCaseFields().get("custom_automation_type"));
        Assert.assertEquals("testUser@test.com", testSuite.getTestCases().get(1).getCustomCaseFields().get("custom_user_field"));

        // TestCase1: suiteId-1, projectId-1 (Context: isGlobal- true & ProjectId - 2)
        Assert.assertEquals("no", testSuite.getTestCases().get(0).getCustomCaseFields().get("custom_dropdown_field"));
    }
}
