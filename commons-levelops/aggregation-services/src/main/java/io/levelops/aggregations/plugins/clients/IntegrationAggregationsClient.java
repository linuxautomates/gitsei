package io.levelops.aggregations.plugins.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.IntegrationAgg;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IntegrationAggregationsClient {
    private final ClientHelper<IntegrationAggregationsClient.IntegrationAggregationsClientException> clientHelper;
    private final ObjectMapper mapper;
    private final String apiBaseUrl;

    @lombok.Builder
    public IntegrationAggregationsClient(final OkHttpClient client, final ObjectMapper mapper, final String apiBaseUrl) {
        this.mapper = mapper;
        this.apiBaseUrl = apiBaseUrl;
        this.clientHelper = new ClientHelper<>(client, mapper, IntegrationAggregationsClient.IntegrationAggregationsClientException.class);
    }

    private HttpUrl.Builder getBaseUrlBuilder(final String company) {
        return HttpUrl.parse(apiBaseUrl).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("product_aggs");
    }

    public String getById(String company, String aggregationResultId) throws IntegrationAggregationsClient.IntegrationAggregationsClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment(aggregationResultId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeRequest(request);
    }

    public PaginatedResponse<IntegrationAgg> getIntegrationAggregationResultsList(String company,
                                                                                  String productId,
                                                                                  List<String> integrationIds,
                                                                                  List<String> integrationTypes,
                                                                                  Integer page,
                                                                                  Integer pageSize)
            throws IntegrationAggregationsClient.IntegrationAggregationsClientException {
        Map<String, Object> filters = new HashMap<>();
        filters.put("product_id", productId);
        filters.put("integration_ids", integrationIds);
        filters.put("integration_types", integrationTypes);
        var url = getBaseUrlBuilder(company)
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(DefaultListRequest.builder()
                    .filter(filters)
                    .page(page)
                    .pageSize(pageSize)
                    .build()))
                .build();

        return clientHelper.executeAndParse(request,
                mapper.getTypeFactory().constructParametricType(PaginatedResponse.class, IntegrationAgg.class));
    }

    public Optional<String> getLatestIntegrationAggregationByToolType(String company,
                                                                      String productId,
                                                                      List<String> integrationIds,
                                                                      List<String> integrationTypes)
            throws IntegrationAggregationsClient.IntegrationAggregationsClientException {
        PaginatedResponse<IntegrationAgg> dbRecords = getIntegrationAggregationResultsList(company, productId, integrationIds, integrationTypes, 0, 1);
        if (dbRecords.getResponse() == null || dbRecords.getResponse().getRecords().size() == 0) {
            return Optional.empty();
        }
        
        String result = getById(company, dbRecords.getResponse().getRecords().get(0).getId());
        return Optional.ofNullable(result);
    }

    public static class IntegrationAggregationsClientException extends Exception {
        public IntegrationAggregationsClientException() {
        }

        public IntegrationAggregationsClientException(String message) {
            super(message);
        }

        public IntegrationAggregationsClientException(String message, Throwable cause) {
            super(message, cause);
        }

        public IntegrationAggregationsClientException(Throwable cause) {
            super(cause);
        }
    }
}
