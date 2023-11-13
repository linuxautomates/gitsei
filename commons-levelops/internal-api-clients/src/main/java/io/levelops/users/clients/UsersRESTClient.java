package io.levelops.users.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.users.requests.ModifyUserRequest;
import io.levelops.users.services.UserService;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;

public class UsersRESTClient implements UserService {

    private final String apiBaseUrl;
    private final ObjectMapper mapper;
    private final ClientHelper<InternalApiClientException> client;

    public UsersRESTClient(final OkHttpClient client,
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

    private HttpUrl.Builder getBaseUsersUrlBuilder(final String company) {
        return HttpUrl.parse(apiBaseUrl).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("users");
    }

    @Override
    public String createUser(String company, ModifyUserRequest modify) throws IOException {
        HttpUrl url = getBaseUsersUrlBuilder(company)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(client.createJsonRequestBody(modify))
                .build();

        return client.executeRequest(request);
    }

    @Override
    public String updateUser(String company, String userId, ModifyUserRequest modify) throws IOException {
        HttpUrl url = getBaseUsersUrlBuilder(company)
                .addPathSegment(userId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .put(client.createJsonRequestBody(modify))
                .build();

        return client.executeRequest(request);
    }

    @Override
    public User getUser(String company, String userId) throws IOException {
        HttpUrl url = getBaseUsersUrlBuilder(company)
                .addPathSegment(userId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return client.executeAndParse(request, User.class);
    }

    @Override
    public String multiUpdateUsers(String company, ModifyUserRequest userRequest) throws IOException {
        HttpUrl url = getBaseUsersUrlBuilder(company)
                .addPathSegment("multi_update")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(client.createJsonRequestBody(userRequest))
                .build();

        return client.executeRequest(request);
    }

    @Override
    public String deleteUser(String company, String userId) throws IOException {
        HttpUrl url = getBaseUsersUrlBuilder(company)
                .addPathSegment(userId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        return client.executeRequest(request);
    }

    @Override
    public DbListResponse<User> listUsers(String company, DefaultListRequest listRequest)
            throws IOException {
        HttpUrl url = getBaseUsersUrlBuilder(company)
                .addPathSegment("list")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(client.createJsonRequestBody(listRequest))
                .build();

        return client.executeAndParse(request,
                mapper.getTypeFactory()
                        .constructParametricType(DbListResponse.class, User.class));
    }

    @Override
    public DbListResponse<User> listUsers(String company)
            throws IOException {
        HttpUrl url = getBaseUsersUrlBuilder(company)
                .addPathSegment("list")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return client.executeAndParse(request,
                mapper.getTypeFactory()
                        .constructParametricType(DbListResponse.class, User.class));
    }
}
