package io.levelops.etl.clients;

import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.jackson.DefaultObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;


public class EtlSchedulerClientIntegrationTest {
    private OkHttpClient okHttpClient;
    private EtlSchedulerClient client;

    @Before
    public void setup() {
        okHttpClient = new OkHttpClient();
        client = new EtlSchedulerClient(okHttpClient, DefaultObjectMapper.get(), "http://localhost:8080/");
    }

    @Test
    public void testGetJobs() throws EtlSchedulerClient.SchedulerClientException {
        var jobs = client.getJobsToRun();
    }

    @Test
    public void testClaimJob() throws EtlSchedulerClient.SchedulerClientException {
        var jobs = client.getJobsToRun();
        var job = jobs.get(0);
        var result = client.claimJob(job.getJobInstanceId(), "myworker");
        System.out.println("Result = " + result);
    }

    @Test
    public void testUnclaimJob() throws EtlSchedulerClient.SchedulerClientException {
        var jobs = client.getJobsToRun();
        var job = jobs.get(0);
        JobInstanceId jobInstanceId = JobInstanceId.builder()
                .jobDefinitionId(UUID.fromString("b7d17b7b-9814-4051-a0de-3c7a97a61b32"))
                .instanceId(1)
                .build();
        var result = client.unclaimJob(jobInstanceId, "myworker");
        System.out.println("Result = " + result);
    }
}