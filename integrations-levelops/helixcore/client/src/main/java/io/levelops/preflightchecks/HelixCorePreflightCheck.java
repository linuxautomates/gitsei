package io.levelops.preflightchecks;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.helixcore.client.HelixCoreClient;
import io.levelops.integrations.helixcore.client.HelixCoreClientException;
import io.levelops.integrations.helixcore.client.HelixCoreClientFactory;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link PreflightCheck} for Tenable integration
 */
@Log4j2
@Component
public class HelixCorePreflightCheck implements PreflightCheck {

    public static final String HELIXCORE = "helixcore";

    private final HelixCoreClientFactory clientFactory;

    @Autowired
    public HelixCorePreflightCheck() {
        this.clientFactory = HelixCoreClientFactory.builder().build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link HelixCorePreflightCheck#HELIXCORE}
     */
    @Override
    public String getIntegrationType() {
        return HELIXCORE;
    }

    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        HelixCoreClient client;
        try {
            client = clientFactory.get(integrationKey);
        } catch (HelixCoreClientException e) {
            log.error("check: error creating client for tenant: " + tenantId + " , integration: " + integration
                    + e.getMessage(), e);
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
        builder.check(checkDepots(client));
        builder.check(checkChangeLists(client));
        return builder.build();
    }

    /**
     * validates the response from {@link HelixCoreClient#getDepots()}
     *
     * @param client {@link HelixCoreClient} with authentication
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkDepots(HelixCoreClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/Depots")
                .success(true);
        try {
            var depots = client.getDepots();
            if (depots == null) {
                checkResultBuilder.success(false).error("response from /depots returned null result");
            }
        } catch (HelixCoreClientException e) {
            log.error("checkDepots: encountered error while getting depots: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link HelixCoreClient#getDepots()}
     *
     * @param client {@link HelixCoreClient} with authentication
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkChangeLists(HelixCoreClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/ChangeLists")
                .success(true);
        try {
            var changeLists = client.getChangeLists(1);
            if (changeLists == null) {
                checkResultBuilder.success(false).error("response from /changeLists returned null result");
            }
        } catch (HelixCoreClientException e) {
            log.error("checkChangeLists: encountered error while getting changeLists: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }
}
