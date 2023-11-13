package io.levelops.ingestion.integrations.slack.services;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.integrations.slack.models.SlackChatInteractiveMessageQuery;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.ingestion.services.IngestionService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SlackInteractiveIngestionService extends BaseIngestionService implements IngestionService {
    private static final String CONTROLLER_NAME = "SlackChatInteractiveMessageController";

    public SlackInteractiveIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, CONTROLLER_NAME);
    }

    public SubmitJobResponse sendChatInteractiveMessage(IntegrationKey integrationKey, UUID workItemId, UUID questionnaireId, String text, final List<ImmutablePair<String, String>> fileUploads, List<String> recipients, @Nullable String botName, @Nullable String callbackUrl) throws IngestionServiceException {
        List<SlackChatInteractiveMessageQuery.FileUpload> fileUploadList = (CollectionUtils.isNotEmpty(fileUploads)) ? fileUploads.stream().map(x -> SlackChatInteractiveMessageQuery.FileUpload.builder().fileName(x.getLeft()).fileContent(x.getRight()).build()).collect(Collectors.toList()) : Collections.emptyList();
        return super.submitJob(callbackUrl, integrationKey, SlackChatInteractiveMessageQuery.builder()
                .integrationKey(integrationKey)
                .workItemId(workItemId).questionnaireId(questionnaireId)
                .text(text)
                .fileUploads(fileUploadList)
                .recipients(recipients)
                .botName(botName)
                .build());
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.SLACK;
    }
}
