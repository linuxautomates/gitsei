package io.levelops.commons.token_services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.token_exceptions.InvalidTokenDataException;
import io.levelops.commons.token_exceptions.TokenException;
import lombok.Builder;
import lombok.Getter;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static io.levelops.commons.client.ClientConstants.ACCEPT;
import static io.levelops.commons.client.ClientConstants.APPLICATION_JSON;
import static io.levelops.commons.client.ClientConstants.AUTHORIZATION;

public class BitbucketTokenService extends TokenService {
    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static final String BITBUCKET_CLOUD_OAUTH_URL = "https://bitbucket.org/site/oauth2/access_token";
    private static final String GRANT_TYPE = "grant_type";
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String CODE = "code";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String REFRESH_TOKEN = "refresh_token";

    private final String redirectHost;
    private final BitbucketTokenService.Secrets secrets;

    @Builder
    public BitbucketTokenService(OkHttpClient httpClient, ObjectMapper mapper,
                                 BitbucketTokenService.Secrets secrets, String redirectHost) {
        super(httpClient, mapper);
        this.secrets = secrets;
        this.redirectHost = redirectHost;
    }

    private Request.Builder addBasicAuthHeader(Request.Builder requestBuilder, final String clientId, final String clientSecret) {
        String basicAuthDecoded = clientId + ":" + clientSecret;
        String basicAuthEncoded = Base64.getEncoder().encodeToString(basicAuthDecoded.getBytes());
        String basicAuthFinalValue = "Basic " + basicAuthEncoded;
        requestBuilder.header(AUTHORIZATION, basicAuthFinalValue);
        return requestBuilder;
    }

    @Override
    public Tokens getTokensFromCode(String code, String state) throws TokenException {
        LOGGER.debug("Get Token From Code... code={}, state={}", code, state);
        if (StringUtils.isEmpty(code)) {
            throw new InvalidTokenDataException("Bad request. Missing code in request.");
        }
        if (StringUtils.isEmpty(state)) {
            throw new InvalidTokenDataException("Bad request. Missing state in request.");
        }
        HttpUrl url = HttpUrl.parse(BITBUCKET_CLOUD_OAUTH_URL);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(GRANT_TYPE, AUTHORIZATION_CODE)
                .addFormDataPart(CODE, code)
                .addFormDataPart("redirect_uri", redirectHost + "/integration-callback")
                .build();
        return getTokens(url, requestBody, null);
    }

    @Override
    public Tokens getTokensFromRefreshToken(String refreshToken) throws TokenException {
        if (StringUtils.isEmpty(refreshToken)) {
            throw new InvalidTokenDataException("Bad request. Missing refreshToken in request.");
        }
        HttpUrl url = HttpUrl.parse(BITBUCKET_CLOUD_OAUTH_URL);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(GRANT_TYPE, REFRESH_TOKEN)
                .addFormDataPart(REFRESH_TOKEN, refreshToken)
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
        Map<String, String> result = getClientHelper().parseResponse(rawResponse,
                DefaultObjectMapper.get().getTypeFactory().constructParametricType(HashMap.class, String.class, String.class));
        String newRefreshToken = result.get(REFRESH_TOKEN);
        if(StringUtils.isEmpty(newRefreshToken) && StringUtils.isEmpty(existingRefreshToken)) {
            throw new InvalidTokenDataException("Bad request. Missing refresh token.");
        }
        return Tokens.builder()
                .accessToken(result.get(ACCESS_TOKEN))
                .refreshToken((StringUtils.isEmpty(newRefreshToken) ? existingRefreshToken : newRefreshToken))
                .rawTokenResponse(rawResponse)
                .build();
    }

    @Getter
    @Builder
    public static class Secrets {
        private final String clientId;
        private final String clientSecret;

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("clientId", clientId)
                    .append("clientSecret", StringUtils.repeat("*", clientSecret.length()))
                    .toString();
        }
    }
}
