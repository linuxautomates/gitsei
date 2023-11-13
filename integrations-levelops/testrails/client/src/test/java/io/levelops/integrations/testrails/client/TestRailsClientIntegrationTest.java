package io.levelops.integrations.testrails.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.testrails.models.Milestone;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.TestPlan;
import io.levelops.integrations.testrails.models.TestRailsTestCase;
import io.levelops.integrations.testrails.models.TestRailsTestSuite;
import io.levelops.integrations.testrails.models.TestRun;
import io.levelops.integrations.testrails.models.User;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRailsClientIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "testrails";
    private static final String APPLICATION = "testrails";
    private static final String TESTRAILS_URL = System.getenv("TESTRAILS_URL");
    private static final String TESTRAILS_USERNAME = System.getenv("TESTRAILS_USERNAME");
    private static final String TESTRAILS_PASSWORD = System.getenv("TESTRAILS_PASSWORD");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private TestRailsClientFactory clientFactory;

    @Before
    public void setup() {
        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory
                .builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, TESTRAILS_URL, Collections.emptyMap(),
                        TESTRAILS_USERNAME, TESTRAILS_PASSWORD)
                .build());
        clientFactory = TestRailsClientFactory.builder()
                .okHttpClient(client)
                .objectMapper(DefaultObjectMapper.get())
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void testGetProjects() throws TestRailsClientException {
        List<Project> response = clientFactory.get(TEST_INTEGRATION_KEY).getProjects().collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
        if (response.size() != 0) {
            Integer projectId = response.get(0).getId();
            testGetUsersByProjectId(projectId);
            testGetMilestones(projectId);
            testGetTestPlans(projectId);
            testGetTestRuns(projectId);
            testGetTestSuites(projectId);
        }
    }

    public void testGetUsersByProjectId(Integer projectId) throws TestRailsClientException {
        List<User> response = clientFactory.get(TEST_INTEGRATION_KEY).getUsersByProjectId(projectId);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }

    public void testGetMilestones(Integer projectId) throws TestRailsClientException {
        List<Milestone> response = clientFactory.get(TEST_INTEGRATION_KEY).getMilestones(projectId)
                .collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }

    public void testGetTestPlans(Integer projectId) throws TestRailsClientException {
        List<TestPlan> response = clientFactory.get(TEST_INTEGRATION_KEY).getTestPlans(projectId, 1, 0);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
        if (response.size() != 0) {
            testGetTestPlan(response.get(0).getId());
        }
    }

    public void testGetTestPlan(Integer planId) throws TestRailsClientException {
        TestPlan response = clientFactory.get(TEST_INTEGRATION_KEY).getPlan(planId);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }

    public void testGetTestRuns(Integer projectId) throws TestRailsClientException {
        List<TestRun> response = clientFactory.get(TEST_INTEGRATION_KEY).getTestRuns(projectId, 0, 1);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
        if (response.size() != 0) {
            testGetTests(response.get(0).getId());
        }
    }

    public void testGetTests(Integer runId) throws TestRailsClientException {
        List<io.levelops.integrations.testrails.models.Test> response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getTests(runId).collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
        if (response.size() != 0) {
            testGetTestResults(response.get(0).getId());
        }
    }

    public void testGetTestSuites(Integer projectId) throws TestRailsClientException {
        List<TestRailsTestSuite> response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getTestSuites(projectId);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
        if(response.size() != 0) {
            testGetTestCases(projectId, response.get(0).getId());
        }
    }

    public void testGetTestCases(Integer projectId, Integer suiteId) throws TestRailsClientException {
        List<TestRailsTestCase> response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getTestCases(projectId, suiteId).collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }

    public void testGetTestResults(Integer testId) throws TestRailsClientException {
        List<io.levelops.integrations.testrails.models.Test.Result> response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getResults(testId).collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }

    @Test
    public void testGetStatuses() throws TestRailsClientException {
        List<io.levelops.integrations.testrails.models.Test.Status> response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getStatuses();
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }

    @Test
    public void testGetCaseTypes() throws TestRailsClientException {
        List<io.levelops.integrations.testrails.models.Test.CaseType> response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getCaseTypes();
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }

    @Test
    public void testGetPriorities() throws TestRailsClientException {
        List<io.levelops.integrations.testrails.models.Test.Priority> response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getPriorities();
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }

    @Test
    public void testGetCaseFields() throws TestRailsClientException {
        List<io.levelops.integrations.testrails.models.CaseField> response = clientFactory.get(TEST_INTEGRATION_KEY)
                .getCaseFields();
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }
}