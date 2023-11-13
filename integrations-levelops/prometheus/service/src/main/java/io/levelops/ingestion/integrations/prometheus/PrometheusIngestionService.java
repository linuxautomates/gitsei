package io.levelops.ingestion.integrations.prometheus;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.prometheus.models.PrometheusIngestionQuery;
import io.levelops.integrations.prometheus.models.PrometheusRunbookQueryRequest;
import org.springframework.stereotype.Service;

@Service
public class PrometheusIngestionService extends BaseIngestionService {

    private static final String PROMETHEUS_INGESTION_CONTROLLER = "PrometheusIngestionController";

    public PrometheusIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, PROMETHEUS_INGESTION_CONTROLLER);
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.PROMETHEUS;
    }

    public SubmitJobResponse queryData(String tenantId, String integrationId,
                                       PrometheusRunbookQueryRequest queryRequest, String callbackUrl)
            throws IngestionServiceException {
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integrationId)
                .build();
        return super.submitJob(callbackUrl, integrationKey, PrometheusIngestionQuery.builder()
                .integrationKey(integrationKey)
                .update(true)
                .queryRequest(queryRequest)
                .build());
    }
}
