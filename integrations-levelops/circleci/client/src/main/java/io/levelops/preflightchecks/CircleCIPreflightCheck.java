package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.circleci.client.CircleCIClient;
import io.levelops.integrations.circleci.client.CircleCIClientException;
import io.levelops.integrations.circleci.client.CircleCIClientFactory;
import io.levelops.integrations.circleci.models.CircleCIBuild;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementation of the {@link PreflightCheck} for CircleCI integration
 */
@Log4j2
@Component
public class CircleCIPreflightCheck implements PreflightCheck {

    private static final String CIRCLECI = "circleci";
    private final CircleCIClientFactory clientFactory;

    @Autowired
    public CircleCIPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        clientFactory = CircleCIClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link io.levelops.preflightchecks.CircleCIPreflightCheck#CIRCLECI}
     */
    @Override
    public String getIntegrationType() {
        return CIRCLECI;
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
        CircleCIClient client;
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
        builder.check(checkBuilds(client, builder));
        return builder.build();
    }

    /**
     * validates the response from {@link CircleCIClient#getRecentBuilds}(CircleCIIngestionQuery)}
     *
     * @param client {@link CircleCIClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkBuilds(CircleCIClient client, PreflightCheckResults.PreflightCheckResultsBuilder builder) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/builds")
                .success(true);
        try {
            List<CircleCIBuild> response = client.getRecentBuilds(0);
            if (response == null){
                checkResultBuilder.success(false).error("response from /builds returned null result");
            }
        } catch (CircleCIClientException e) {
            log.error("checkTicket: encountered error while fetching builds: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }
}
