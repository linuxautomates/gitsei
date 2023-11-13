package io.levelops.etl.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.aggregations_shared.utils.IngestionResultPayloadUtils;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.GcsDataResultWithDataType;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobInstancePayload;
import io.levelops.commons.etl.models.JobPriority;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.ListResponse;
import io.levelops.etl.job_framework.EtlJobRunner;
import io.levelops.etl.job_framework.EtlProcessorRegistry;
import io.levelops.etl.services.JobTrackingUtilsService;
import io.levelops.etl.utils.GcsUtils;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.integrations.gcs.models.BlobId;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.integrations.storage.models.StorageContent;
import io.levelops.utils.SampleJobStage;
import io.levelops.utils.TestEtlProcessor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Log4j2
public class EtlEngineTest {
    EtlEngine engine;
    TestEtlProcessor aggProcessor;
    EtlJobRunner jobRunner;
    JobContext jobContext;
    TestEtlProcessor.TestJobState testJobState;
    @Mock
    JobInstanceDatabaseService jobInstanceDatabaseService;
    @Mock
    JobDefinitionDatabaseService jobDefinitionDatabaseService;
    @Mock
    JobTrackingUtilsService jobTrackingUtilsService;
    LongRunningJobStage longRunningJobStage;

    @Mock
    IngestionResultPayloadUtils ingestionResultPayloadUtils;

    @Log4j2
    public static class LongRunningJobStage extends SampleJobStage {
        @Override
        public void process(JobContext context, TestEtlProcessor.TestJobState jobState, String ingestionJobId, SampleJobStage.ExampleSerialized entity) throws SQLException {
            log.info("Thread interrupted before: {}", Thread.currentThread().isInterrupted());
            for (int i = 0; i < 2000; i++) {
                log.info("Printing useless message {} {}", i, Thread.currentThread().getName());
            }
            log.info("Thread {} interrupted after: {}", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
        }
    }


    @Before
    public void setup() throws JsonProcessingException, IngestionServiceException {
        MockitoAnnotations.initMocks(this);
        this.testJobState = spy(new TestEtlProcessor.TestJobState());
        GcsUtils gcsUtils = mock(GcsUtils.class);
        this.longRunningJobStage = spy(new LongRunningJobStage());
        this.aggProcessor = spy(new TestEtlProcessor(List.of(longRunningJobStage)));
        when(gcsUtils.fetchRecordsFromGcs(any(), any(), any(), any(), any())).thenReturn(
                StorageContent.<ListResponse<Object>>builder()
                        .data(ListResponse.builder()
                                .records(List.of(
                                        SampleJobStage.ExampleSerialized.builder()
                                                .name("test")
                                                .build(),
                                        SampleJobStage.ExampleSerialized.builder()
                                                .name("test2")
                                                .build()))
                                .build()).build()
        );
        when(jobTrackingUtilsService.updateJobInstanceStatus(any(), any())).thenReturn(true);
        when(jobTrackingUtilsService.updateJobInstanceToPending(any())).thenReturn(true);
        when(jobInstanceDatabaseService.update(any(), any())).thenReturn(true);
        var jobDefinitionId = UUID.randomUUID();
        DbJobDefinition jobDefinition = DbJobDefinition.builder()
                .aggProcessorName(aggProcessor.getComponentClass())
                .ingestionTriggerId("testTriggerId")
                .id(jobDefinitionId)
                .fullFrequencyInMinutes(30)
                .tenantId("1")
                .integrationId("1")
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .defaultPriority(JobPriority.HIGH)
                .isActive(true)
                .build();
        DbJobInstance jobInstance = DbJobInstance.builder()
                .jobDefinitionId(jobDefinitionId)
                .instanceId(1)
                .aggProcessorName(aggProcessor.getComponentClass())
                .isFull(true)
                .id(UUID.randomUUID())
                .build();

        when(jobInstanceDatabaseService.get(any())).thenReturn(Optional.of(jobInstance));
        when(jobDefinitionDatabaseService.get(any())).thenReturn(Optional.of(jobDefinition));

        JobInstancePayload jobInstancePayload = JobInstancePayload.builder()
                .gcsRecords(List.of(
                                GcsDataResultWithDataType.builder()
                                        .gcsDataResult(GcsDataResult.builder()
                                                .blobId(BlobId.builder()
                                                        .bucket("bucket")
                                                        .generation(1L)
                                                        .build())
                                                .uri("uri")
                                                .htmlUri("htmluri")
                                                .build())
                                        .build()
                        )
                ).build();

        when(ingestionResultPayloadUtils.generatePayloadForJobDefinition(any(UUID.class), anyBoolean())).thenReturn(jobInstancePayload);
        when(ingestionResultPayloadUtils.generatePayloadForJobDefinition(any(DbJobDefinition.class), anyBoolean())).thenReturn(jobInstancePayload);
        when(ingestionResultPayloadUtils.uploadPayloadToGcs(any(), any())).thenReturn(GcsDataResult.builder()
                .blobId(BlobId.builder()
                        .name("name")
                        .bucket("bucket")
                        .generation(1L)
                        .build())
                .htmlUri("htmlUri")
                .uri("uri")
                .build());

        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        this.jobRunner = new EtlJobRunner(gcsUtils, DefaultObjectMapper.get(), jobInstanceDatabaseService, jobTrackingUtilsService, jobDefinitionDatabaseService, meterRegistry);
        this.engine = new EtlEngine(2, 100000, jobRunner, jobTrackingUtilsService, jobInstanceDatabaseService, new EtlProcessorRegistry(List.of(this.aggProcessor)), meterRegistry, ingestionResultPayloadUtils);
        when(aggProcessor.createState(any())).thenReturn(testJobState);
        this.jobContext = JobContext.builder()
                .stageProgressMap(new HashMap<>())
                .stageProgressDetailMap(new HashMap<>())
                .jobInstanceId(JobInstanceId.builder().jobDefinitionId(jobDefinitionId).instanceId(1).build())
                .integrationId("1")
                .jobScheduledStartTime(new Date())
                .tenantId("1")
                .integrationType("jira")
                .gcsRecords(List.of(
                        GcsDataResultWithDataType.builder()
                                .index(0)
                                .dataTypeName("jira")
                                .gcsDataResult(null)
                                .build(),
                        GcsDataResultWithDataType.builder()
                                .index(1)
                                .dataTypeName("jira")
                                .gcsDataResult(null)
                                .build()))
                .etlProcessorName(aggProcessor.getComponentClass())
                .isFull(false)
                .timeoutInMinutes(1L)
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .build();
    }

