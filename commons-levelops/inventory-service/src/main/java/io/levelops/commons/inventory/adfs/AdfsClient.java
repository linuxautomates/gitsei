package io.levelops.commons.inventory.adfs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.Builder;
import lombok.Value;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.Objects;

public class AdfsClient {

    private final ClientHelper<AdfsClientException> clientHelper;
    private final String adfsUrl;
    private final String adfsClientId;
    private final String adfsResource;
    private final String adfsUsername;
    private final String adfsPassword;

    public AdfsClient(OkHttpClient okHttpClient,
                      String adfsUrl,
                      String adfsClientId,
                      String adfsResource,
                      String adfsUsername,
                      String adfsPassword) {
        clientHelper = new ClientHelper<>(okHttpClient, DefaultObjectMapper.get(), AdfsClientException.class);
        this.adfsUrl = adfsUrl;
        this.adfsClientId = adfsClientId;
        this.adfsResource = adfsResource;
        this.adfsUsername = adfsUsername;
        this.adfsPassword = adfsPassword;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = AdfsTokenResponse.AdfsTokenResponseBuilder.class)
    public static class AdfsTokenResponse {
        @JsonProperty("access_token")
        String accessToken;
    }

    public String getAccessToken() throws AdfsClientException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(adfsUrl));
        Request request = new Request.Builder()
                .url(url)
                .post(new FormBody.Builder()
                        .add("client_id", adfsClientId)
                        .add("resource", adfsResource)
                        .add("username", adfsUsername)
                        .add("password", adfsPassword)
                        .add("grant_type", "password")
                        .build())
                .build();
        return clientHelper.executeAndParse(request, AdfsTokenResponse.class).getAccessToken();
    }


}
