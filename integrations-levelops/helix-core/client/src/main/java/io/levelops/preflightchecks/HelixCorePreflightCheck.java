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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Implementation of the {@link PreflightCheck} for Tenable integration
 */
@Log4j2
@Component
public class HelixCorePreflightCheck implements PreflightCheck {

    private static final int MAX_FILE_SIZE = 1000000;
    public static final String HELIX_CORE = "helix_core";

    private final HelixCoreClientFactory clientFactory;

    @Autowired
    public HelixCorePreflightCheck() {
        this.clientFactory = HelixCoreClientFactory.builder().build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link HelixCorePreflightCheck#HELIX_CORE}
     */
    @Override
    public String getIntegrationType() {
        return HELIX_CORE;
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
        client = clientFactory.buildFromToken(tenantId, integration, token);
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
            ZoneId zoneId = ZoneId.of("UTC");
            Instant untilDate = Instant.now();
            Instant sinceDate = untilDate.minus(1, ChronoUnit.MINUTES);
            LocalDate specFrom = LocalDate.ofInstant(sinceDate, zoneId);
            LocalDate specTo = LocalDate.ofInstant(untilDate, zoneId);

            var changeLists = client.getChangeLists(specFrom, specTo, sinceDate, untilDate, MAX_FILE_SIZE);
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
