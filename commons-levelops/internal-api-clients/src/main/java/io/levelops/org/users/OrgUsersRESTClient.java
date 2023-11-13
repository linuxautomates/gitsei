package io.levelops.org.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.organization.OrgUserDTO;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import java.util.Map;
import java.util.Set;

public class OrgUsersRESTClient  {

    private final String apiBaseURL;
    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper objectMapper;

    public OrgUsersRESTClient(final OkHttpClient client, final String apiBaseURL, ObjectMapper objectMapper) {
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
                .addPathSegment("users");
    }

    public Map<String,Object> createOrgUsers(String company, Set<OrgUserDTO> orgUsers) throws InternalApiClientException {
        HttpUrl url = getBaseUrlBuilder(company)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(orgUsers))
                .build();
        return clientHelper.executeAndParse(request,Map.class);
    }

    public PaginatedResponse<OrgUserDTO> listOrgUsers(String company, DefaultListRequest filter) throws InternalApiClientException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(filter))
                .build();
        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, OrgUserDTO.class));
    }
}
