package io.levelops.org.units;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.organization.OrgUnitDTO;
import io.levelops.exceptions.InternalApiClientException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.Map;
import java.util.Set;

public class OrgUnitsRESTClient {

    private final String apiBaseURL;
    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper objectMapper;

    public OrgUnitsRESTClient(final OkHttpClient client, final String apiBaseURL, ObjectMapper objectMapper) {
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
                .addPathSegment(company)
                .addPathSegment("org")
                .addPathSegment("units");
    }

    public Map<String,Object> createOrgUnits(String company, Set<OrgUnitDTO> orgUnits) throws InternalApiClientException {
        HttpUrl url = getBaseUrlBuilder(company)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(orgUnits))
                .build();
        return clientHelper.executeAndParse(request,Map.class);
    }
}
