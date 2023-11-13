package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.tenable.client.TenableClient;
import io.levelops.integrations.tenable.client.TenableClientException;
import io.levelops.integrations.tenable.client.TenableClientFactory;
import io.levelops.integrations.tenable.models.NetworkResponse;
import io.levelops.integrations.tenable.models.ScannerPoolResponse;
import io.levelops.integrations.tenable.models.ScannerResponse;
import io.levelops.integrations.tenable.models.WASResponse;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link PreflightCheck} for Tenable integration
 */
@Log4j2
@Component
public class TenablePreflightCheck implements PreflightCheck {

    private static final String TENABLE = "tenable";

    private final TenableClientFactory clientFactory;
    private static final int RESPONSE_PAGE_SIZE = 1;

    @Autowired
    public TenablePreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.clientFactory = TenableClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .pageSize(RESPONSE_PAGE_SIZE)
                .build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link TenablePreflightCheck#TENABLE}
     */
    @Override
    public String getIntegrationType() {
        return TENABLE;
    }

    /**
     * checks the validity of {@link Integration} and {@link Token} by calling get scanners, get scanner groups,
     * get networks and get web application scanning vulnerabilities api. Validates successful response.
     * @param tenantId      {@link String} id of the tenant for which the {@code integration} is being validated
     * @param integration   {@link Integration} to validate
     * @param token         {@link Token} containing the credentials for the {@code integration}
     * @return              {@link PreflightCheckResults} containing {@link PreflightCheckResult}
     */
    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        TenableClient client;
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
        builder.check(checkScanner(client));
        builder.check(checkScannerGroup(client));
        builder.check(checkNetwork(client));
        builder.check(checkWASVulnerability(client));
        return builder.build();
    }

    /**
     * validates the response from {@link TenableClient#getScanners()}
     *
     * @param client {@link TenableClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkScanner(TenableClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/scanners")
                .success(true);
        try {
            ScannerResponse scanners = client.getScanners();
            if(scanners == null || scanners.getScanners() == null) {
                checkResultBuilder.success(false).error("response from /scanners returned null result");
            }
        } catch (TenableClientException e) {
            log.error("getScanners: encountered error while getting scanner detail: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link TenableClient#getScannerPools()}
     *
     * @param client {@link TenableClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkScannerGroup(TenableClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/scanner-groups")
                .success(true);
        try {
            ScannerPoolResponse scannerPools = client.getScannerPools();
            if(scannerPools == null || scannerPools.getScannerPools() == null) {
                checkResultBuilder.success(false).error("response from /scanner-groups returned null result");
            }
        } catch (TenableClientException e) {
            log.error("getScanners: encountered error while getting scanner pools detail: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link TenableClient#getNetworks(Integer, Integer)}
     *
     * @param client {@link TenableClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkNetwork(TenableClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/networks")
                .success(true);
        try {
            NetworkResponse networks = client.getNetworks(0, 1);
            if(networks == null || networks.getNetworks() == null) {
                checkResultBuilder.success(false).error("response from /networks returned null result");
            }
        } catch (TenableClientException e) {
            log.error("getScanners: encountered error while getting networks detail: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link TenableClient#getWasResponse(Integer, Integer, String)}
     *
     * @param client {@link TenableClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkWASVulnerability(TenableClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/was/v2/vulnerabilities")
                .success(true);
        try {
            WASResponse wasResponse = client.getWasResponse(0, 1, "desc");
            if(wasResponse == null || wasResponse.getData() == null) {
                checkResultBuilder.success(false).error("response from /was/v2/vulnerabilities returned null result");
            }
        } catch (TenableClientException e) {
            log.error("getWasResponse: encountered error while getting web app scanning vulnerabilities detail: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }
}
