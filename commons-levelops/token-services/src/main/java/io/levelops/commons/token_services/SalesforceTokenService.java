package io.levelops.commons.token_services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.token_exceptions.InvalidTokenDataException;
import io.levelops.commons.token_exceptions.TokenException;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;

public class SalesforceTokenService extends TokenService {
    private static final String SALESFORCE_OAUTH_URL = "https://login.salesforce.com/services/oauth2/token";

    private final SalesforceSecrets secrets;
    private final String redirectHost;

    @Builder
    public SalesforceTokenService(OkHttpClient httpClient, ObjectMapper mapper,
                                  SalesforceSecrets secrets, String redirectHost) {
        super(httpClient, mapper);
        this.secrets = secrets;
        this.redirectHost = redirectHost;
    }

    @Override
    public Tokens getTokensFromCode(String code, String state) throws TokenException {
        if (StringUtils.isEmpty(code)) {
            throw new InvalidTokenDataException("Bad request. Missing code in request.");
        }

        var url = HttpUrl.parse(SALESFORCE_OAUTH_URL).newBuilder()
                .addQueryParameter("client_id", secrets.getClientId())
                .addQueryParameter("client_secret", secrets.getClientSecret())
                .addQueryParameter("code", code)
                .addQueryParameter("state", state)
                .addQueryParameter("grant_type", "authorization_code")
                .addQueryParameter("redirect_uri", redirectHost + "/integration-callback")
                .build();
        return getTokens(url, null);
    }

    @Override
    public Tokens getTokensFromRefreshToken(String refreshToken) throws TokenException {
        if (StringUtils.isEmpty(refreshToken)) {
            throw new InvalidTokenDataException("Bad request. Missing refresh token in request.");
        }
        var url = HttpUrl.parse(SALESFORCE_OAUTH_URL).newBuilder()
                .addQueryParameter("client_id", secrets.getClientId())
                .addQueryParameter("client_secret", secrets.getClientSecret())
                .addQueryParameter("refresh_token", refreshToken)
                .addQueryParameter("grant_type", "refresh_token")
                .build();
        return getTokens(url, refreshToken);
    }

    private Tokens getTokens(HttpUrl url, String existingRefreshToken) throws TokenException {
        Request request = new Request.Builder()
                .url(url)
                .post(new FormBody.Builder().build())
                .build();
        String rawResponse = getClientHelper().executeRequest(request);
        SalesforceOauthResponse response = getClientHelper().parseResponse(rawResponse, SalesforceOauthResponse.class);
        String newRefreshToken = response.getRefreshToken();
        if(StringUtils.isEmpty(newRefreshToken) && StringUtils.isEmpty(existingRefreshToken)) {
            throw new InvalidTokenDataException("Bad request. Missing refresh token.");
        }
        return Tokens.builder()
                .accessToken((response.getAccessToken()))
                .refreshToken((StringUtils.isEmpty(newRefreshToken) ? existingRefreshToken : newRefreshToken))
                .instanceUrl(response.getInstanceUrl())
                .rawTokenResponse(rawResponse)
                .build();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SalesforceOauthResponse.SalesforceOauthResponseBuilder.class)
    public static class SalesforceOauthResponse {
        @JsonProperty("access_token")
        String accessToken;
        @JsonProperty("refresh_token")
        String refreshToken;
        @JsonProperty("signature")
        String signature;
        @JsonProperty("scope")
        String scope;
        @JsonProperty("id_token")
        String idToken;
        @JsonProperty("instance_url")
        String instanceUrl;
        @JsonProperty("id")
        String id;
        @JsonProperty("token_type")
        String tokenType;
        @JsonProperty("issued_at")
        Long issuedAt;
    }

    @Getter
    @Builder
    public static class SalesforceSecrets {
        private final String clientId;
        private final String clientSecret;
        private final String code;

        @Override
        public String toString() {
            return "SalesforceSecrets{" +
                    "clientId='" + clientId + '\'' +
                    ", clientSecret='" + clientSecret + '\'' +
                    ", code='" + code + '\'' +
                    '}';
        }
    }
}
