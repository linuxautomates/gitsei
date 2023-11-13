package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.controllers.IntermediateStateUpdater;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.gitlab.models.GitlabIterativeScanQuery;
import io.levelops.integrations.gitlab.models.GitlabQuery;
import io.levelops.integrations.gitlab.sources.GitlabProjectDataSource;
import io.levelops.integrations.gitlab.sources.GitlabProjectPipelineDataSource;
import io.levelops.integrations.gitlab.sources.GitlabUsersDataSource;
import io.levelops.integrations.storage.models.StorageResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GitlabIterativeScanControllerTest {
    GitlabIterativeScanController controller;
    private ObjectMapper objectMapper;
    @Mock
    IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> commitController;
    @Mock
    IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> tagController;
    @Mock
    IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> mergeRequestController;
    @Mock
    IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> projectController;
    @Mock
    IntegrationController<GitlabUsersDataSource.GitlabUserQuery> userController;
    @Mock
    IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> milestoneController;
    @Mock
    IntegrationController<GitlabProjectPipelineDataSource.GitlabProjectQuery> pipelineController;
    @Mock
    IntegrationController<GitlabQuery> issueController;
    @Mock
    IntegrationController<GitlabQuery> groupController;
    @Mock
    InventoryService inventoryService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        objectMapper = DefaultObjectMapper.get();
        controller = new GitlabIterativeScanController(
                objectMapper,
                commitController,
                tagController,
                mergeRequestController,
                projectController,
                userController,
                milestoneController,
                pipelineController,
                issueController,
                groupController,
                inventoryService,
                10
        );
        when(commitController.ingest(any(), any(), any())).thenReturn(new StorageResult());
        when(tagController.ingest(any(), any(), any())).thenReturn(new StorageResult());
        when(mergeRequestController.ingest(any(), any(), any())).thenReturn(new StorageResult());
        when(projectController.ingest(any(), any(), any())).thenReturn(new StorageResult());
        when(userController.ingest(any(), any(), any())).thenReturn(new StorageResult());
        when(pipelineController.ingest(any(), any(), any())).thenReturn(new StorageResult());
        when(issueController.ingest(any(), any(), any())).thenReturn(new StorageResult());
        when(groupController.ingest(any(), any(), any())).thenReturn(new StorageResult());
    }

    @Test
    public void testProjectQueryDefaultConfigs() throws IngestException, InventoryException {
        JobContext jobContext = JobContext.builder()
                .jobId(UUID.randomUUID().toString())
                .integrationId("1")
                .tenantId("test")
                .attemptCount(0)
                .build();
        GitlabIterativeScanQuery query = GitlabIterativeScanQuery.builder()
                .integrationKey(IntegrationKey.builder()
                        .integrationId("1")
                        .tenantId("test")
                        .build())
                .from(new Date())
                .to(new Date())
                .shouldFetchGroups(false)
                .shouldFetchProjects(false)
                .shouldFetchAllUsers(false)
                .build();
        IntermediateStateUpdater updater = Mockito.mock(IntermediateStateUpdater.class);

        Integration integration = Integration.builder()
                .id("1")
                .metadata(Map.of())
                .build();
        when(inventoryService.getIntegration(any())).thenReturn(integration);
        controller.ingest(jobContext, query, updater);
        ArgumentCaptor<GitlabProjectDataSource.GitlabProjectQuery> captor = ArgumentCaptor.forClass(GitlabProjectDataSource.GitlabProjectQuery.class);
        verify(commitController).ingest(any(), captor.capture(), any());
        GitlabProjectDataSource.GitlabProjectQuery q = captor.getValue();
        assertThat(q.getFetchCommitPatches()).isTrue();
        assertThat(q.getFetchPrPatches()).isFalse();
        assertThat(q.getFetchStateEvents()).isFalse();
        assertThat(q.getPrCommitsLimit()).isEqualTo(250);
    }
}