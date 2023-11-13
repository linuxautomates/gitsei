package io.levelops.ingestion.integrations.jira.services;

import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.integrations.jira.models.JiraCreateIssueQuery;
import io.levelops.ingestion.integrations.jira.models.JiraEditIssueQuery;
import io.levelops.ingestion.integrations.jira.models.JiraGetIssueQuery;
import io.levelops.ingestion.integrations.jira.models.JiraQuery;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;

import javax.annotation.Nullable;

public class JiraIngestionService extends BaseIngestionService {

    private static final String CONTROLLER_NAME = "JiraProjectController";
    private static final String CREATE_ISSUE_CONTROLLER = "JiraCreateIssueController";
    private static final String GET_ISSUE_CONTROLLER = "JiraGetIssueController";
    private static final String EDIT_ISSUE_CONTROLLER = "JiraEditIssueController";

    public JiraIngestionService(ControlPlaneService controlPlaneService,
                                InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, CONTROLLER_NAME);
    }

    public SubmitJobResponse listProjects(IntegrationKey integrationKey, @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(callbackUrl, integrationKey, JiraQuery.builder()
                .integrationKey(integrationKey)
                .build());
    }

    public SubmitJobResponse createIssue(IntegrationKey integrationKey, JiraCreateIssueQuery query, @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(CREATE_ISSUE_CONTROLLER , callbackUrl, integrationKey, query);
    }

    public SubmitJobResponse getIssue(IntegrationKey integrationKey, String issueKey, @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(GET_ISSUE_CONTROLLER, callbackUrl, integrationKey, JiraGetIssueQuery.builder()
                .integrationKey(integrationKey)
                .issueKey(issueKey)
                .build());
    }

    public SubmitJobResponse editIssue(IntegrationKey integrationKey, JiraEditIssueQuery query, @Nullable String callbackUrl) throws IngestionServiceException {
        return super.submitJob(EDIT_ISSUE_CONTROLLER , callbackUrl, integrationKey, query);
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.JIRA;
    }
}
