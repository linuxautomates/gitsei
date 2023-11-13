package io.levelops.ingestion.integrations.template.services;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.integrations.template.models.TemplateScanQuery;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.ingestion.services.IngestionService;
import io.levelops.ingestion.models.IntegrationType;

import lombok.Builder;
import javax.annotation.Nullable;

public class TemplateIngestionService extends BaseIngestionService implements IngestionService {

    private static final String CONTROLLER_NAME = "TemplateIngestionService";

    @Builder
    public TemplateIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, CONTROLLER_NAME);
    }

    public SubmitJobResponse listProjects(IntegrationKey integrationKey, @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(callbackUrl, integrationKey, TemplateScanQuery.builder()
                .integrationKey(integrationKey)
                .build());
    }

    @Override
    public IntegrationType getIntegrationType() {
        // return IntegrationType.TEMPLATE;
        return null;
    }

}