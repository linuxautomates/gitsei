package io.levelops.tenants.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.TenantState;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.models.DbListResponse;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.tenants.services.TenantManagementService;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.Map;

public class TenantManagementRESTClient implements TenantManagementService {

    private final String apiBaseURL;
    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper objectMapper;

    public TenantManagementRESTClient(final OkHttpClient client, final String apiBaseURL, ObjectMapper objectMapper) {
        this.apiBaseURL = apiBaseURL;
        this.objectMapper = objectMapper;
        this.clientHelper = ClientHelper.<InternalApiClientException>builder()
                .objectMapper(objectMapper)
                .client(client)
                .exception(InternalApiClientException.class).build();
    }

    private HttpUrl.Builder getBaseUrlBuilder(final String company) {
        return HttpUrl.parse(apiBaseURL).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company);
    }

    @Override
    public Map<String, String> createTenantState(String company, TenantState state) throws InternalApiClientException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment("state")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(state))
                .build();
        return clientHelper.executeAndParse(request, Map.class);
    }

    @Override
    public DbListResponse<TenantState> getTenantStates(String company) throws InternalApiClientException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment("state")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, TenantState.class));
    }
}
