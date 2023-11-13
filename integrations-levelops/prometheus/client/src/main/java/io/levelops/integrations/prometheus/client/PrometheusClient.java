package io.levelops.integrations.prometheus.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.integrations.prometheus.models.PrometheusQueryRequest;
import io.levelops.integrations.prometheus.models.PrometheusQueryResponse;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Log4j2
public class PrometheusClient {

    private static final String QUERY_RANGE_API_PATH = "api/v1/query_range";
    private static final String INSTANT_QUERY_API_PATH = "api/v1/query";

    private static final String QUERY_PARAM = "query";
    private static final String START_PARAM = "start";
    private static final String END_PARAM = "end";
    private static final String STEP_PARAM = "step";

    private final ClientHelper<PrometheusClientException> clientHelper;
    private final String resourceUrl;

    @Builder
    public PrometheusClient(final OkHttpClient okHttpClient, String resourceUrl, ObjectMapper objectMapper) {
        this.resourceUrl = resourceUrl;
        this.clientHelper = ClientHelper.<PrometheusClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(PrometheusClientException.class)
                .build();
    }

    public PrometheusQueryResponse runRangeQuery(PrometheusQueryRequest prometheusQueryRequest) throws PrometheusClientException {
        log.info("runQueryRange: Querying prometheus for range query. API - {}", QUERY_RANGE_API_PATH);
        var url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(QUERY_RANGE_API_PATH)
                .addQueryParameter(QUERY_PARAM, prometheusQueryRequest.getQueryString())
                .addQueryParameter(START_PARAM, DateTimeFormatter.ISO_INSTANT.format(prometheusQueryRequest.getStartTime().toInstant()))
                .addQueryParameter(END_PARAM, DateTimeFormatter.ISO_INSTANT.format(prometheusQueryRequest.getEndTime().toInstant()))
                .addQueryParameter(STEP_PARAM, prometheusQueryRequest.getStep())
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<PrometheusQueryResponse> queryResponse = clientHelper
                .executeAndParseWithHeaders(request, PrometheusQueryResponse.class);
        log.debug("runQueryRange: Output for api {} - {}", QUERY_RANGE_API_PATH, queryResponse);
        return queryResponse.getBody();
    }

    public PrometheusQueryResponse runInstantQuery(PrometheusQueryRequest prometheusQueryRequest) throws PrometheusClientException {
        log.info("runInstanceQuery: Querying prometheus for instant query. API - {}", INSTANT_QUERY_API_PATH);
        var url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(INSTANT_QUERY_API_PATH)
                .addQueryParameter(QUERY_PARAM, prometheusQueryRequest.getQueryString())
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<PrometheusQueryResponse> queryResponse = clientHelper
                .executeAndParseWithHeaders(request, PrometheusQueryResponse.class);
        log.debug("runInstanceQuery: Output for api {} - {}", INSTANT_QUERY_API_PATH, queryResponse);
        return queryResponse.getBody();
    }

    private Request buildRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
    }
}
