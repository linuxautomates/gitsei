package io.levelops.commons.token_services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.token_exceptions.InvalidTokenDataException;
import io.levelops.commons.token_exceptions.TokenException;
import lombok.Builder;
import lombok.Value;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;

import static io.levelops.commons.client.ClientConstants.*;

public class ServicenowTokenService extends TokenService{

    private static final String SERVICENOW_OAUTH_URL = "/oauth_token.do";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String GRANT_TYPE = "grant_type";

    private final ServicenowSecrets secrets;

    @Builder
    ServicenowTokenService(OkHttpClient httpClient, ObjectMapper mapper, ServicenowSecrets secrets) {
        super(httpClient, mapper);
        this.secrets = secrets;
    }

    @Override
    public Tokens getTokensFromCode(String code, String state) throws TokenException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Tokens getTokensFromRefreshToken(String refreshToken) throws TokenException {
        throw new UnsupportedOperationException();
    }

    public Tokens getTokens(String username, String password, String servicenowOAuthURL) throws TokenException {

        if (StringUtils.isEmpty(servicenowOAuthURL)) {
            throw new InvalidTokenDataException("Bad request. Missing servicenow instace url in request.");
        }
        if (StringUtils.isEmpty(username)) {
            throw new InvalidTokenDataException("Bad request. Missing username in request.");
        }
        if (StringUtils.isEmpty(password)) {
            throw new InvalidTokenDataException("Bad request. Missing password in request.");
        }

        servicenowOAuthURL += SERVICENOW_OAUTH_URL;
        var url = HttpUrl.parse(servicenowOAuthURL);
        RequestBody requestBody = new FormBody.Builder()
                .addEncoded(GRANT_TYPE, PASSWORD)
                .addEncoded(USERNAME, username)
                .addEncoded(PASSWORD, password)
                .build();
        return getTokens(url, requestBody,  null);
    }

    public Tokens getRefreshToken(String refreshToken, String servicenowOAuthURL) throws TokenException {
        if (StringUtils.isEmpty(refreshToken)) {
            throw new InvalidTokenDataException("Bad request. Missing refresh token in request.");
        }
        if (StringUtils.isEmpty(servicenowOAuthURL)) {
            throw new InvalidTokenDataException("Bad request. Missing servicenow instace url in request.");
        }

        servicenowOAuthURL += SERVICENOW_OAUTH_URL;
        var url = HttpUrl.parse(servicenowOAuthURL);
        RequestBody requestBody = new FormBody.Builder()
                .addEncoded(GRANT_TYPE, REFRESH_TOKEN)
                .addEncoded(REFRESH_TOKEN, refreshToken)
                .build();

        return getTokens(url, requestBody, refreshToken);
    }

    private Tokens getTokens(HttpUrl url, RequestBody requestBody, String existingRefreshToken) throws TokenException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header(ACCEPT, APPLICATION_JSON.toString())
                .post(requestBody);

        addBasicAuthHeader(requestBuilder, secrets.getClientId(), secrets.getClientSecret());

        String rawResponse = getClientHelper().executeRequest(requestBuilder.build());
        ServicenowTokenService.ServicenowOauthResponse response = getClientHelper().parseResponse(rawResponse, ServicenowTokenService.ServicenowOauthResponse.class);
        String newRefreshToken = response.getRefreshToken();
        if(StringUtils.isEmpty(newRefreshToken) && StringUtils.isEmpty(existingRefreshToken)) {
            throw new InvalidTokenDataException("Bad request. Missing refresh token.");
        }
        return Tokens.builder()
                .accessToken((response.getAccessToken()))
                .refreshToken((StringUtils.isEmpty(newRefreshToken) ? existingRefreshToken : newRefreshToken))
                .rawTokenResponse(rawResponse)
                .build();
    }

    private Request.Builder addBasicAuthHeader(Request.Builder requestBuilder, final String clientId, final String clientSecret) {
        String basicAuthDecoded = clientId + ":" + clientSecret;
        String basicAuthEncoded = Base64.getEncoder().encodeToString(basicAuthDecoded.getBytes());
        String basicAuthFinalValue = "Basic " + basicAuthEncoded;
        requestBuilder.header(AUTHORIZATION, basicAuthFinalValue);
        return requestBuilder;
    }


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ServicenowTokenService.ServicenowOauthResponse.ServicenowOauthResponseBuilder.class)
    public static class ServicenowOauthResponse {
        @JsonProperty("access_token")
        String accessToken;
        @JsonProperty("refresh_token")
        String refreshToken;
        @JsonProperty("scope")
        String scope;
        @JsonProperty("token_type")
        String tokenType;
        @JsonProperty("expires_in")
        Long expiresIn;
    }

    @Builder
    @Value
    public static class ServicenowSecrets{

        private String clientId;
        private String clientSecret;

    }
}
