package io.levelops.ingestion.integrations.custom.k8s.services;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.integrations.custom.k8s.models.K8sCreatePodQuery;
import io.levelops.ingestion.integrations.custom.k8s.models.K8sExecQuery;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;

import javax.annotation.Nullable;

public class K8sIngestionService extends BaseIngestionService {

    private static final String EXEC_CONTROLLER = "K8sExecController";
    private static final String CREATE_POD_CONTROLLER = "K8sCreatePodController";

    public K8sIngestionService(ControlPlaneService controlPlaneService,
                               InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, null);
    }

    public SubmitJobResponse submitExecJob(IntegrationKey integrationKey, K8sExecQuery query, @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(EXEC_CONTROLLER, callbackUrl, integrationKey, query);
    }

    public SubmitJobResponse submitCreatePodJob(IntegrationKey integrationKey, K8sCreatePodQuery query, @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(CREATE_POD_CONTROLLER, callbackUrl, integrationKey, query);
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.CUSTOM;
    }
}
