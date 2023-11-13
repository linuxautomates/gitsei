package io.levelops.etl.jobs.jira;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobInstanceUpdate;
import io.levelops.aggregations_shared.exceptions.InvalidJobInstanceIdException;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobMetadata;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JiraIssuesStageTest {

    @Mock
    JiraIssueService issueService;
    @Mock
    JobInstanceDatabaseService jobInstanceDatabaseService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void name() throws SQLException, InvalidJobInstanceIdException, JsonProcessingException {

        int readPageSize = 1000;
        JiraIssuesStage jiraIssuesStage = new JiraIssuesStage(null, null, issueService, null, null, null, null, 1, false, null, null,
                readPageSize, 100);
        JobInstanceId jobInstanceId = JobInstanceId.builder()
                .jobDefinitionId(UUID.randomUUID())
                .instanceId(1)
                .build();
        JobContext jobContext = JobContext.builder()
                .jobInstanceId(jobInstanceId)
                .tenantId("test")
                .integrationId("1")
                .integrationType("jira")
                .jobScheduledStartTime(new Date())
                .etlProcessorName("-")
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .build();
        jobContext = jobContext.withMetadataAccessors(jobInstanceDatabaseService);

        when(jobInstanceDatabaseService.get(eq(jobInstanceId))).thenReturn(Optional.of(DbJobInstance.builder()
                        .metadata(null)
                .build()));
        when(issueService.bulkUpdateEpicStoryPointsSinglePage(eq("test"), eq("1"), anyLong(), anyInt(), anyInt(), anyInt()))
                .thenReturn(true, true, false);

        jiraIssuesStage.bulkUpdateEpicStoryPoints(jobContext);

        ArgumentCaptor<DbJobInstanceUpdate> updateCaptor = ArgumentCaptor.forClass(DbJobInstanceUpdate.class);
        verify(jobInstanceDatabaseService, times(3)).update(eq(jobInstanceId), updateCaptor.capture());
        DefaultObjectMapper.prettyPrint(updateCaptor.getAllValues());

        assertThat(updateCaptor.getAllValues().stream()
                .map(DbJobInstanceUpdate::getMetadata)
                .map(JobMetadata::getCheckpoint)
                .map(checkpoint -> checkpoint.get("bulk_update_epic_story_points_offset")))
                .containsExactly(0, 1000, 2000);
    }
}