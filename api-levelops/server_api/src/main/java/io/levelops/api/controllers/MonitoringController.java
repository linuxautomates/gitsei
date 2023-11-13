package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.model.IntegrationLog;
import io.levelops.commons.etl.EtlMonitoringClient;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.EtlLogDTO;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.ingestion.clients.IngestionClient;
import io.levelops.ingestion.models.controlplane.IngestionLogDTO;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequestMapping("/v1/monitoring")
public class MonitoringController {
    private final IngestionClient ingestionClient;
    private final EtlMonitoringClient etlMonitoringClient;
    private final ObjectMapper objectMapper;

    public MonitoringController(
            IngestionClient ingestionClient,
            EtlMonitoringClient etlMonitoringClient,
            ObjectMapper objectMapper
    ) {
        this.ingestionClient = ingestionClient;
        this.etlMonitoringClient = etlMonitoringClient;
        this.objectMapper = objectMapper;
    }

    public PaginatedResponse<IntegrationLog> getMonitoringLogsInternal(
            String tenantId,
            String integrationId,
            DefaultListRequest request
    ) throws InternalApiClientException, EtlMonitoringClient.EtlMonitoringClientException {
        PaginatedResponse<IngestionLogDTO> ingestionLogsResponse = ingestionClient
                .getIngestionLogs(tenantId, integrationId, request, true);
        List<IngestionLogDTO> ingestionLogs = ingestionLogsResponse.getResponse().getRecords();
        List<String> nonEmptyIngestionJobIds = ingestionLogs.stream()
                .filter(ingestionLogDTO -> BooleanUtils.isNotTrue(ingestionLogDTO.getIsEmptyResult()))
                .map(IngestionLogDTO::getId)
                .collect(Collectors.toList());
        Map<String, DbJobInstance> etlLogs = etlMonitoringClient.getJobInstances(tenantId, integrationId, nonEmptyIngestionJobIds);
        List<IntegrationLog> integrationLogs = ingestionLogs.stream()
                .map(ingestionLogDTO -> IntegrationLog.builder()
                        .ingestionLogDTO(ingestionLogDTO)
                        .etlLogDTO(
                                Optional.ofNullable(etlLogs.get(ingestionLogDTO.getId()))
                                        .map(jobInstance -> EtlLogDTO.fromDbJobInstance(objectMapper, jobInstance))
                                        .orElse(null))
                        .build())
                .collect(Collectors.toList());
        return PaginatedResponse.of(
                ingestionLogsResponse.getMetadata().getPage(),
                ingestionLogsResponse.getMetadata().getPageSize(),
                integrationLogs
        );
    }

    @PostMapping("{integrationId:[0-9]+}/logs")
    public DeferredResult<ResponseEntity<PaginatedResponse<IntegrationLog>>> getMonitoringLogs(
            @PathVariable("integrationId") String integrationId,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest request
    ) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                getMonitoringLogsInternal(company, integrationId, request)));
    }
}
