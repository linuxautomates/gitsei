package io.levelops.ingestion.integrations.pagerduty.services;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.pagerduty.models.PagerDutyIterativeScanQuery;
import lombok.Builder;

import javax.annotation.Nullable;

public class PagerDutyIngestionService extends BaseIngestionService {

    private static final String CONTROLLER_NAME = "PagerDutyIngestionService";

    @Builder
    public PagerDutyIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, CONTROLLER_NAME);
    }

    public SubmitJobResponse listProjects(IntegrationKey integrationKey, @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(callbackUrl, integrationKey, PagerDutyIterativeScanQuery.builder()
                .integrationKey(integrationKey)
                .build());
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.PAGERDUTY;
    }

}