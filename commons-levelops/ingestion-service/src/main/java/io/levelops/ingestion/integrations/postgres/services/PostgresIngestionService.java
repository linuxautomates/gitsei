package io.levelops.ingestion.integrations.postgres.services;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.integrations.postgres.models.PostgresQuery;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.ingestion.services.IngestionService;

import javax.annotation.Nullable;

public class PostgresIngestionService extends BaseIngestionService implements IngestionService {
    private static final String CONTROLLER_NAME = "PostgresController";

    public PostgresIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, CONTROLLER_NAME);
    }

    public SubmitJobResponse queryDb(IntegrationKey integrationKey, String server, String userName, String password, String databaseName, String query, @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(callbackUrl, integrationKey, PostgresQuery.builder()
                .integrationKey(integrationKey)
                .query(query)
                .build());
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.POSTGRES;
    }
}
