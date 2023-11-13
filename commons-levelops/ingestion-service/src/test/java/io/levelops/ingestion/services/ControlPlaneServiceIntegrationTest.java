package io.levelops.ingestion.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.TriggerType;
import io.levelops.ingestion.models.controlplane.IngestionLogDTO;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import okhttp3.OkHttpClient;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;

public class ControlPlaneServiceIntegrationTest {

    ControlPlaneService controlPlaneService;

    @Before
    public void setUp() throws Exception {
        controlPlaneService = new ControlPlaneService(new OkHttpClient(), DefaultObjectMapper.get(), "http://localhost:8081", false);
    }

    @Test
    public void streamResults() {
        long total = controlPlaneService.streamTriggerResults(4, "3f1370f0-6657-4281-9381-f5d9466966c7", false, false, true)
                .map(JobDTO::getId)
                .peek(System.out::println)
                .count();
        System.out.println(total);
    }

    @Test
    public void testGetAllTriggerResults() throws IngestionServiceException, IOException {
        MultipleTriggerResults results = controlPlaneService.getAllTriggerResults(IntegrationKey.builder()
                .tenantId("foo")
                .integrationId("1492")
                .build(), false, false, true);
//        DefaultObjectMapper.prettyPrint(results);
        IOUtils.write(DefaultObjectMapper.writeAsPrettyJson(results), new FileOutputStream("/Users/mbellier/tmp"));
    }

    @Test
    public void testsStreamTriggers() {
//        long total = controlPlaneService.streamTriggers(null, null, null)
//                .peek(System.out::println)
//                .count();
//        System.out.println(total);

//        long total = controlPlaneService.streamTriggers("rubrik", null, null)
//                .peek(System.out::println)
//                .count();
//        System.out.println(total);

        long total = controlPlaneService.streamTriggers(null, null, TriggerType.JIRA)
                .peek(System.out::println)
                .count();
        System.out.println(total);
    }

    @Test
    public void testGetJobs() throws IngestionServiceException {
        var jobs = controlPlaneService.getJobs(
                        "foo",
                        0,
                        50,
                        "e2925f46-2a47-4d4a-b603-1b46f94dd5f7",
                        null,
                        null,
                        null,
                        null,
                        true)
                .getResponse()
                .getRecords();
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        var logs = jobs.stream().map(jobDTO -> {
            return IngestionLogDTO.fromJobDTO(objectMapper, jobDTO);
        }).collect(Collectors.toList());
        for (var log : logs) {
            System.out.println(log);
        }
    }
}