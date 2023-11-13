package io.levelops.ingestion.integrations.github.services;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.integrations.github.models.GithubCreateWebhookQuery;
import io.levelops.ingestion.integrations.github.models.GithubQuery;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;

import javax.annotation.Nullable;

public class GithubIngestionService extends BaseIngestionService {

    private static final String CONTROLLER_NAME = "GithubController";
    private static final String WEBHOOK_CONTROLLER_NAME = "GithubWebhookController";
    private static final String GET_PR_CONTROLLER_NAME = "GithubPRController";

    public GithubIngestionService(ControlPlaneService controlPlaneService, InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, CONTROLLER_NAME);
    }

    public SubmitJobResponse listRepositories(IntegrationKey integrationKey, @Nullable String callbackUrl) throws IngestionServiceException {
        return submitJob(callbackUrl, integrationKey, GithubQuery.builder()
                        .integrationKey(integrationKey)
                        .build());
    }

    public SubmitJobResponse createWebhook(IntegrationKey integrationKey, GithubCreateWebhookQuery query,
                                           @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(WEBHOOK_CONTROLLER_NAME, callbackUrl, integrationKey, query);
    }

    public SubmitJobResponse getPullRequest(IntegrationKey integrationKey, String repoOwner, String repoName,
                                            String prNumber, @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(GET_PR_CONTROLLER_NAME, callbackUrl, integrationKey,
                GithubQuery.builder()
                        .integrationKey(integrationKey)
                        .repoOwner(repoOwner)
                        .repoName(repoName)
                        .prNumber(prNumber)
                        .build());
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.GITHUB;
    }
}
