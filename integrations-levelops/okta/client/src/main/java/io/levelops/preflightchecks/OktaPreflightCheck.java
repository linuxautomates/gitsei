package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.okta.client.OktaClient;
import io.levelops.integrations.okta.client.OktaClientException;
import io.levelops.integrations.okta.client.OktaClientFactory;
import io.levelops.integrations.okta.models.*;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementation of the {@link PreflightCheck} for Okta integration
 */
@Log4j2
@Component
public class OktaPreflightCheck implements PreflightCheck {

    private static final String OKTA = "okta";
    private final OktaClientFactory clientFactory;
    private static final int RESPONSE_PAGE_SIZE = 1;

    @Autowired
    public OktaPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.clientFactory = OktaClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .pageSize(RESPONSE_PAGE_SIZE)
                .build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link OktaPreflightCheck#OKTA}
     */
    @Override
    public String getIntegrationType() {
        return OKTA;
    }

    /**
     * checks the validity of {@link Integration} and {@link Token} by calling list users,
     * list groups, list user types. Validates successful response.
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
        OktaClient client;
        try {
            client = clientFactory.buildFromToken(tenantId, integration, token);
        } catch (InventoryException e) {
            log.error("check: error creating client for okta: " + tenantId + " , integration: " + integration
                    + e.getMessage(), e);
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
        builder.check(checkUsers(client));
        builder.check(checkGroups(client));
        builder.check(checkUserTypes(client));
        return builder.build();
    }

    private PreflightCheckResult checkUsers(OktaClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/users")
                .success(true);
        try {
            PaginatedOktaResponse<OktaUser> usersResponse = client.getUsers(OktaScanQuery.builder().build());
            if(usersResponse == null || usersResponse.getValues() == null) {
                checkResultBuilder.success(false).error("response from /usersResponse returned null result");
            }
        } catch (OktaClientException e) {
            log.error("checkUsers: encountered error while getting scanner detail: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    private PreflightCheckResult checkGroups(OktaClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/groups")
                .success(true);
        try {
            PaginatedOktaResponse<OktaGroup> groupsResponse = client.getGroups(OktaScanQuery.builder().build());
            if(groupsResponse == null || groupsResponse.getValues() == null) {
                checkResultBuilder.success(false).error("response from /groups returned null result");
            }
        } catch (OktaClientException e) {
            log.error("checkUsers: encountered error while getting scanner detail: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    private PreflightCheckResult checkUserTypes(OktaClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/userTypes")
                .success(true);
        try {
            List<OktaUserType> userTypes = client.getUserTypes();
            if(userTypes == null) {
                checkResultBuilder.success(false).error("response from /usertypes returned null result");
            }
        } catch (OktaClientException e) {
            log.error("checkUserTypes: encountered error while getting scanner detail: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    private PreflightCheckResult checkLinkedObjectDefinitions(OktaClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/linkedObjects")
                .success(true);
        try {
            List<OktaLinkedObject> linkedObjectDefinitions = client.getLinkedObjectDefinitions();
            if(linkedObjectDefinitions == null) {
                checkResultBuilder.success(false).error("response from /linkedObjects returned null result");
            }
        } catch (OktaClientException e) {
            log.error("checkLinkedObjectDefinitions: encountered error while getting scanner detail: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }
}
