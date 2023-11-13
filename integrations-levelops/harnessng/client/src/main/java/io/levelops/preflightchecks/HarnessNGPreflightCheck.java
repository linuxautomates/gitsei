package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.harnessng.client.HarnessNGClient;
import io.levelops.integrations.harnessng.client.HarnessNGClientException;
import io.levelops.integrations.harnessng.client.HarnessNGClientFactory;
import io.levelops.integrations.harnessng.models.HarnessNGPipeline;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineExecution;
import io.levelops.integrations.harnessng.models.HarnessNGProject;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementation of the {@link PreflightCheck} for HarnessNG integration
 */
@Log4j2
@Component
public class HarnessNGPreflightCheck implements PreflightCheck {

    private static final String HARNESSNG = "harnessng";
    private final HarnessNGClientFactory clientFactory;
    private String accountIdentifier = "";

    @Autowired
    public HarnessNGPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        clientFactory = HarnessNGClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link io.levelops.preflightchecks.HarnessNGPreflightCheck#HARNESSNG}
     */
    @Override
    public String getIntegrationType() {
        return HARNESSNG;
    }

    /**
     * checks the validity of {@link Integration} and {@link Token} by calling list tickets,
     * list requests and jira links api. Validates successful response.
     *
     * @param tenantId    {@link String} id of the tenant for which the {@code integration} is being validated
     * @param integration {@link Integration} to validate
     * @param token       {@link Token} containing the credentials for the {@code integration}
     * @return {@link PreflightCheckResults} containing {@link PreflightCheckResult}
     */
    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        HarnessNGClient client;
        try {
            client = clientFactory.buildFromToken(tenantId, integration, token);
            accountIdentifier = integration.getMetadata().get("accountId").toString();
        } catch (InventoryException e) {
            log.error("check: error creating client for tenant: " + tenantId + " , integration: " + integration
                    + e.getMessage(), e);
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
        builder.check(checkProjects(client, builder));
        return builder.build();
    }

    /**
     * validates the response from {@link HarnessNGClient#getProjects}(HarnessNGIngestionQuery)}
     *
     * @param client {@link HarnessNGClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkProjects(HarnessNGClient client, PreflightCheckResults.PreflightCheckResultsBuilder builder) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/ng/api/projects")
                .success(true);
        try {
            List<HarnessNGProject> response = client.getProjects(accountIdentifier, 0);
            if (response == null){
                throw new HarnessNGClientException("response from /ng/api/projects returned null result");
            }
            else{
                if (!response.isEmpty()) {
                    HarnessNGProject project = response.get(0);
                    builder.check(checkPipelines(client, builder, project));
                }
                else{
                    throw new HarnessNGClientException("No projects found for given accountId. Access to at least one project is required.");
                }
            }
        } catch (HarnessNGClientException e) {
            log.error("checkProjects: encountered error while fetching projects: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link HarnessNGClient#getExecutions}(HarnessNGIngestionQuery)}
     *
     * @param client {@link HarnessNGClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkPipelines(HarnessNGClient client, PreflightCheckResults.PreflightCheckResultsBuilder builder, HarnessNGProject pro) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/pipeline/api/pipelines/execution/summary")
                .success(true);
        try {
            HarnessNGProject.Project project = pro.getProject();
            List<HarnessNGPipeline> response = client.getExecutions(accountIdentifier, project.getIdentifier(), project.getOrgIdentifier(), 0, null, null);
            if (response == null){
                throw new HarnessNGClientException("response from /pipeline/api/pipelines/execution/summary returned null result");
            }
            else {
                if (!response.isEmpty()) {
                    HarnessNGPipeline pipeline = response.get(0);
                    builder.check(checkPipelineExecutionDetails(client, pipeline, project));
                }
            }
        } catch (HarnessNGClientException e) {
            log.error("checkPipelines: encountered error while fetching pipelines: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link HarnessNGClient#getExecutionDetails}(HarnessNGIngestionQuery)}
     *
     * @param client {@link HarnessNGClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkPipelineExecutionDetails(HarnessNGClient client, HarnessNGPipeline pipeline, HarnessNGProject.Project project) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/pipeline/api/pipelines/execution/v2/{pipeline_Id}")
                .success(true);
        try {
            HarnessNGPipelineExecution response = client.getExecutionDetails(accountIdentifier, project.getIdentifier(), project.getOrgIdentifier(), pipeline.getExecutionId(), true);
            if (response == null){
                throw new HarnessNGClientException("response from /pipeline/api/pipelines/execution/v2/{pipeline_Id} returned null result");
            }
        } catch (HarnessNGClientException e) {
            log.error("checkPipelineExecutionDetails: encountered error while fetching pipeline execution details: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }
}
