package io.levelops.integrations.testrails.source;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.testrails.client.TestRailsClient;
import io.levelops.integrations.testrails.client.TestRailsClientException;
import io.levelops.integrations.testrails.client.TestRailsClientFactory;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.TestRailsQuery;
import io.levelops.integrations.testrails.services.TestRailsEnrichmentService;
import io.levelops.integrations.testrails.sources.TestRailsProjectDataSource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

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
public class TestRailsProjectDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    TestRailsProjectDataSource dataSource;

    @Before
    public void setup() throws TestRailsClientException {
        TestRailsClient client = Mockito.mock(TestRailsClient.class);
        TestRailsClientFactory clientFactory = Mockito.mock(TestRailsClientFactory.class);

        TestRailsEnrichmentService enrichmentService = new TestRailsEnrichmentService(1, 10);
        dataSource = new TestRailsProjectDataSource(clientFactory, enrichmentService);
        when(clientFactory.get(ArgumentMatchers.eq(TEST_KEY))).thenReturn(client);
        List<Project> projects = List.of(Project.builder()
                .id(1)
                .build());
        when(client.getProjects(eq(0), anyInt()))
                .thenReturn(projects);
        when(client.getMilestones(anyInt())).thenReturn(Stream.empty());
    }

    @Ignore
    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(TestRailsQuery.builder().integrationKey(TEST_KEY).build()));
    }

    @Ignore
    @Test
    public void fetchMany() throws FetchException {
        List<Data<Project>> projects = dataSource.fetchMany(TestRailsQuery.builder()
                .integrationKey(TEST_KEY)
                .from(null)
                .shouldFetchUsers(true)
                .build())
                .collect(Collectors.toList());
        assertThat(projects).hasSize(1);
    }
}
