package io.levelops.ingestion.integrations.custom.rest.services;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.integrations.custom.rest.models.CustomRestCallQuery;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;

import javax.annotation.Nullable;

public class CustomIngestionService extends BaseIngestionService {

    private static final String REST_CALL_CONTROLLER = "CustomRestCallController";

    public CustomIngestionService(ControlPlaneService controlPlaneService,
                                  InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, null);
    }

    public SubmitJobResponse makeRestCall(IntegrationKey integrationKey, CustomRestCallQuery query, @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(REST_CALL_CONTROLLER, callbackUrl, integrationKey, query);
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.CUSTOM;
    }
}
