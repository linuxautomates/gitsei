package io.levelops.tenant_config.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.models.DbListResponse;
import io.levelops.exceptions.InternalApiClientException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;


public class TenantConfigClient {
    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;

    public TenantConfigClient(final OkHttpClient client, final ObjectMapper mapper, final String apiBaseUrl) {
        this.objectMapper = mapper;
        this.apiBaseUrl = apiBaseUrl;
        this.clientHelper = new ClientHelper<>(client, mapper, InternalApiClientException.class);
    }

    private HttpUrl.Builder getBaseUrlBuilder(final String company) {
        return HttpUrl.parse(apiBaseUrl).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenant_config")
                .addPathSegment(company);
    }

    public DbListResponse<TenantConfig> get(String company, String configKey) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .addQueryParameter("config_key", String.join(",", configKey))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, TenantConfig.class));
    }
}