    @Test
    public void testCancelFuture() throws ExecutionException, InterruptedException, SQLException, JsonProcessingException {
        var jobOptional = engine.submitJob(jobContext);
        log.info("Job submitted");
        assertThat(jobOptional.isPresent()).isTrue();
        System.out.println("Cancelling task");
        boolean cancelled = jobOptional.get().f.cancel(true);
        System.out.println("Cancelled : " + cancelled);
        // Wait for computation to finish
        Thread.sleep(1000);
        // There are 2GCS files and each has 2 entities set up in mocks
        // If there was no cancel then we should see process() called 4 times
        // But since we cancel immediately we will only see it being called once
        verify(longRunningJobStage, times(1)).process(any(), any(), any(), any());
        verify(jobTrackingUtilsService, times(1)).updateJobInstanceStatus(any(), eq(JobStatus.FAILURE));

        // Submit the job again and this time don't cancel. Confirm that it goes all the way through
        clearInvocations(longRunningJobStage, jobTrackingUtilsService);
        engine.clearJobs();
        var successJobOptional = engine.submitJob(jobContext);
        assertThat(successJobOptional.isPresent()).isTrue();
        synchronized (successJobOptional.get().f) {
            successJobOptional.get().f.get();
        }
        verify(longRunningJobStage, times(4)).process(any(), any(), any(), any());
        verify(jobTrackingUtilsService, times(0)).updateJobInstanceStatus(any(), eq(JobStatus.FAILURE));
        verify(jobTrackingUtilsService, times(1)).updateJobInstanceStatus(any(), eq(JobStatus.SUCCESS));
    }

    // TODO: Re-enable
    //    @Test
    public void testNoJobDefinition() {
        var job = engine.submitJob((jobContext));
        assertThat(job).isEmpty();
    }

    @Test
    public void testSimpleEngineRunWithRehydratingPayload() throws ExecutionException, InterruptedException {
        engine.clearJobs();
        JobContext noPayloadContext = jobContext.toBuilder().gcsRecords(null).build();
        var engineJobOptional = engine.submitJob(noPayloadContext);
        assertThat(engineJobOptional.isPresent()).isTrue();
        engineJobOptional.get().f.get();
    }
}
