package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.gerrit.client.GerritClient;
import io.levelops.integrations.gerrit.client.GerritClientException;
import io.levelops.integrations.gerrit.client.GerritClientFactory;
import io.levelops.integrations.gerrit.models.GerritQuery;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Implementation of the {@link PreflightCheck} for Tenable integration
 */
@Log4j2
@Component
public class GerritPreflightCheck implements PreflightCheck {

    private static final String GERRIT = "gerrit";

    private final GerritClientFactory clientFactory;

    @Autowired
    public GerritPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.clientFactory = GerritClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link GerritPreflightCheck#GERRIT}
     */
    @Override
    public String getIntegrationType() {
        return GERRIT;
    }

    /**
     * checks the validity of {@link Integration} and {@link Token} by calling list projects, list accounts, groups, changes
     *
     * @param tenantId {@link String} id of the tenant for which the {@code integration} is being validated
     * @param integration {@link Integration} to validate
     * @param token {@link Token} containing the credentials for the {@code integration}
     * @return {@link PreflightCheckResults} containing {@link PreflightCheckResult}
     */
    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        GerritClient client;
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
        builder.check(checkProjects(client));
        builder.check(checkGroups(client));
        builder.check(checkAccounts(client));
        builder.check(checkChanges(client));
        return builder.build();
    }

    /**
     * validates the response from {@link GerritClient#getProjects(Integer, Integer)}
     *
     * @param client {@link GerritClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkProjects(GerritClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/projects")
                .success(true);
        try {
            var projects = client.getProjects(0, 1);
            if (projects == null) {
                checkResultBuilder.success(false).error("response from /projects returned null result");
            }
        } catch (GerritClientException e) {
            log.error("checkProjects: encountered error while getting projects: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link GerritClient#getGroups(Integer, Integer)}
     *
     * @param client {@link GerritClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkGroups(GerritClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/groups")
                .success(true);
        try {
            var groups = client.getGroups(0, 1);
            if (groups == null) {
                checkResultBuilder.success(false).error("response from /groups returned null result");
            }
        } catch (GerritClientException e) {
            log.error("checkProjects: encountered error while getting groups : " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link GerritClient#getAccounts(Integer, Integer)}
     *
     * @param client {@link GerritClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkAccounts(GerritClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/accounts")
                .success(true);
        try {
            var accounts = client.getAccounts(0, 1);
            if (accounts == null) {
                checkResultBuilder.success(false).error("response from /accounts returned null result");
            }
        } catch (GerritClientException e) {
            log.error("checkProjects: encountered error while getting accounts : " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link GerritClient#getChanges(Integer, Integer, GerritQuery)}
     *
     * @param client {@link GerritClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkChanges(GerritClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/changes")
                .success(true);
        try {
            var changes = client.getChanges(0, 1, GerritQuery.builder()
                    .after(new Date())
                    .integrationKey(IntegrationKey.builder().build())
                    .build());
            if (changes == null) {
                checkResultBuilder.success(false).error("response from /changes returned null result");
            }
        } catch (GerritClientException e) {
            log.error("checkProjects: encountered error while getting changes : " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }
}
