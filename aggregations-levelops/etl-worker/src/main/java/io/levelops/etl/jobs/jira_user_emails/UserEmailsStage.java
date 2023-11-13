package io.levelops.etl.jobs.jira_user_emails;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionUpdate;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.etl.job_framework.GenericJobProcessingStage;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Log4j2
@Service
public class UserEmailsStage implements GenericJobProcessingStage<JiraUserEmailsJobState> {
    private static String USER_EMAIL_STAGE_METADATA_KEY = "user_email_stage_metadata";
    private final ObjectMapper objectMapper;
    private final JiraUserEmailsService jiraUserEmailsService;
    private final JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private final InventoryService inventoryService;

    public UserEmailsStage(
            ObjectMapper objectMapper,
            JiraUserEmailsService jiraUserEmailsService,
            InventoryService inventoryService,
            JobDefinitionDatabaseService jobDefinitionDatabaseService) {
        this.objectMapper = objectMapper;
        this.jiraUserEmailsService = jiraUserEmailsService;
        this.inventoryService = inventoryService;
        this.jobDefinitionDatabaseService = jobDefinitionDatabaseService;
    }

    @Builder(toBuilder = true)
    @Data
    @JsonDeserialize(builder = UserEmailStageMetadata.UserEmailStageMetadataBuilder.class)
    public static class UserEmailStageMetadata {
        @JsonProperty("job_ids")
        private List<String> jobIds;
    }

    private UserEmailStageMetadata getMetadata(JobContext context) {
        DbJobDefinition jobDefinition = jobDefinitionDatabaseService.get(context.getJobInstanceId().getJobDefinitionId()).get();
        var metadata = jobDefinition.getMetadata();
        if (metadata.containsKey(USER_EMAIL_STAGE_METADATA_KEY)) {
            return objectMapper.convertValue(metadata.get(USER_EMAIL_STAGE_METADATA_KEY), UserEmailStageMetadata.class);
        }
        return UserEmailStageMetadata.builder().build();
    }

    private void setMetadata(JobContext context, UserEmailStageMetadata userEmailStageMetadata) throws JsonProcessingException {
        DbJobDefinition jobDefinition = jobDefinitionDatabaseService.get(context.getJobInstanceId().getJobDefinitionId()).get();
        var metadata = jobDefinition.getMetadata();
        metadata.put(USER_EMAIL_STAGE_METADATA_KEY, userEmailStageMetadata);
        jobDefinitionDatabaseService.update(DbJobDefinitionUpdate.builder()
                .metadata(metadata)
                .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                        .id(context.getJobInstanceId().getJobDefinitionId())
                        .build())
                .build());
    }

    private boolean isJiraConnectIntegration(String integrationId, String tenantId) {
        try {
            Integration integration = inventoryService.getIntegration(tenantId, integrationId);
            return integration.getAuthentication().equals(Integration.Authentication.ATLASSIAN_CONNECT_JWT);
        } catch (InventoryException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void process(JobContext context, JiraUserEmailsJobState jobState) {
        // Only run this stage for jira connect apps, since the email apis are only available for jira connect apps
        boolean isJiraConnect = isJiraConnectIntegration(context.getIntegrationId(), context.getTenantId());
        if (!isJiraConnect) {
            log.info("Skipping user email stage because the integration is not a jira connect app. " +
                    "Tenant: {}, integration id: {}", context.getTenantId(), context.getIntegrationId());
            return;
        }

        //  Check if the job ids list is present in the job definition metadata
        UserEmailStageMetadata metadata = getMetadata(context);
        if (CollectionUtils.isNotEmpty(metadata.getJobIds())) {
            List<String> jobIds = metadata.getJobIds();
            log.info("Processing ingestion job ids: {}", jobIds);
            try {
                List<String> successfullyProcessedJobIds =
                        jiraUserEmailsService.processIngestionJobs(jobIds, context.getTenantId(), context.getIntegrationId());
                jobIds.removeAll(successfullyProcessedJobIds);
                UserEmailStageMetadata updatedMetadata = metadata.toBuilder()
                        .jobIds(jobIds)
                        .build();
                setMetadata(context, updatedMetadata);
            } catch (IngestionServiceException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            // We need to create the ingestion jobs because no ingestion jobs exist in the metadata
            List<String> userIdsPayload;
            if (context.getIsFull()) {
                userIdsPayload = jiraUserEmailsService.getFullPayload(context);
            } else {
                userIdsPayload = jiraUserEmailsService.getIncrementalPayload(context);
            }
            log.info("Creating ingestion jobs for fetching emails for {} user ids", userIdsPayload.size());
            try {
                List<String> ingestionJobIds = jiraUserEmailsService.createIngestionJobs(userIdsPayload, context);
                log.info("Created ingestion jobs with ids: {}", ingestionJobIds);
                UserEmailStageMetadata updatedMetadata = metadata.toBuilder()
                        .jobIds(ingestionJobIds)
                        .build();
                setMetadata(context, updatedMetadata);
            } catch (IngestionServiceException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getName() {
        return "UserEmailsStage";
    }

    @Override
    public void preStage(JobContext context, JiraUserEmailsJobState jobState) throws SQLException {

    }

    @Override
    public void postStage(JobContext context, JiraUserEmailsJobState jobState) throws SQLException {

    }
}
