package io.levelops.integrations.prometheus.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.prometheus.models.PrometheusQueryRequest;
import io.levelops.integrations.prometheus.models.PrometheusQueryResponse;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class PrometheusClientIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "prometheus1";
    private static final String APPLICATION = "prometheus";

    private static final String PROMETHEUS_URL = System.getenv("PROMETHEUS_URL");
    private static final String PROMETHEUS_QUERY_STRING = System.getenv("PROMETHEUS_QUERY_STRING");
    private static final String PROMETHEUS_QUERY_START_TIME = System.getenv("PROMETHEUS_QUERY_START_TIME");
    private static final String PROMETHEUS_QUERY_END_TIME = System.getenv("PROMETHEUS_QUERY_END_TIME");
    private static final String PROMETHEUS_QUERY_STEP = System.getenv("PROMETHEUS_QUERY_STEP");
    private static final String PROMETHEUS_USERNAME = "";
    private static final String PROMETHEUS_PASSWORD = "";

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private PrometheusClientFactory clientFactory;

    @Before
    public void setup() {
        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, PROMETHEUS_URL,
                        Collections.emptyMap(), PROMETHEUS_USERNAME, PROMETHEUS_PASSWORD)
                .build());
        clientFactory = PrometheusClientFactory.builder()
                .okHttpClient(client)
                .inventoryService(inventoryService)
                .objectMapper(DefaultObjectMapper.get())
                .build();
    }

    @Test
    public void runQueryRange() throws PrometheusClientException, ParseException {
        Date queryStartDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(PROMETHEUS_QUERY_START_TIME);
        Date queryEndDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(PROMETHEUS_QUERY_END_TIME);
        PrometheusQueryResponse response = clientFactory.get(TEST_INTEGRATION_KEY)
                .runRangeQuery(PrometheusQueryRequest.builder()
                        .queryString(PROMETHEUS_QUERY_STRING)
                        .startTime(queryStartDate)
                        .endTime(queryEndDate)
                        .step(PROMETHEUS_QUERY_STEP)
                        .build());
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isNotNull();
        assertThat(response.getData().getResult()).isNotNull();
        if (response.getStatus().toString().equalsIgnoreCase("error")) {
            assertThat(response.getErrorType()).isNotNull();
            assertThat(response.getError()).isNotNull();
        }
    }

    @Test
    public void runInstanceQuery() throws PrometheusClientException {
        PrometheusQueryResponse response = clientFactory.get(TEST_INTEGRATION_KEY)
                .runInstantQuery(PrometheusQueryRequest.builder()
                        .queryString(PROMETHEUS_QUERY_STRING)
                        .build());
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isNotNull();
        assertThat(response.getData().getResult()).isNotNull();
        if (response.getStatus().toString().equalsIgnoreCase("error")) {
            assertThat(response.getErrorType()).isNotNull();
            assertThat(response.getError()).isNotNull();
        }
    }
}
