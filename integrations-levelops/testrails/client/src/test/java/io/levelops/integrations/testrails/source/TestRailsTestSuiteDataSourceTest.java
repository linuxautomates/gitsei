package io.levelops.integrations.testrails.source;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.testrails.client.TestRailsClient;
import io.levelops.integrations.testrails.client.TestRailsClientException;
import io.levelops.integrations.testrails.client.TestRailsClientFactory;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.TestRailsQuery;
import io.levelops.integrations.testrails.models.TestRailsTestCase;
import io.levelops.integrations.testrails.models.TestRailsTestSuite;
import io.levelops.integrations.testrails.models.User;
import io.levelops.integrations.testrails.services.TestRailsEnrichmentService;
import io.levelops.integrations.testrails.sources.TestRailsTestSuiteDataSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class TestRailsTestSuiteDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    TestRailsTestSuiteDataSource dataSource;
    private List<TestRailsTestCase> testCases;
    private List<TestRailsTestSuite> testSuites;

    @Before
    public void setup() throws TestRailsClientException {
        TestRailsClient client = Mockito.mock(TestRailsClient.class);
        TestRailsClientFactory clientFactory = Mockito.mock(TestRailsClientFactory.class);

        TestRailsEnrichmentService enrichmentService = new TestRailsEnrichmentService(1, 10);
        dataSource = new TestRailsTestSuiteDataSource(clientFactory, enrichmentService);
        when(clientFactory.get(ArgumentMatchers.eq(TEST_KEY))).thenReturn(client);
        List<Project> projects = List.of(Project.builder()
                .id(1)
                .build());
        when(client.getProjects()).thenReturn(projects.stream());
        when(client.getMilestones(anyInt())).thenReturn(Stream.empty());
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
        testCase2.setDynamicCustomFields("custom_user_field", 0);
        testCase2.setDynamicCustomFields("custom_automation_type", 1);
        testCase2.setDynamicCustomFields("custom_multiselect_field", List.of(1,3));
        testCases = List.of(testCase1,testCase2);
        testSuites = List.of(TestRailsTestSuite.builder()
                .id(1)
                .name("Master")
                .projectId(1)
                .isMaster(true)
                .build());
        when(client.getTestCases(anyInt(), anyInt()))
                .thenReturn(testCases.stream());
        when(client.getTestSuites(anyInt()))
                .thenReturn(testSuites);
        when(client.getPriorities())
                .thenReturn(List.of(io.levelops.integrations.testrails.models.Test.Priority.builder()
                        .id(1)
                        .name("high")
                        .build()));
        when(client.getCaseTypes())
                .thenReturn(List.of(io.levelops.integrations.testrails.models.Test.CaseType.builder()
                        .id(1)
                        .name("functional")
                        .build()));
        when(client.getUsersByProjectId(anyInt()))
                .thenReturn(List.of(User.builder().id(0).email("testUser@test.com").build(), User.builder().id(1).name("testUser").build(),User.builder().id(2).role("role-1").build(),User.builder().id(2).role("role-2").build()));
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(TestRailsQuery.builder().integrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<TestRailsTestSuite>> testSuites = dataSource.fetchMany(TestRailsQuery.builder()
                        .integrationKey(TEST_KEY)
                        .from(null)
                        .shouldFetchUsers(true)
                        .build())
                .collect(Collectors.toList());
        assertThat(testSuites).hasSize(1);
        assertThat(testSuites.get(0).getPayload().getTestCases()).hasSize(2);
    }
}

