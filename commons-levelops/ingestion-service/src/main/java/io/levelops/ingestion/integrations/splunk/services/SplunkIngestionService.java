package io.levelops.ingestion.integrations.splunk.services;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.integrations.splunk.models.SplunkQuery;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.ingestion.services.IngestionService;

import javax.annotation.Nullable;

public class SplunkIngestionService extends BaseIngestionService implements IngestionService {
    private static final String CONTROLLER_NAME = "SplunkSearchController";

    public SplunkIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, CONTROLLER_NAME);
    }

    public SubmitJobResponse searchSplunk(IntegrationKey integrationKey, String query, @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(callbackUrl, integrationKey, SplunkQuery.builder()
                .integrationKey(integrationKey)
                .query(query)
                .limit(200) // No of records are limited to 200
                .build());
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.SPLUNK;
    }
}
