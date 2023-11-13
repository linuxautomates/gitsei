package io.levelops.ingestion.integrations.zendesk.services;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.zendesk.models.TicketIngestionQuery;
import io.levelops.integrations.zendesk.models.TicketRequest;

public class ZendeskIngestionService extends BaseIngestionService {

    private static final String ZENDESK_INGESTION_CONTROLLER = "ZendeskIngestionController";

    public ZendeskIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, ZENDESK_INGESTION_CONTROLLER);
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.ZENDESK;
    }

    public SubmitJobResponse updateTicket(String tenantId, String integrationId, Long ticketId, TicketRequest ticketRequest,
                                          String callbackUrl)
            throws IngestionServiceException {
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integrationId)
                .build();
        return super.submitJob(callbackUrl, integrationKey, TicketIngestionQuery.builder()
                .integrationKey(integrationKey)
                .id(ticketId)
                .update(true)
                .ticketRequest(ticketRequest)
                .build());
    }

    public SubmitJobResponse insertTicket(String tenantId, String integrationId, TicketRequest ticket, String callbackUrl)
            throws IngestionServiceException {
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integrationId)
                .build();
        return super.submitJob(callbackUrl, integrationKey, TicketIngestionQuery.builder()
                .integrationKey(integrationKey)
                .update(false)
                .ticketRequest(ticket)
                .build());
    }
}
