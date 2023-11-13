package io.levelops.config_tables.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.collections4.ListUtils;

import java.util.List;
import java.util.Map;

public class ConfigTableClient {

    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;

    public ConfigTableClient(final OkHttpClient client, final ObjectMapper mapper, final String apiBaseUrl) {
        this.objectMapper = mapper;
        this.apiBaseUrl = apiBaseUrl;
        this.clientHelper = new ClientHelper<>(client, mapper, InternalApiClientException.class);
    }

    private HttpUrl.Builder getBaseUrlBuilder(final String company) {
        return HttpUrl.parse(apiBaseUrl).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("config-tables");
    }

    public ConfigTable get(String company, String id, List<String> expand) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment(id)
                .addQueryParameter("expand", String.join(",", ListUtils.emptyIfNull(expand)))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, ConfigTable.class);
    }

    public PaginatedResponse<ConfigTable> list(String company, DefaultListRequest defaultListRequest) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment("list")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(defaultListRequest))
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, ConfigTable.class));
    }

    public Map<String, String> create(String company, ConfigTable configTable) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(configTable))
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
    }

    public Map<String, Object> update(String company, String id, ConfigTable configTable) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment(id)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .put(clientHelper.createJsonRequestBody(configTable))
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    }

    /**
     * THIS DOESN'T BUMP THE TABLE'S VERSION
     */
    public Map<String, Object> updateRow(String company, String tableId, String rowId, ConfigTable.Row row) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment(tableId)
                .addPathSegment("rows")
                .addPathSegment(rowId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .put(clientHelper.createJsonRequestBody(row))
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    }

    public Map<String, Object> createRow(String company, String tableId, ConfigTable.Row row) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment(tableId)
                .addPathSegment("rows")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(row))
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    }

    /**
     * THIS DOESN'T BUMP THE TABLE'S VERSION
     */
    public Map<String, Object> updateColumn(String company, String tableId, String rowId, String columnId, String value) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment(tableId)
                .addPathSegment("rows")
                .addPathSegment(rowId)
                .addPathSegment("columns")
                .addPathSegment(columnId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .put(clientHelper.createJsonRequestBody(value))
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    }


    public DeleteResponse delete(String company, String id) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment(id)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        return clientHelper.executeAndParse(request, DeleteResponse.class);
    }

    public BulkDeleteResponse bulkDelete(String company, List<String> ids) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .delete(clientHelper.createJsonRequestBody(ids))
                .build();
        return clientHelper.executeAndParse(request, BulkDeleteResponse.class);
    }

    public ConfigTable getRevision(String company, String id, String version) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment(id)
                .addPathSegment("revisions")
                .addPathSegment(version)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, ConfigTable.class);
    }

}
