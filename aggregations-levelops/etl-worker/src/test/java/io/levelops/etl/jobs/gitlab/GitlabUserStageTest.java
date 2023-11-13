package io.levelops.etl.jobs.gitlab;


import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobType;
import io.levelops.integrations.gitlab.models.GitlabUser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class GitlabUserStageTest {
    @Mock
    UserIdentityService userIdentityService;

    @Captor
    private ArgumentCaptor<List<DbScmUser>> captor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private JobContext createJobContext() {
        return JobContext.builder()
                .jobInstanceId(JobInstanceId.builder().jobDefinitionId(UUID.randomUUID()).instanceId(1).build())
                .integrationId("1")
                .jobScheduledStartTime(new Date())
                .tenantId("BCCI")
                .integrationType("gitlab")
                .stageProgressMap(new HashMap<>())
                .stageProgressDetailMap(new HashMap<>())
                .gcsRecords(List.of())
                .etlProcessorName("GitlabEtlProcessor")
                .isFull(false)
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .build();
    }

    @Test
    public void testProcess() throws SQLException {
        GitlabUserStage gitlabUserStage = new GitlabUserStage(userIdentityService);
        JobContext ctx = createJobContext();
        GitlabState state = new GitlabState(new ArrayList<>());
        GitlabUser gitlabUser = GitlabUser.builder()
                .id("id")
                .email("virat@goat.com")
                .username("viratk")
                .name("Virat Kohli")
                .build();
        gitlabUserStage.process(ctx, state, "1", gitlabUser);
        assertThat(state.getUsers()).hasSize(1);
        assertThat(state.getUsers().get(0).getCloudId()).isEqualTo("viratk");
        assertThat(state.getUsers().get(0).getDisplayName()).isEqualTo("Virat Kohli");
        assertThat(state.getUsers().get(0).getEmails()).containsExactly("virat@goat.com");

        gitlabUserStage.postStage(ctx, state);
        verify(userIdentityService).batchUpsert(eq("BCCI"), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }
}