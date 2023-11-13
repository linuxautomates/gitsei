package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.etl.EtlMonitoringClient;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.ingestion.clients.IngestionClient;
import io.levelops.ingestion.models.controlplane.IngestionLogDTO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class MonitoringControllerTest {
    @Mock
    private IngestionClient ingestionClient;
    @Mock
    private EtlMonitoringClient etlMonitoringClient;

    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        objectMapper = DefaultObjectMapper.get();
    }


    @Test
    public void testGetIntegrationLogs() throws EtlMonitoringClient.EtlMonitoringClientException, InternalApiClientException {
        MonitoringController monitoringController = new MonitoringController(
                ingestionClient, etlMonitoringClient, objectMapper
        );
        when(ingestionClient.getIngestionLogs(eq("warriors"), eq("1"), any(), any()))
                .thenReturn(PaginatedResponse.of(1, 10, List.of(
                        IngestionLogDTO.builder()
                                .id("1")
                                .isEmptyResult(false)
                                .build(),
                        IngestionLogDTO.builder()
                                .id("2")
                                .isEmptyResult(true)
                                .build()
                )));
        var uuid1 = UUID.randomUUID();
        when(etlMonitoringClient.getJobInstances("warriors", "1", List.of("1")))
                .thenReturn(Map.of("1", DbJobInstance.builder()
                        .id(uuid1)
                        .status(JobStatus.SUCCESS)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .lastHeartbeat(Instant.now())
                        .aggProcessorName("test")
                        .tags(Set.of())
                        .build()));
        var response = monitoringController.getMonitoringLogsInternal(
                "warriors", "1", DefaultListRequest.builder()
                        .page(1)
                        .pageSize(10)
                        .build()
        );
        var records = response.getResponse().getRecords();
        assertThat(records.size()).isEqualTo(2);
        assertThat(records.get(0).getIngestionLogDTO().getId()).isEqualTo("1");
        assertThat(records.get(0).getEtlLogDTO().getId()).isEqualTo(uuid1.toString());
        assertThat(records.get(1).getIngestionLogDTO().getId()).isEqualTo("2");
        assertThat(records.get(1).getEtlLogDTO()).isNull();
    }
}