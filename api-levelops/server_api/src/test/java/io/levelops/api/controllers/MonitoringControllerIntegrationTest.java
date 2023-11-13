package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.etl.EtlMonitoringClient;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.ingestion.clients.IngestionClient;
import okhttp3.OkHttpClient;
import org.junit.Test;

public class MonitoringControllerIntegrationTest {
    @Test
    public void test() throws EtlMonitoringClient.EtlMonitoringClientException, InternalApiClientException {
        OkHttpClient okHttpClient = new OkHttpClient();
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        IngestionClient ingestionClient = new IngestionClient(okHttpClient, objectMapper, "http://localhost:8080");
        EtlMonitoringClient etlMonitoringClient = new EtlMonitoringClient(okHttpClient, objectMapper, "http://localhost:8082");
        MonitoringController monitoringController = new MonitoringController(
                ingestionClient, etlMonitoringClient, objectMapper
        );
        var results = monitoringController.getMonitoringLogsInternal("foo", "4323", DefaultListRequest.builder()
                .page(2)
                .pageSize(50)
                .build());
        for (var result : results.getResponse().getRecords()) {
            System.out.println(result);
        }
    }
}