package io.levelops.etl.job_framework;

import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.aggregations_shared.utils.IngestionResultPayloadUtils;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobType;
import io.levelops.etl.clients.EtlSchedulerClient;
import io.levelops.etl.engine.EtlEngine;
import io.levelops.etl.services.WorkerControlService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorkerControlServiceTest {
    @Mock
    JobInstanceDatabaseService jobInstanceDatabaseService;

    @Mock
    IngestionResultPayloadUtils ingestionResultPayloadUtils;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    //    @Test
    public void test() throws InterruptedException, EtlSchedulerClient.SchedulerClientException {
        EtlEngine ETLEngine = mock(EtlEngine.class);
        EtlSchedulerClient schedulerService = mock(EtlSchedulerClient.class);
        when(schedulerService.getJobsToRun()).thenReturn(List.of());
        WorkerControlService controlService = new WorkerControlService(
                ETLEngine, schedulerService, jobInstanceDatabaseService, "test", 2, 2, 10
        );

        Thread.sleep(3 * 1000);
        controlService.stopScheduling();
        verify(schedulerService, times(2)).getJobsToRun();
        verify(ETLEngine, times(2)).recordHeartbeatForAllThreads();
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
    public void testMaxNewJobsInOneCycle() throws EtlSchedulerClient.SchedulerClientException, InterruptedException {
        EtlEngine ETLEngine = mock(EtlEngine.class);
        EtlSchedulerClient schedulerService = mock(EtlSchedulerClient.class);
        EtlEngine.EngineJob engineJob = mock(EtlEngine.EngineJob.class);
        when(ETLEngine.canRunJob(any())).thenReturn(true);
        when(ETLEngine.canAcceptJobs()).thenReturn(true);
        when(ETLEngine.submitJob(any())).thenReturn(Optional.of(engineJob));
        when(schedulerService.getJobsToRun()).thenReturn(List.of(createJobContext(), createJobContext(), createJobContext()));
        when(schedulerService.claimJob(any(), any())).thenReturn(true);
        WorkerControlService controlService = new WorkerControlService(
                ETLEngine, schedulerService, jobInstanceDatabaseService, "test", 0, 0, 1
        );
        controlService.fetchAndRunJobs();
        verify(ETLEngine, times(1)).submitJob(any());
    }
}