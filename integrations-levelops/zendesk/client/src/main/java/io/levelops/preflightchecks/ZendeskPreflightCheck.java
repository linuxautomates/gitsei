package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.zendesk.client.ZendeskClient;
import io.levelops.integrations.zendesk.client.ZendeskClientException;
import io.levelops.integrations.zendesk.client.ZendeskClientFactory;
import io.levelops.integrations.zendesk.models.*;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import static io.levelops.integrations.zendesk.client.ZendeskClientFactory.JIRALINKS_ENABLED;

/**
 * Implementation of the {@link PreflightCheck} for Zendesk integration
 */
@Log4j2
@Component
public class ZendeskPreflightCheck implements PreflightCheck {

    private static final String ZENDESK = "zendesk";

    private static final int RESPONSE_PAGE_SIZE = 1;

    private final ZendeskClientFactory clientFactory;

    @Autowired
    public ZendeskPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        clientFactory = ZendeskClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .pageSize(RESPONSE_PAGE_SIZE)
                .build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link ZendeskPreflightCheck#ZENDESK}
     */
    @Override
    public String getIntegrationType() {
        return ZENDESK;
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
        ZendeskClient client;
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
        builder.check(checkTicket(client));
        builder.check(checkField(client));
        boolean jiraLinksEnabled = integration.getMetadata() != null && (Boolean) integration.getMetadata().getOrDefault(JIRALINKS_ENABLED, false);
        if (jiraLinksEnabled)
            builder.check(checkJiraLinks(client));
        builder.check(checkRequests(client));
        return builder.build();
    }

    /**
     * validates the response from {@link ZendeskClient#getTickets(ZendeskTicketQuery)}
     *
     * @param client {@link ZendeskClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkTicket(ZendeskClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/tickets")
                .success(true);
        try {
            ExportTicketsResponse response = client.getTickets(ZendeskTicketQuery.builder()
                    .from(Date.from(new Date().toInstant().minus(1, ChronoUnit.DAYS)))
                    .build());
            if (response == null || response.getTickets() == null)
                checkResultBuilder.success(false).error("response from /tickets returned null result");
        } catch (ZendeskClientException e) {
            log.error("checkTicket: encountered error while fetching tickets: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    private PreflightCheckResult checkField(ZendeskClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/ticket_fields")
                .success(true);
        try {
            ListTicketFieldsResponse response = client.getTicketFields();
            if (response == null || response.getFields() == null)
                checkResultBuilder.success(false).error("response from /ticket_fields returned null result");
        } catch (ZendeskClientException e) {
            log.error("checkTicket: encountered error while fetching fields: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link ZendeskClient#getJiraLinks()}
     *
     * @param client {@link ZendeskClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkJiraLinks(ZendeskClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/jiraLinks")
                .success(true);
        try {
            GetJiraLinkResponse response = client.getJiraLinks();
            if (response == null || response.getLinks() == null)
                checkResultBuilder.success(false).error("response from /jira/links returned null result");
        } catch (ZendeskClientException e) {
            log.error("checkJiraLinks: encountered error while fetching tickets: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link ZendeskClient#getRequestAttributes()}
     *
     * @param client {@link ZendeskClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkRequests(ZendeskClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/requests")
                .success(true);
        try {
            ListRequestsResponse response = client.getRequestAttributes();
            if (response == null || response.getRequestAttributes() == null)
                checkResultBuilder.success(false).error("response from /requests returned null result");
        } catch (ZendeskClientException e) {
            log.error("checkRequests: encountered error while fetching tickets: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }
}
