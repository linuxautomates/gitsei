package io.levelops.user_identity.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.user_identity.services.UserIdentityService;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class UserIdentityRESTClient implements UserIdentityService {

    private final String apiBaseURL;
    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper objectMapper;

    public UserIdentityRESTClient(final OkHttpClient client, final String apiBaseURL, ObjectMapper objectMapper) {
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
                .addPathSegment("user_identities");
    }

    @Override
    public PaginatedResponse<DbScmUser> listUsers(String company, DefaultListRequest filter) throws InternalApiClientException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(filter))
                .build();
        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, DbScmUser.class));
    }
}
