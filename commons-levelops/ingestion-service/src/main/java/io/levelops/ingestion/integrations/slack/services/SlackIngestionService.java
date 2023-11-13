package io.levelops.ingestion.integrations.slack.services;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.integrations.slack.models.SlackChatMessageQuery;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.ingestion.services.IngestionService;

import javax.annotation.Nullable;

import java.util.List;

public class SlackIngestionService extends BaseIngestionService implements IngestionService {
    private static final String CONTROLLER_NAME = "SlackChatMessageController";

    public SlackIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, CONTROLLER_NAME);
    }

    /**
     * Send chat message to one or more recipients.
     * @param recipients can contain channel ids, channel names, or user emails
     */
    public SubmitJobResponse sendChatMessage(IntegrationKey integrationKey, String text, List<String> recipients, @Nullable String botName, @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(callbackUrl, integrationKey, SlackChatMessageQuery.builder()
                .integrationKey(integrationKey)
                .text(text)
                .recipients(recipients)
                .botName(botName)
                .build());
    }

    @Deprecated
    public SubmitJobResponse sendChatMessageToUser(IntegrationKey integrationKey, String text, String userEmail, @Nullable String botName, @Nullable String callbackUrl) throws IngestionServiceException {
        return sendChatMessage(integrationKey, text, List.of(userEmail), botName, callbackUrl);
    }

    @Deprecated
    public SubmitJobResponse sendChatMessageToChannel(IntegrationKey integrationKey, String text, String channelId, @Nullable String botName, @Nullable String callbackUrl) throws IngestionServiceException {
        return sendChatMessage(integrationKey, text, List.of(channelId), botName, callbackUrl);
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.SLACK;
    }
}
