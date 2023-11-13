package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.pagerduty.client.PagerDutyClient;
import io.levelops.integrations.pagerduty.client.PagerDutyClientException;
import io.levelops.integrations.pagerduty.client.PagerDutyClientFactory;
import io.levelops.integrations.pagerduty.models.PagerDutyAlertsPage;
import io.levelops.integrations.pagerduty.models.PagerDutyIncidentsPage;
import io.levelops.integrations.pagerduty.models.PagerDutyLogEntriesPage;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import io.levelops.models.PreflightCheckResult.PreflightCheckResultBuilder;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

public class PagerDutyPreFlightCheck implements PreflightCheck {

    private final PagerDutyClientFactory clientFactory;

    @Autowired
    public PagerDutyPreFlightCheck(@NonNull final ObjectMapper objectMapper, @NonNull final OkHttpClient okHttpClient,
            @NonNull final InventoryService inventoryService) {
        this.clientFactory = PagerDutyClientFactory.builder().inventoryService(inventoryService)
                .objectMapper(objectMapper).okHttpClient(okHttpClient).build();
    }

    @Override
    public String getIntegrationType() {
        return IntegrationType.PAGERDUTY.toString();
    }

    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PagerDutyClient client;
        try {
            client = clientFactory
                    .get(IntegrationKey.builder().integrationId(integration.getId()).tenantId(tenantId).build());
        } catch (PagerDutyClientException e) {
            return PreflightCheckResults.builder()
                .exception("Unable to create a pager duty client: " + e.getMessage())
                .success(false)
                .build();
        }
        return PreflightCheckResults.builder()
            .tenantId(tenantId)
            .integration(integration)
            .allChecksMustPass()
            .check(checkAlerts(client))
            .check(checkIncidents(client))
            .check(checkLogEntries(client))
            .build();
    }

	private PreflightCheckResult checkAlerts(final PagerDutyClient client) {
        PreflightCheckResultBuilder result = PreflightCheckResult.builder()
            .name("/Alerts")
            .success(true);
        try {
            client.getPagerDutyAlerts(PagerDutyAlertsPage.Query.builder()
                .limit(1)
                .offset(0)
                .build());
        } catch (PagerDutyClientException e) {
            return result
                .error("Unable to access pager duty's alerts.")
                .exception(e.getMessage())
                .success(false)
                .build();
        }
		return null;
	}

	private PreflightCheckResult checkIncidents(final PagerDutyClient client) {
        PreflightCheckResultBuilder result = PreflightCheckResult.builder()
            .name("/Incidents")
            .success(true);
        try {
            client.getPagerDutyIncidents(PagerDutyIncidentsPage.Query.builder()
                .limit(1)
                .offset(0)
                .build());
        } catch (PagerDutyClientException e) {
            return result
                .error("Unable to access pager duty's incidents.")
                .exception(e.getMessage())
                .success(false)
                .build();
        }
		return null;
	}

	private PreflightCheckResult checkLogEntries(final PagerDutyClient client) {
        PreflightCheckResultBuilder result = PreflightCheckResult.builder()
            .name("/LogEntries")
            .success(true);
        try {
            client.getPagerDutyLogEntries(PagerDutyLogEntriesPage.Query.builder()
                .limit(1)
                .offset(0)
                .build());
            return result.build();
        } catch (PagerDutyClientException e) {
            return result
                .error("Unable to access pager duty's log entries.")
                .exception(e.getMessage())
                .success(false)
                .build();
        }
	}
    
}