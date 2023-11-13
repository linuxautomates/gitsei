package io.levelops.integrations.testrails.source;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.testrails.client.TestRailsClient;
import io.levelops.integrations.testrails.client.TestRailsClientException;
import io.levelops.integrations.testrails.client.TestRailsClientFactory;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.TestPlan;
import io.levelops.integrations.testrails.models.TestRailsQuery;
import io.levelops.integrations.testrails.models.User;
import io.levelops.integrations.testrails.services.TestRailsEnrichmentService;
import io.levelops.integrations.testrails.sources.TestRailsTestPlanDataSource;
import org.assertj.core.util.Arrays;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Ignore
public class TestRailsTestPlanDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    TestRailsTestPlanDataSource dataSource;

    @Before
    public void setup() throws TestRailsClientException {
        TestRailsClient client = Mockito.mock(TestRailsClient.class);
        TestRailsClientFactory clientFactory = Mockito.mock(TestRailsClientFactory.class);
        TestRailsEnrichmentService enrichmentService = new TestRailsEnrichmentService(1, 10);
        dataSource = new TestRailsTestPlanDataSource(clientFactory, enrichmentService);
        when(clientFactory.get(TEST_KEY)).thenReturn(client);
        List<TestPlan> testPlans = List.of(TestPlan.builder().id(6).build());
        when(client.getTestPlans(eq(1), anyInt(), eq(0)))
                .thenReturn(testPlans);
        when(client.getTestPlans(eq(1), anyInt(), eq(50)))
                .thenReturn(Collections.emptyList());
        when(client.getProjects())
                .thenReturn(Stream.of(Project.builder()
                        .id(1)
                        .milestones(Collections.emptyList())
                        .build()));
        when(client.getPlan(anyInt()))
                .thenReturn(TestPlan.builder()
                        .entries(Arrays.array(TestPlan.Entry.builder()
                                .build()))
                        .build());
        when(client.getTests(anyInt()))
                .thenReturn(Stream.of(io.levelops.integrations.testrails.models.Test.builder().build()));
        when(client.getUsers())
                .thenReturn(List.of(User.builder().build()));
        when(client.getStatuses())
                .thenReturn(List.of(io.levelops.integrations.testrails.models.Test.Status.builder().build()));
        when(client.getCaseTypes())
                .thenReturn(List.of(io.levelops.integrations.testrails.models.Test.CaseType.builder().build()));
        when(client.getPriorities())
                .thenReturn(List.of(io.levelops.integrations.testrails.models.Test.Priority.builder().build()));
    }

    @Ignore
    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(TestRailsQuery.builder().integrationKey(TEST_KEY).build()));
    }

    @Ignore
    @Test
    public void fetchMany() throws FetchException {
        List<Data<TestPlan>> testPlans = dataSource.fetchMany(TestRailsQuery.builder()
                .integrationKey(TEST_KEY)
                .build())
                .collect(Collectors.toList());
        assertThat(testPlans).hasSize(1);
    }
}
