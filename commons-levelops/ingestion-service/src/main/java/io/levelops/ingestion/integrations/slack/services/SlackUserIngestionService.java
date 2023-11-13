package io.levelops.ingestion.integrations.slack.services;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.integrations.slack.models.SlackUserQuery;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.ingestion.services.IngestionService;

import java.util.UUID;

public class SlackUserIngestionService extends BaseIngestionService implements IngestionService  {
    private static final String CONTROLLER_NAME = "SlackUserController";

    public SlackUserIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, CONTROLLER_NAME);
    }

    public SubmitJobResponse fetchUser(IntegrationKey integrationKey, UUID workItemNoteId, String slackUserId, String callbackUrl) throws IngestionServiceException {
        return super.submitJob(callbackUrl, integrationKey, SlackUserQuery.builder()
                .integrationKey(integrationKey)
                .workItemNoteId(workItemNoteId)
                .slackUserId(slackUserId)
                .build());
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.SLACK;
    }
}
