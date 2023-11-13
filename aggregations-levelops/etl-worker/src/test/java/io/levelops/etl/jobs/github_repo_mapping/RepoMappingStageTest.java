package io.levelops.etl.jobs.github_repo_mapping;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.ScmRepoMappingResult;
import io.levelops.repomapping.AutoRepoMappingService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepoMappingStageTest {
    @Mock
    AutoRepoMappingService autoRepoMappingService;

    @Mock
    InventoryService inventoryService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private void testGetUpdatedReposInternal(List<String> newRepos, String existingRepos, Set<String> expectedRepos) {
        RepoMappingStage repoMappingStage = new RepoMappingStage(autoRepoMappingService, inventoryService, 1000);
        ScmRepoMappingResult result = ScmRepoMappingResult.builder()
                .mappedRepos(newRepos)
                .build();
        Integration integration = Integration.builder()
                .id("curryIntegration")
                .metadata(Map.of("repos", existingRepos, "otherKey", "otherValue"))
                .build();
        String updatedRepos = repoMappingStage.getUpdatedRepos(result, integration);
        var updatedReposSet = CommaListSplitter.splitToSet(updatedRepos);
        assertThat(updatedReposSet).containsExactlyInAnyOrderElementsOf(expectedRepos);
    }

    @Test
    public void testGetUpdatedRepos() {
        testGetUpdatedReposInternal(
                List.of("repo1", "repo2"),
                "repos3,repos4",
                Set.of("repo1", "repo2", "repos3", "repos4"));
        testGetUpdatedReposInternal(
                List.of(),
                "repos3,repos4",
                Set.of("repos3", "repos4"));
        testGetUpdatedReposInternal(
                List.of("repo1", "repo2"),
                "",
                Set.of("repo1", "repo2"));
    }

    private JobContext createJobContext() {
        return JobContext.builder()
                .jobInstanceId(JobInstanceId.builder().jobDefinitionId(UUID.randomUUID()).instanceId(1).build())
                .integrationId("1")
                .jobScheduledStartTime(new Date())
                .tenantId("1")
                .integrationType("jira")
                .stageProgressMap(new HashMap<>())
                .stageProgressDetailMap(new HashMap<>())
                .gcsRecords(List.of())
                .etlProcessorName("TestJobDefinition")
                .isFull(false)
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .build();
    }

    @Test
    public void testProcess() throws SQLException, IngestionServiceException, InterruptedException, TimeoutException, InventoryException {
        when(autoRepoMappingService.createAndWaitForRepoMappingJob(any(), anyInt())).thenReturn(ScmRepoMappingResult.builder()
                .mappedRepos(List.of("repo1", "repo2"))
                .build());
        when(inventoryService.getIntegration(any(), any())).thenReturn(
                Integration.builder()
                        .id("curryIntegration")
                        .metadata(Map.of("repos", "repos3,repos4", "otherKey", "otherValue"))
                        .build()
        );
        RepoMappingStage repoMappingStage = new RepoMappingStage(autoRepoMappingService, inventoryService, 1000);
        repoMappingStage.process(createJobContext(), new RepoMappingJobState());
        ArgumentCaptor<Integration> integrationArgumentCaptor = ArgumentCaptor.forClass(Integration.class);
        verify(inventoryService).updateIntegration(any(), any(), integrationArgumentCaptor.capture());
        Integration updatedIntegration = integrationArgumentCaptor.getValue();
        assertThat(updatedIntegration.getMetadata()).containsEntry("repos", "repos3,repos4,repo1,repo2");
        assertThat(updatedIntegration.getMetadata()).containsEntry("otherKey", "otherValue");
    }

}