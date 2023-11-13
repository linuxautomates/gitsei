package io.harness.atlassian_connect;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.atlassian_connect.exceptions.AtlassianConnectServiceClientException;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.client.retrying.RetryingInterceptor;
import io.levelops.commons.databases.models.database.atlassian_connect.AtlassianConnectAppMetadata;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AtlassianConnectServiceClient {
    private final String atlassianConnectServiceUrl;
    private final ClientHelper<AtlassianConnectServiceClientException> clientHelper;


    public AtlassianConnectServiceClient(String atlassianConnectServiceUrl, OkHttpClient client, ObjectMapper objectMapper) {
        this.atlassianConnectServiceUrl = atlassianConnectServiceUrl;
        this.clientHelper = ClientHelper.<AtlassianConnectServiceClientException>builder()
                .client(client.newBuilder()
                        .addInterceptor(RetryingInterceptor.buildHttpCodeRetryer(3, 20, 100, TimeUnit.MILLISECONDS, 500))
                        .build())
                .objectMapper(objectMapper)
                .exception(AtlassianConnectServiceClientException.class)
                .build();
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return HttpUrl.parse(atlassianConnectServiceUrl).newBuilder()
                .addPathSegment("v1")
                .addPathSegment("atlassian-connect");
    }

    public String getSecret(String clientKey) throws AtlassianConnectServiceClientException {
        // GET /v1/atlassian-connect/secrets?client_key={clientKey}
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("secrets")
                .addQueryParameter("client_key", clientKey)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeRequest(request);
    }

    public AtlassianConnectAppMetadata getMetadata(String clientKey) throws AtlassianConnectServiceClientException {
        // GET /v1/atlassian-connect/metadata?client_key={clientKey}
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("metadata")
                .addQueryParameter("client_key", clientKey)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, AtlassianConnectAppMetadata.class);
    }

    public String generateOtp(String tenant) throws AtlassianConnectServiceClientException {
        // POST /v1/atlassian-connect/otp/generate
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("otp")
                .addPathSegment("generate")
                .addQueryParameter("tenant", tenant)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(StringUtils.EMPTY, null))
                .build();
        return clientHelper.executeRequest(request);
    }

    public Boolean submitOtp(String atlassianClientKey, String otp) throws AtlassianConnectServiceClientException {
        // POST /v1/atlassian-connect/otp/submit
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("otp")
                .addPathSegment("submit")
                .addQueryParameter("atlassian_client_key", atlassianClientKey)
                .addQueryParameter("otp", otp)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(StringUtils.EMPTY, null))
                .build();
        return clientHelper.executeAndParse(request, Boolean.class);
    }

    public Optional<String> claimOtp(String tenant, String otp) throws AtlassianConnectServiceClientException {
        // GET /v1/atlassian-connect/otp/claim
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("otp")
                .addPathSegment("claim")
                .addQueryParameter("tenant", tenant)
                .addQueryParameter("otp", otp)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return Optional.ofNullable(clientHelper.executeRequest(request)).filter(s -> !s.isEmpty());
    }
}
