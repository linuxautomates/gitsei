package io.levelops.api.controllers.monitoring;

import io.levelops.commons.etl.EtlMonitoringClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/monitoring/etl")
@Log4j2
public class EtlMonitoringController {
    private final EtlMonitoringClient etlMonitoringClient;

    @Autowired
    public EtlMonitoringController(EtlMonitoringClient etlMonitoringClient) {
        this.etlMonitoringClient = etlMonitoringClient;
    }

    @GetMapping("/job_summary")
    public Map<String, Object> getJobDefinitionSummary(
            @RequestParam(value = "job_definition_id", required = false, defaultValue = "") String jobDefinitionId,
            @RequestParam(value = "tenant_id", required = false, defaultValue = "") String tenantId,
            @RequestParam(value = "integration_id", required = false, defaultValue = "") String integrationId
    ) throws EtlMonitoringClient.EtlMonitoringClientException {
        return etlMonitoringClient.getJobDefinitionSummary(jobDefinitionId, tenantId, integrationId);
    }
}
