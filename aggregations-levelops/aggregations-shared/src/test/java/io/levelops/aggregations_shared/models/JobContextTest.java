package io.levelops.aggregations_shared.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobInstanceUpdate;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobMetadata;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.etl.models.job_progress.EntityProgressDetail;
import io.levelops.commons.etl.models.job_progress.FileProgressDetail;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class JobContextTest {

    JobInstanceId jobInstanceId = JobInstanceId.builder()
            .instanceId(1)
            .jobDefinitionId(UUID.randomUUID())
            .build();
    JobContext jobContext = JobContext.builder()
            .jobInstanceId(jobInstanceId)
            .tenantId("sid")
            .integrationId("1")
            .integrationType("jira")
            .jobScheduledStartTime(new Date())
            .etlProcessorName("test-processor")
            .timeoutInMinutes(10L)
            .isFull(true)
            .jobType(JobType.GENERIC_INTEGRATION_JOB)
            .gcsRecords(null)
            .build();

    @Test
    public void testSerialization() throws JsonProcessingException {
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        String serialized = objectMapper.writeValueAsString(jobContext);
        System.out.println(serialized);

        JobContext deserialized = objectMapper.readValue(serialized, JobContext.class);
        System.out.println(deserialized);
        assertThat(deserialized).isEqualTo(jobContext);
    }

    @Test
    public void testMetadata() throws JsonProcessingException {
        // check exception case
        assertThatThrownBy(() -> jobContext.getMetadata()).hasMessageContaining("Metadata accessors not available for this JobContext");
        assertThatThrownBy(() -> jobContext.setMetadata(null)).hasMessageContaining("Metadata accessors not available for this JobContext");

        JobInstanceDatabaseService jobInstanceDatabaseService = Mockito.mock(JobInstanceDatabaseService.class);
        JobContext withMetadataAccessors = jobContext.withMetadataAccessors(jobInstanceDatabaseService);

        // check getter
        JobMetadata emptyMetadata = JobMetadata.builder().build();
        JobMetadata metadataWithData = JobMetadata.builder()
                .checkpoint(Map.of("some", "value"))
                .build();
        when(jobInstanceDatabaseService.get(eq(jobInstanceId))).thenReturn(
                Optional.of(DbJobInstance.builder()
                        .metadata(null)
                        .build()),
                Optional.of(DbJobInstance.builder()
                        .metadata(metadataWithData)
                        .build()));

        assertThat(withMetadataAccessors.getMetadata()).isEqualTo(emptyMetadata);
        assertThat(withMetadataAccessors.getMetadata()).isEqualTo(metadataWithData);

        // check setter
        withMetadataAccessors.setMetadata(metadataWithData);

        Mockito.verify(jobInstanceDatabaseService, times(1)).update(eq(jobInstanceId), eq(DbJobInstanceUpdate.builder()
                .metadata(metadataWithData)
                .build()));
    }

    @Test
    public void testBatchUpdateProgressDetails() throws JsonProcessingException {
        JobInstanceDatabaseService jobInstanceDatabaseService = Mockito.mock(JobInstanceDatabaseService.class);
        JobContext withMetadataAccessors = jobContext.withMetadataAccessors(jobInstanceDatabaseService).toBuilder()
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .stageProgressDetailMap(new HashMap<>())
                .build();
        FileProgressDetail fileProgressDetail = FileProgressDetail.builder()
                .entityProgressDetail(
                        EntityProgressDetail.builder()
                                .totalEntities(10)
                                .successful(10)
                                .build())
                .build();


        int batchSize = JobContext.PROGRESS_DETAIL_BATCH_SIZE;

        // We only write to the db every 5th time
        for (int i = 0; i < batchSize - 1; i++) {
            withMetadataAccessors.batchUpdateProgressDetail(
                    "Warriors Stage",
                    i,
                    fileProgressDetail,
                    jobInstanceDatabaseService
            );
            Mockito.verify(jobInstanceDatabaseService, times(0)).update(any(), any());
        }

        // This is the 5th time, so we should write to the db
        withMetadataAccessors.batchUpdateProgressDetail(
                "Warriors Stage",
                batchSize - 1,
                fileProgressDetail,
                jobInstanceDatabaseService
        );
        ArgumentCaptor<DbJobInstanceUpdate> dbJobInstanceUpdateArgumentCaptor = ArgumentCaptor.forClass(DbJobInstanceUpdate.class);
        Mockito.verify(jobInstanceDatabaseService, times(1)).update(any(), dbJobInstanceUpdateArgumentCaptor.capture());
        DbJobInstanceUpdate update = dbJobInstanceUpdateArgumentCaptor.getValue();
        assertThat(update.getProgressDetails().get("Warriors Stage").getFileProgressMap()).hasSize(batchSize);

        reset(jobInstanceDatabaseService);

        // After writing to DB the count should reset so we should again not write 4 times
        // We only write to the db every 5th time
        for (int i = 0; i < batchSize - 1; i++) {
            withMetadataAccessors.batchUpdateProgressDetail(
                    "Warriors Stage",
                    i + batchSize,
                    fileProgressDetail,
                    jobInstanceDatabaseService
            );
            Mockito.verify(jobInstanceDatabaseService, times(0)).update(any(), any());
        }
        withMetadataAccessors.batchUpdateProgressDetail(
                "Warriors Stage",
                batchSize * 2 - 1,
                fileProgressDetail,
                jobInstanceDatabaseService
        );
        dbJobInstanceUpdateArgumentCaptor = ArgumentCaptor.forClass(DbJobInstanceUpdate.class);
        Mockito.verify(jobInstanceDatabaseService, times(1)).update(any(), dbJobInstanceUpdateArgumentCaptor.capture());
        update = dbJobInstanceUpdateArgumentCaptor.getValue();
        assertThat(update.getProgressDetails().get("Warriors Stage").getFileProgressMap()).hasSize(batchSize * 2);
    }
}
