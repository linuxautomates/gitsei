package io.levelops.aggregations.plugins.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.models.JenkinsMonitoringAggDataDTO;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.AggregationRecord;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PluginResultAggregationsClient {
    private final static AggregationRecord.Type TYPE = AggregationRecord.Type.PLUGIN_AGGREGATION;
    private final ClientHelper<PluginResultAggregationsClientException> clientHelper;
    private final ObjectMapper mapper;
    private final String apiBaseUrl;

    @lombok.Builder
    public PluginResultAggregationsClient(final OkHttpClient client, final ObjectMapper mapper, final String apiBaseUrl) {
        this.mapper = mapper;
        this.apiBaseUrl = apiBaseUrl;
        this.clientHelper = new ClientHelper<>(client, mapper, PluginResultAggregationsClientException.class);
    }

    private HttpUrl.Builder getBaseUrlBuilder(final String company){
        return HttpUrl.parse(apiBaseUrl).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugin_aggs");
    }

    public JenkinsMonitoringAggDataDTO getById(String company, String aggregationResultId) throws PluginResultAggregationsClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment(aggregationResultId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, JenkinsMonitoringAggDataDTO.class);
    }

    public PaginatedResponse<AggregationRecord> getPluginAggregationResultsList(String company, String productId, String toolType) throws PluginResultAggregationsClientException {
        Map<String,Object> filters = new HashMap<>();
        filters.put("type", TYPE.toString());
        filters.put("product_id", productId);
        filters.put("tool_type", toolType);
        DefaultListRequest requestBody = DefaultListRequest.builder()
                .filter(filters)
                .build();
        var url = getBaseUrlBuilder(company)
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(requestBody))
                .build();

        return clientHelper.executeAndParse(request,
                mapper.getTypeFactory().constructParametricType(PaginatedResponse.class, AggregationRecord.class));
    }

    public Optional<JenkinsMonitoringAggDataDTO> getLatestPluginAggregationByToolType(String company, String productId, String toolType) throws PluginResultAggregationsClientException {
        PaginatedResponse<AggregationRecord> dbRecords = getPluginAggregationResultsList(company, productId, toolType);
        if((dbRecords.getResponse() == null) || (dbRecords.getResponse().getRecords().size() ==0)){
            return Optional.empty();
        }
        JenkinsMonitoringAggDataDTO result = getById(company, dbRecords.getResponse().getRecords().get(0).getId());
        return Optional.ofNullable(result);
    }

    public static class PluginResultAggregationsClientException extends Exception {
        public PluginResultAggregationsClientException() {
        }

        public PluginResultAggregationsClientException(String message) {
            super(message);
        }

        public PluginResultAggregationsClientException(String message, Throwable cause) {
            super(message, cause);
        }

        public PluginResultAggregationsClientException(Throwable cause) {
            super(cause);
        }
    }
}
