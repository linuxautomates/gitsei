package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.utils.ListUtils;
import io.levelops.integrations.droneci.client.DroneCIClient;
import io.levelops.integrations.droneci.client.DroneCIClientException;
import io.levelops.integrations.droneci.client.DroneCIClientFactory;
import io.levelops.integrations.droneci.models.DroneCIBuild;
import io.levelops.integrations.droneci.models.DroneCIBuildStage;
import io.levelops.integrations.droneci.models.DroneCIBuildStep;
import io.levelops.integrations.droneci.models.DroneCIBuildStepLog;
import io.levelops.integrations.droneci.models.DroneCIEnrichRepoData;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementation of the {@link PreflightCheck} for DroneCI integration
 */
@Log4j2
@Component
public class DroneCIPreflightCheck implements PreflightCheck {

    private static final String DRONECI = "droneci";
    private final DroneCIClientFactory clientFactory;

    @Autowired
    public DroneCIPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        clientFactory = DroneCIClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link io.levelops.preflightchecks.DroneCIPreflightCheck#DRONECI}
     */
    @Override
    public String getIntegrationType() {
        return DRONECI;
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
        DroneCIClient client;
        try {
            client = clientFactory.buildFromToken(tenantId, integration, token);
        } catch (InventoryException e) {
            log.error("check: error creating client for tenant: " + tenantId + " , integration: " + integration
                    + e.getMessage(), e);
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
        builder.check(checkRepos(client, builder));
        return builder.build();
    }

    /**
     * validates the response from {@link DroneCIClient#getRepositories}(DroneCIIngestionQuery)}
     *
     * @param client {@link DroneCIClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkRepos(DroneCIClient client, PreflightCheckResults.PreflightCheckResultsBuilder builder) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/repos")
                .success(true);
        try {
            List<DroneCIEnrichRepoData> response = client.getRepositories(1, 1);
            if (response == null){
                checkResultBuilder.success(false).error("response from /repos returned null result");
            }
            else{
                if (!response.isEmpty()) {
                    DroneCIEnrichRepoData repo = response.get(0);
                    builder.check(checkBuilds(client, builder, repo));
                }
            }
        } catch (DroneCIClientException e) {
            log.error("checkRepos: encountered error while fetching repos: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link DroneCIClient#getRepoBuilds}(DroneCIIngestionQuery)}
     *
     * @param client {@link DroneCIClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkBuilds(DroneCIClient client, PreflightCheckResults.PreflightCheckResultsBuilder builder, DroneCIEnrichRepoData repo) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/builds")
                .success(true);
        try {
            List<DroneCIBuild> response = client.getRepoBuilds(repo.getNamespace(), repo.getName(), 1, 1);
            if (response == null){
                checkResultBuilder.success(false).error("response from /builds returned null result");
            }
            else {
                if (!response.isEmpty()) {
                    DroneCIBuild build = response.get(0);
                    builder.check(checkBuildEnrichment(client, builder, repo, build));
                }
            }
        } catch (DroneCIClientException e) {
            log.error("checkBuilds: encountered error while fetching builds: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link DroneCIClient#getBuildInfo}(DroneCIIngestionQuery)}
     *
     * @param client {@link DroneCIClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkBuildEnrichment(DroneCIClient client, PreflightCheckResults.PreflightCheckResultsBuilder builder, DroneCIEnrichRepoData repo, DroneCIBuild build) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/builds/{buildNumber}")
                .success(true);
        try {
            DroneCIBuild response = client.getBuildInfo(repo.getNamespace(), repo.getName(), build.getNumber());
            if (response == null){
                checkResultBuilder.success(false).error("response from /builds/{buildNumber} returned null result");
            }
            else {
                if(!ListUtils.isEmpty(ListUtils.emptyIfNull(response.getStages()))){
                    DroneCIBuildStage buildStage = response.getStages().get(0);
                    if(!ListUtils.isEmpty(ListUtils.emptyIfNull(buildStage.getSteps()))) {
                        DroneCIBuildStep buildStep = buildStage.getSteps().get(0);
                        builder.check(checkBuildStepLog(client, repo, build, buildStage, buildStep));
                    }
                }
            }
        } catch (DroneCIClientException e) {
            log.error("checkBuildEnrichment: encountered error while fetching builds: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link DroneCIClient#getBuildStepLogs}(DroneCIIngestionQuery)}
     *
     * @param client {@link DroneCIClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkBuildStepLog(DroneCIClient client, DroneCIEnrichRepoData repo, DroneCIBuild build, DroneCIBuildStage buildStage, DroneCIBuildStep buildStep){
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/build/stage/step/logs")
                .success(true);
        try {
            long buildNumber = build.getNumber();
            long buildStageNumber = buildStage.getNumber();
            long buildStepNumber = buildStep.getNumber();
            List<DroneCIBuildStepLog> response = client.getBuildStepLogs(repo.getNamespace(), repo.getName(), buildNumber, buildStageNumber, buildStepNumber);
            if (response == null){
                checkResultBuilder.success(false).error("response from /buildTestMetadata returned null result");
            }
        } catch (DroneCIClientException e) {
            log.error("checkBuildStepLog: encountered error while fetching buildTestMetadata: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }
}
