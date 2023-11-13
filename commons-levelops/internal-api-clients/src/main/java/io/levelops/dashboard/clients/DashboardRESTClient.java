package io.levelops.dashboard.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.dashboard.services.DashboardService;
import io.levelops.exceptions.InternalApiClientException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.Map;

public class DashboardRESTClient implements DashboardService {

    private final String apiBaseURL;
    private final ClientHelper<InternalApiClientException> client;
    private final ObjectMapper objectMapper;

    public DashboardRESTClient(final OkHttpClient client, final String apiBaseURL, final ObjectMapper mapper) {
        this.objectMapper = mapper;
        this.client = ClientHelper.<InternalApiClientException>builder()
                .objectMapper(mapper)
                .client(client)
                .exception(InternalApiClientException.class)
                .build();
        this.apiBaseURL = apiBaseURL;
    }

    private HttpUrl.Builder getBaseUrlBuilder(final String company) {
        return HttpUrl.parse(apiBaseURL).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("dashboards");
    }

    @Override
    public Map<String, String> createDashboard(String company, Dashboard dashboard) throws InternalApiClientException {
        HttpUrl url = getBaseUrlBuilder(company)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(client.createJsonRequestBody(dashboard))
                .build();
        return client.executeAndParse(request, Map.class);
    }
}