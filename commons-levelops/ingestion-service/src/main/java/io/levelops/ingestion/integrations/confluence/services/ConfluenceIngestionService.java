package io.levelops.ingestion.integrations.confluence.services;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.confluence.models.ConfluenceSearchQuery;

import java.util.List;

import javax.annotation.Nullable;

public class ConfluenceIngestionService extends BaseIngestionService {

    private static final String CONTROLLER_NAME = "ConfluenceSearchController";

    public ConfluenceIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, CONTROLLER_NAME);
    }

    public SubmitJobResponse search(IntegrationKey integrationKey, List<String> keywords, @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(callbackUrl, integrationKey, ConfluenceSearchQuery.builder()
                .integrationKey(integrationKey)
                .keywords(keywords)
                .build());
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.CONFLUENCE;
    }
}
