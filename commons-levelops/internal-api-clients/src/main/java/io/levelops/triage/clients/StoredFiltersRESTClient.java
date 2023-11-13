package io.levelops.triage.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.StoredFilter;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.triage.services.StoredService;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.util.List;

public class StoredFiltersRESTClient implements StoredService {

    private final String apiBaseUrl;
    private final ObjectMapper mapper;
    private final ClientHelper<InternalApiClientException> client;

    public StoredFiltersRESTClient(final OkHttpClient client,
                                   final ObjectMapper mapper,
                                   final String apiBaseURL) {
        this.client = ClientHelper.<InternalApiClientException>builder()
                .client(client)
                .objectMapper(mapper)
                .exception(InternalApiClientException.class)
                .build();
        this.apiBaseUrl = apiBaseURL;
        this.mapper = mapper;
    }

    private HttpUrl.Builder getBaseStoredFilterUrlBuilder(final String company, final String type) {
        return HttpUrl.parse(apiBaseUrl).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("filters")
                .addPathSegment(type);
    }

    @Override
    public String upsertStoredFilter(String company, String type, StoredFilter filter, String name) throws IOException {
        HttpUrl url = getBaseStoredFilterUrlBuilder(company, type)
                .addPathSegment(name)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .put(client.createJsonRequestBody(filter))
                .build();
        return client.executeRequest(request);
    }

    @Override
    public StoredFilter getStoredFilter(String company, String type, String name) throws IOException {
        HttpUrl url = getBaseStoredFilterUrlBuilder(company, type)
                .addPathSegment(name)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return client.executeAndParse(request, StoredFilter.class);
    }

    @Override
    public DeleteResponse deleteStoredFilter(String company, String type, String name) throws IOException {
        HttpUrl url = getBaseStoredFilterUrlBuilder(company, type)
                .addPathSegment(name)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        return client.executeAndParse(request, DeleteResponse.class);
    }

    @Override
    public BulkDeleteResponse bulkDeleteStoredFilters(String company, String type, List<String> filterNames) throws IOException {
        HttpUrl url = getBaseStoredFilterUrlBuilder(company, type)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .delete(client.createJsonRequestBody(filterNames))
                .build();
        return client.executeAndParse(request, BulkDeleteResponse.class);
    }

    @Override
    public DbListResponse<StoredFilter> listStoredFilters(String company, String type, DefaultListRequest listRequest) throws IOException {
        HttpUrl url = getBaseStoredFilterUrlBuilder(company, type)
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(client.createJsonRequestBody(listRequest))
                .build();
        return client.executeAndParse(request,
                mapper.getTypeFactory().constructParametricType(DbListResponse.class, StoredFilter.class));
    }
}