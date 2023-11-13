package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.salesforce.client.SalesforceClient;
import io.levelops.integrations.salesforce.client.SalesforceClientException;
import io.levelops.integrations.salesforce.client.SalesforceClientFactory;
import io.levelops.integrations.salesforce.models.SOQLJobResponse;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link PreflightCheck} for Salesforce integration
 */
@Log4j2
@Component
public class SalesforcePreflightCheck implements PreflightCheck{

    private static final String SALESFORCE = "salesforce";

    private final SalesforceClientFactory clientFactory;
    private static final int RESPONSE_PAGE_SIZE = 1;

    @Autowired
    public SalesforcePreflightCheck(InventoryService inventoryService, ObjectMapper objectMapper,
                                    OkHttpClient okHttpClient) {
        this.clientFactory = SalesforceClientFactory.builder()
                .inventoryService(inventoryService)
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .pageSize(RESPONSE_PAGE_SIZE)
                .build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link SalesforcePreflightCheck#SALESFORCE}
     */
    @Override
    public String getIntegrationType() {
        return SALESFORCE;
    }

    /**
     * checks the validity of {@link Integration} and {@link Token} by submitting SOQL query request. Validates successful response.
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
        SalesforceClient client;
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
        builder.check(checkSOQLJobSubmit(client));
        return builder.build();
    }

    private PreflightCheckResult checkSOQLJobSubmit(SalesforceClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/services/data/v48.0/jobs/query")
                .success(true);
        try {
            SOQLJobResponse jobResponse = client.createQueryJob("query",
                    "SELECT Id, Account.Name, Contact.Email FROM Case LIMIT 1");
            if (!jobResponse.getState().equalsIgnoreCase("JobComplete")) {
                checkResultBuilder.success(false).error("Query job submission not completed");
            }
        } catch (SalesforceClientException | InterruptedException e) {
            log.error("Submit job: encountered error while submitting job : " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }
}
