package io.levelops.ingestion.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.models.SubmitJobResponse;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;

@AllArgsConstructor
public abstract class BaseIngestionService implements IngestionService {

    private final ControlPlaneService controlPlaneService;
    private final InventoryService inventoryService;
    private final String controllerName;

    public SubmitJobResponse submitJob(@Nullable String callbackUrl, @Nullable IntegrationKey integrationKey, DataQuery query) throws IngestionServiceException {
        return submitJob(controllerName, callbackUrl, integrationKey, query);
    }

    public SubmitJobResponse submitJob(String controllerName, @Nullable String callbackUrl, @Nullable IntegrationKey integrationKey, DataQuery query) throws IngestionServiceException {
        Validate.notBlank(controllerName, "controllerName cannot be null or empty.");
        return controlPlaneService.submitJob(CreateJobRequest.builder()
                .controllerName(controllerName)
                .query(query)
                .callbackUrl(callbackUrl)
                .tenantId(integrationKey != null ? integrationKey.getTenantId() : null)
                .integrationId(integrationKey != null ? integrationKey.getIntegrationId() : null)
                .reserved(isIntegrationReserved(integrationKey))
                .build());
    }

    public boolean isIntegrationReserved(@Nullable IntegrationKey integrationKey) throws IngestionServiceException {
        if (integrationKey == null) {
            return false;
        }
        Integration integration;
        try {
            integration = inventoryService.getIntegration(integrationKey);
        } catch (InventoryException e) {
            throw new IngestionServiceException("Failed to look up integration " + integrationKey, e);
        }
        if (integration == null) {
            throw new IngestionServiceException("Could not find integration " + integrationKey);
        }
        return BooleanUtils.isTrue(integration.getSatellite());
    }
}
