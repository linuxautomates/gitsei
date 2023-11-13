package io.levelops.etl.job_framework;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobInstanceUpdate;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.etl.models.GcsDataResultWithDataType;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.ListResponse;
import io.levelops.etl.services.JobTrackingUtilsService;
import io.levelops.etl.utils.GcsUtils;
import io.levelops.integrations.gcs.models.BlobId;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.integrations.storage.models.StorageContent;
import io.levelops.utils.SampleGenericJobStage;
import io.levelops.utils.SampleJobStage;
import io.levelops.utils.SampleJobStage.ExampleSerialized;
import io.levelops.utils.TestEtlProcessor;
import io.levelops.utils.TestEtlProcessor.TestJobState;
import io.levelops.utils.TestGenericEtlProcessor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EtlJobRunnerTest {
    @Mock
    JobInstanceDatabaseService jobInstanceDatabaseService;
    @Mock
    JobDefinitionDatabaseService jobDefinitionDatabaseService;
    @Mock
    JobTrackingUtilsService jobTrackingUtilsService;
    MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private JobContext createJobContext(UUID jobDefinitionId, List<GcsDataResultWithDataType> gcsRecords, JobType jobType) {
        return JobContext.builder()
                .jobInstanceId(JobInstanceId.builder().jobDefinitionId(jobDefinitionId).instanceId(1).build())
                .integrationId("1")
                .jobScheduledStartTime(new Date())
                .tenantId("1")
                .integrationType("jira")
                .stageProgressMap(new HashMap<>())
                .stageProgressDetailMap(new HashMap<>())
                .gcsRecords(gcsRecords)
                .etlProcessorName("TestJobDefinition")
                .isFull(false)
                .jobType(jobType)
                .build();
    }

    @Test
    public void simpleTest() throws JsonProcessingException, SQLException {
        MockitoAnnotations.initMocks(this);
        TestJobState testJobState = spy(new TestJobState());
        IngestionResultProcessingStage<ExampleSerialized, TestJobState> testStage = spy(new SampleJobStage());
        EtlProcessor<TestJobState> testEtlProcessor = spy(new TestEtlProcessor(List.of(testStage)));
        when(testEtlProcessor.createState(any())).thenReturn(testJobState);
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        GcsUtils gcsUtils = mock(GcsUtils.class);
        when(gcsUtils.fetchRecordsFromGcs(any(), any(), any(), any(), any())).thenReturn(
                StorageContent.<ListResponse<Object>>builder()
                        .data(ListResponse.builder()
                                .records(List.of(
                                        ExampleSerialized.builder()
                                                .name("test")
                                                .build()))
                                .build()).build()
        );
        when(jobTrackingUtilsService.updateJobInstanceToPending(any())).thenReturn(true);
        var jobRunner = new EtlJobRunner(gcsUtils, objectMapper, jobInstanceDatabaseService, jobTrackingUtilsService, jobDefinitionDatabaseService, meterRegistry);
        var uuid = UUID.randomUUID();
        var jobContext = createJobContext(uuid, List.of(
                GcsDataResultWithDataType.builder()
                        .dataTypeName("jira")
                        .gcsDataResult(null)
                        .index(0)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("jira")
                        .gcsDataResult(null)
                        .index(1)
                        .build()), JobType.INGESTION_RESULT_PROCESSING_JOB);
        jobContext = jobContext.toBuilder().jobInstanceDatabaseService(jobInstanceDatabaseService).build();
        assertThat(jobContext.getStageProgress("stage")).isEmpty();
        jobRunner.run(jobContext, testEtlProcessor);
        verify(testEtlProcessor).postProcess(any(), any());
        verify(testEtlProcessor).preProcess(any(), any());
        verify(testEtlProcessor, times(2)).getJobStages(); // Once by the agg runner, once by JobUtils.determineJObSuccessStatus
        verify(testStage, times(2)).process(any(), any(), any(), any());
        assertThat(testJobState.getName()).isEqualTo("testing testing");
        // Since 2 is added in the entity processing stage and there are 2 entities, we should see twice
        assertThat(testJobState.getListPopulatedBeforeJob()).isEqualTo(List.of(1, 2, 2, 3, 4));
    }

    @Test
    public void multipleDataTypesTest() throws JsonProcessingException, SQLException {
        MockitoAnnotations.initMocks(this);
        TestJobState testJobState = spy(new TestJobState());
        IngestionResultProcessingStage<ExampleSerialized, TestJobState> testStage = spy(new SampleJobStage());
        EtlProcessor<TestJobState> testEtlProcessor = spy(new TestEtlProcessor(List.of(testStage)));
        when(testEtlProcessor.createState(any())).thenReturn(testJobState);
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        GcsUtils gcsUtils = mock(GcsUtils.class);
        when(gcsUtils.fetchRecordsFromGcs(any(), any(), any(), any(), any())).thenReturn(
                StorageContent.<ListResponse<Object>>builder()
                        .data(ListResponse.builder()
                                .records(List.of(
                                        ExampleSerialized.builder()
                                                .name("test")
                                                .build()))
                                .build()).build()
        );
        when(jobTrackingUtilsService.updateJobInstanceToPending(any())).thenReturn(true);
        var jobRunner = new EtlJobRunner(gcsUtils, objectMapper, jobInstanceDatabaseService, jobTrackingUtilsService, jobDefinitionDatabaseService, meterRegistry);
        var uuid = UUID.randomUUID();
        var jobContext = createJobContext(uuid, List.of(
                GcsDataResultWithDataType.builder()
                        .dataTypeName("jira")
                        .gcsDataResult(null)
                        .index(0)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("prs")
                        .gcsDataResult(null)
                        .index(1)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("prs")
                        .gcsDataResult(null)
                        .index(2)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("jira")
                        .gcsDataResult(null)
                        .index(3)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("commits")
                        .gcsDataResult(null)
                        .index(4)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("jira")
                        .gcsDataResult(null)
                        .index(5)
                        .build()), JobType.INGESTION_RESULT_PROCESSING_JOB);
        assertThat(jobContext.getStageProgress("stage")).isEmpty();
        jobRunner.run(jobContext, testEtlProcessor);
        verify(testEtlProcessor).postProcess(any(), any());
        verify(testEtlProcessor).preProcess(any(), any());
        verify(testEtlProcessor, times(2)).getJobStages(); // Once by the agg runner, once by JobUtils.determineJObSuccessStatus
        verify(testStage, times(3)).process(any(), any(), any(), any());
        assertThat(testJobState.getName()).isEqualTo("testing testing");
        assertThat(jobContext.getStageProgress("test").get()).isEqualTo(5);
    }

    @Test
    public void checkpointingTest() throws JsonProcessingException, SQLException {
        MockitoAnnotations.initMocks(this);
        TestJobState testJobState = spy(new TestJobState());
        IngestionResultProcessingStage<ExampleSerialized, TestJobState> testStage = spy(new SampleJobStage());
        EtlProcessor<TestJobState> testEtlProcessor = spy(new TestEtlProcessor(List.of(testStage)));
        when(testEtlProcessor.createState(any())).thenReturn(testJobState);
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        GcsUtils gcsUtils = mock(GcsUtils.class);
        when(gcsUtils.fetchRecordsFromGcs(any(), any(), any(), any(), any())).thenReturn(
                StorageContent.<ListResponse<Object>>builder()
                        .data(ListResponse.builder()
                                .records(List.of(
                                        ExampleSerialized.builder()
                                                .name("test")
                                                .build()))
                                .build()).build()
        );
        when(jobTrackingUtilsService.updateJobInstanceToPending(any())).thenReturn(true);
        var jobRunner = new EtlJobRunner(gcsUtils, objectMapper, jobInstanceDatabaseService, jobTrackingUtilsService, jobDefinitionDatabaseService, meterRegistry);
        var uuid = UUID.randomUUID();
        var jobContext = createJobContext(uuid, List.of(
                GcsDataResultWithDataType.builder()
                        .dataTypeName("jira")
                        .gcsDataResult(null)
                        .index(0)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("prs")
                        .gcsDataResult(null)
                        .index(1)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("prs")
                        .gcsDataResult(null)
                        .index(2)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("jira")
                        .gcsDataResult(null)
                        .index(3)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("commits")
                        .gcsDataResult(null)
                        .index(4)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("jira")
                        .gcsDataResult(null)
                        .index(5)
                        .build()), JobType.INGESTION_RESULT_PROCESSING_JOB);
        var progressMap = new HashMap<String, Integer>();
        progressMap.put("test", 4);
        jobContext = jobContext.toBuilder()
                .stageProgressMap(progressMap)
                .build();
        assertThat(jobContext.getStageProgress("test").get()).isEqualTo(4);
        jobRunner.run(jobContext, testEtlProcessor);
        verify(testStage, times(1)).process(any(), any(), any(), any());
        assertThat(jobContext.getStageProgress("test").get()).isEqualTo(5);
    }

    public static class OnlyLatestSampleJobStage extends SampleJobStage {
        @Override
        public boolean onlyProcessLatestIngestionJob() {
            return true;
        }
    }

    @Test
    public void testProcessOnlyLatestIngestionJobStage() throws JsonProcessingException, SQLException {
        MockitoAnnotations.initMocks(this);
        TestJobState testJobState = spy(new TestJobState());
        IngestionResultProcessingStage<ExampleSerialized, TestJobState> testStage = spy(new OnlyLatestSampleJobStage());
        EtlProcessor<TestJobState> testEtlProcessor = spy(new TestEtlProcessor(List.of(testStage)));
        when(testEtlProcessor.createState(any())).thenReturn(testJobState);
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        GcsUtils gcsUtils = mock(GcsUtils.class);
        when(gcsUtils.fetchRecordsFromGcs(any(), any(), any(), any(), any())).thenReturn(
                StorageContent.<ListResponse<Object>>builder()
                        .data(ListResponse.builder()
                                .records(List.of(
                                        ExampleSerialized.builder()
                                                .name("test")
                                                .build()))
                                .build()).build()
        );
        when(jobTrackingUtilsService.updateJobInstanceToPending(any())).thenReturn(true);
        var jobRunner = new EtlJobRunner(gcsUtils, objectMapper, jobInstanceDatabaseService, jobTrackingUtilsService, jobDefinitionDatabaseService, meterRegistry);
        var uuid = UUID.randomUUID();
        var jobContext = createJobContext(uuid, List.of(
                GcsDataResultWithDataType.builder()
                        .dataTypeName("jira")
                        .gcsDataResult(null)
                        .ingestionJobId("j0")
                        .index(0)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("prs")
                        .gcsDataResult(null)
                        .ingestionJobId("j1")
                        .index(1)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("prs")
                        .gcsDataResult(null)
                        .ingestionJobId("j1")
                        .index(2)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("jira")
                        .gcsDataResult(null)
                        .ingestionJobId("j2")
                        .index(3)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("commits")
                        .gcsDataResult(null)
                        .ingestionJobId("j2")
                        .index(4)
                        .build(),
                GcsDataResultWithDataType.builder()
                        .dataTypeName("jira")
                        .gcsDataResult(null)
                        .ingestionJobId("j2")
                        .index(5)
                        .build()), JobType.INGESTION_RESULT_PROCESSING_JOB);
        assertThat(jobContext.getStageProgress("test")).isEmpty();
        jobRunner.run(jobContext, testEtlProcessor);
        verify(testStage, times(2)).process(any(), any(), eq("j2"), any());
        verify(testStage, times(2)).process(any(), any(), any(), any());
        assertThat(jobContext.getStageProgress("test").get()).isEqualTo(5);
    }

    @Log4j2
    public static class ParallelProcessingJobStage extends SampleJobStage {
        @Override
        public boolean allowParallelProcessing(String tenantId, String integrationId) {
            return true;
        }

        @Override
        public int getParallelProcessingThreadCount() {
            return 4;
        }

        @Override
        public void process(JobContext context, TestEtlProcessor.TestJobState jobState, String ingestionJobId, SampleJobStage.ExampleSerialized entity) throws SQLException {
            log.info("Starting process() - sleeping for 1 second");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("Finished sleeping");
        }
    }

    @Test
    public void testParallelProcessingJobStage() throws JsonProcessingException, SQLException {
        MockitoAnnotations.initMocks(this);
        TestJobState testJobState = spy(new TestJobState());
        IngestionResultProcessingStage<ExampleSerialized, TestJobState> testStage = spy(new ParallelProcessingJobStage());
        EtlProcessor<TestJobState> testEtlProcessor = spy(new TestEtlProcessor(List.of(testStage)));
        when(testEtlProcessor.createState(any())).thenReturn(testJobState);
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        GcsUtils gcsUtils = mock(GcsUtils.class);
        when(gcsUtils.fetchRecordsFromGcs(any(), any(), any(), any(), any())).thenReturn(
                StorageContent.<ListResponse<Object>>builder()
                        .data(ListResponse.builder()
                                .records(List.of(
                                        ExampleSerialized.builder()
                                                .name("test")
                                                .build(),
                                        ExampleSerialized.builder()
                                                .name("test1")
                                                .build(),
                                        ExampleSerialized.builder()
                                                .name("test2")
                                                .build(),
                                        ExampleSerialized.builder()
                                                .name("test3")
                                                .build(),
                                        ExampleSerialized.builder()
                                                .name("test4")
                                                .build()))
                                .build()).build()
        );
        when(jobTrackingUtilsService.updateJobInstanceToPending(any())).thenReturn(true);
        var jobRunner = new EtlJobRunner(gcsUtils, objectMapper, jobInstanceDatabaseService, jobTrackingUtilsService, jobDefinitionDatabaseService, meterRegistry);
        var uuid = UUID.randomUUID();
        var jobContext = createJobContext(uuid, List.of(
                GcsDataResultWithDataType.builder()
                        .dataTypeName("jira")
                        .gcsDataResult(null)
                        .ingestionJobId("j0")
                        .index(0)
                        .build()), JobType.INGESTION_RESULT_PROCESSING_JOB);
        assertThat(jobContext.getStageProgress("test")).isEmpty();
        jobRunner.run(jobContext, testEtlProcessor);
    }

    @Test
    public void testFailureToReadGcsFile() throws JsonProcessingException, SQLException {
        MockitoAnnotations.initMocks(this);
        TestJobState testJobState = spy(new TestJobState());
        IngestionResultProcessingStage<ExampleSerialized, TestJobState> testStage = spy(new SampleJobStage());
        EtlProcessor<TestJobState> testEtlProcessor = spy(new TestEtlProcessor(List.of(testStage)));
        when(testEtlProcessor.createState(any())).thenReturn(testJobState);
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        GcsUtils gcsUtils = mock(GcsUtils.class);
        when(gcsUtils.fetchRecordsFromGcs(any(), any(), any(), any(), any())).thenReturn(null);
        when(jobTrackingUtilsService.updateJobInstanceToPending(any())).thenReturn(true);
        var jobRunner = new EtlJobRunner(gcsUtils, objectMapper, jobInstanceDatabaseService, jobTrackingUtilsService, jobDefinitionDatabaseService, meterRegistry);
        var uuid = UUID.randomUUID();
        var jobContext = createJobContext(uuid, List.of(
                GcsDataResultWithDataType.builder()
                        .dataTypeName("jira")
                        .gcsDataResult(GcsDataResult.builder()
                                .blobId(BlobId.builder().bucket("bucket").generation(1L).build())
                                .htmlUri("htmluri")
                                .uri("uri")
                                .build())
                        .ingestionJobId("j0")
                        .index(0)
                        .build()), JobType.INGESTION_RESULT_PROCESSING_JOB);
        jobContext = jobContext.toBuilder().jobInstanceDatabaseService(jobInstanceDatabaseService).build();
        assertThat(jobContext.getStageProgress("test")).isEmpty();
        ArgumentCaptor<DbJobInstanceUpdate> updateCaptor = ArgumentCaptor.forClass(DbJobInstanceUpdate.class);
        jobRunner.run(jobContext, testEtlProcessor);
        verify(jobInstanceDatabaseService, times(2)).update(any(), updateCaptor.capture());
        assertThat(updateCaptor.getAllValues().get(1).getProgressDetails().get("test").getFileProgressMap().get(0).getEntityProgressDetail().successful).isEqualTo(-1);
    }

    @Test
    public void testGenericProcessor() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);
        TestGenericEtlProcessor.TestJobState testJobState = spy(new TestGenericEtlProcessor.TestJobState());
        GenericJobProcessingStage<TestGenericEtlProcessor.TestJobState> testStage = spy(new SampleGenericJobStage());
        TestGenericEtlProcessor testEtlProcessor = spy(new TestGenericEtlProcessor(List.of(testStage)));
        when(testEtlProcessor.createState(any())).thenReturn(testJobState);
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        GcsUtils gcsUtils = mock(GcsUtils.class);
        when(jobTrackingUtilsService.updateJobInstanceToPending(any())).thenReturn(true);
        var jobRunner = new EtlJobRunner(gcsUtils, objectMapper, jobInstanceDatabaseService, jobTrackingUtilsService, jobDefinitionDatabaseService, meterRegistry);
        var uuid = UUID.randomUUID();
        var jobContext = createJobContext(uuid, List.of(), JobType.GENERIC_INTEGRATION_JOB);
        JobStatus status = jobRunner.run(jobContext, testEtlProcessor);
        assertThat(status).isEqualTo(JobStatus.SUCCESS);
        verify(testEtlProcessor).postProcess(any(), any());
        verify(testEtlProcessor).preProcess(any(), any());
        verify(testEtlProcessor, times(1)).getJobStages();
        verify(testStage, times(1)).process(any(), any());
        assertThat(testJobState.getName()).isEqualTo("testing testing");
        assertThat(testJobState.getListPopulatedBeforeJob()).isEqualTo(List.of(1, 2, 3, 4));
    }
}
