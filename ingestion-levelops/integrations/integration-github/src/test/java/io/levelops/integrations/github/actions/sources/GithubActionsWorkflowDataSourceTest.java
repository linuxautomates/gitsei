package io.levelops.integrations.github.actions.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.github.actions.models.GithubActionsIngestionQuery;
import io.levelops.integrations.github.actions.services.GithubActionsWorkflowRunJobService;
import io.levelops.integrations.github.actions.services.GithubActionsWorkflowRunService;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.services.GithubOrganizationService;
import io.levelops.integrations.github.services.GithubRepositoryService;
import io.levelops.integrations.github_actions.models.GithubActionsEnrichedWorkflowRun;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GithubActionsWorkflowDataSourceTest {
    private GithubClient client;

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder()
            .integrationId(EMPTY).tenantId(EMPTY).build();

    private GithubActionsWorkflowDataSource dataSource;
    private GithubRepositoryService repositoryService;
    private GithubOrganizationService organizationService;
    private GithubActionsWorkflowRunService workflowRunService;
    private GithubActionsWorkflowRunJobService workflowRunJobService;

    @Before
    public void setup() throws GithubClientException {
        client = mock(GithubClient.class);
        GithubClientFactory clientFactory = mock(GithubClientFactory.class);
        repositoryService = mock(GithubRepositoryService.class);
        organizationService = mock(GithubOrganizationService.class);
        workflowRunService = mock(GithubActionsWorkflowRunService.class);
        workflowRunJobService = mock(GithubActionsWorkflowRunJobService.class);
        dataSource = new GithubActionsWorkflowDataSource(clientFactory, repositoryService, organizationService, workflowRunService, workflowRunJobService);
        when(clientFactory.get(TEST_KEY, true)).thenReturn(client);
        when(clientFactory.get(TEST_KEY, false)).thenReturn(client);
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(GithubActionsIngestionQuery.builder().integrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<GithubActionsEnrichedWorkflowRun>> workflowRuns = dataSource.fetchMany(JobContext.builder().build(), GithubActionsIngestionQuery.builder()
                        .integrationKey(TEST_KEY)
                        .from(null)
                        .to(null)
                        .build())
                .collect(Collectors.toList());
        assertThat(workflowRuns).hasSize(0);
    }
}
