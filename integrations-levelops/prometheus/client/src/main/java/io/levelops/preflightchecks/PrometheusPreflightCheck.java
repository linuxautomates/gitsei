package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.prometheus.client.PrometheusClient;
import io.levelops.integrations.prometheus.client.PrometheusClientException;
import io.levelops.integrations.prometheus.client.PrometheusClientFactory;
import io.levelops.integrations.prometheus.models.PrometheusQueryRequest;
import io.levelops.integrations.prometheus.models.PrometheusQueryResponse;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Log4j2
@Component
public class PrometheusPreflightCheck implements PreflightCheck {

    private static final String PROMETHEUS = "prometheus";
    private static final String QUERY_STRING = "up";
    private static final String STEP = "5s";

    private final PrometheusClientFactory clientFactory;

    @Autowired
    public PrometheusPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        clientFactory = PrometheusClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .build();
    }

    @Override
    public String getIntegrationType() {
        return PROMETHEUS;
    }

    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        PrometheusClient client;
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
        builder.check(checkQueryRange(client));
        builder.check(checkInstantQuery(client));
        return builder.build();
    }

    private PreflightCheckResult checkQueryRange(PrometheusClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/query_range")
                .success(true);
        try {
            long currentTimestamp = Instant.now().getEpochSecond();
            long startTime = currentTimestamp - 10;
            Date startDate = new Date(startTime);
            Date endDate = new Date(currentTimestamp);
            PrometheusQueryResponse response = client.runRangeQuery(PrometheusQueryRequest.builder()
                    .queryString(QUERY_STRING)
                    .startTime(startDate)
                    .endTime(endDate)
                    .step(STEP).build());
            if (response == null || response.getData() == null)
                checkResultBuilder.success(false).error("response from /query_range returned null result");
        } catch (PrometheusClientException e) {
            log.error("checkQueryRange: encountered error while querying prometheus: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    private PreflightCheckResult checkInstantQuery(PrometheusClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/query")
                .success(true);
        try {
            PrometheusQueryResponse response = client.runInstantQuery(PrometheusQueryRequest.builder()
                    .queryString(QUERY_STRING)
                    .build());
            if (response == null || response.getData() == null)
                checkResultBuilder.success(false).error("response from /query returned null result");
        } catch (PrometheusClientException e) {
            log.error("checkInstanceQuery: encountered error while querying prometheus: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }
}
