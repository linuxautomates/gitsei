package io.levelops.commons.token_services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.token_exceptions.InvalidTokenDataException;
import io.levelops.commons.token_exceptions.TokenException;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import static io.levelops.commons.client.ClientConstants.*;

public class AzureDevopsTokenService extends TokenService {

    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static final String AZURE_DEVOPS_OAUTH_URL = "https://app.vssps.visualstudio.com/oauth2/token";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String REFRESH_TOKEN = "refresh_token";

    private final AzureDevopsSecrets secrets;
    private final String redirectHost;

    @Builder
    public AzureDevopsTokenService(OkHttpClient httpClient, ObjectMapper mapper,
                                   AzureDevopsSecrets secrets, String redirectHost) {
        super(httpClient, mapper);
        this.secrets = secrets;
        this.redirectHost = redirectHost;
    }

    @Override
    public TokenService.Tokens getTokensFromCode(String code, String state) throws TokenException {
        LOGGER.debug("Get Token From Code... code={}, state={}", code, state);
        if (StringUtils.isEmpty(code)) {
            throw new InvalidTokenDataException("Bad request. Missing code in request.");
        }
        if (StringUtils.isEmpty(state)) {
            throw new InvalidTokenDataException("Bad request. Missing state in request.");
        }
        HttpUrl url = HttpUrl.parse(AZURE_DEVOPS_OAUTH_URL);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                .addFormDataPart("client_assertion", secrets.getClientSecret())
                .addFormDataPart("assertion", code)
                .addFormDataPart("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                .addFormDataPart("redirect_uri", redirectHost + "/integration-callback")
                .build();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header(CONTENT_TYPE, APPLICATION_FORM_URL_ENCODED.toString())
                .post(requestBody);
        String rawResponse = getClientHelper().executeRequest(requestBuilder.build());
        Map<String, String> result = getClientHelper().parseResponse(rawResponse,
                DefaultObjectMapper.get().getTypeFactory().constructParametricType(HashMap.class, String.class, String.class));
        return TokenService.Tokens.builder()
                .accessToken(result.get(ACCESS_TOKEN))
                .refreshToken(result.get(REFRESH_TOKEN))
                .rawTokenResponse(rawResponse)
                .build();
    }


    @Override
    public TokenService.Tokens getTokensFromRefreshToken(String refreshToken) throws TokenException {
        if (StringUtils.isEmpty(refreshToken)) {
            throw new InvalidTokenDataException("Bad request. Missing refreshToken in request.");
        }

        HttpUrl url = HttpUrl.parse(AZURE_DEVOPS_OAUTH_URL);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                .addFormDataPart("client_assertion", secrets.getClientSecret())
                .addFormDataPart("assertion", refreshToken)
                .addFormDataPart("grant_type", "refresh_token")
                .addFormDataPart("redirect_uri", redirectHost + "/integration-callback")
                .build();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header(CONTENT_TYPE, APPLICATION_FORM_URL_ENCODED.toString())
                .post(requestBody);
        String rawResponse = getClientHelper().executeRequest(requestBuilder.build());
        Map<String, String> result = getClientHelper().parseResponse(rawResponse,
                DefaultObjectMapper.get().getTypeFactory().constructParametricType(HashMap.class,
                        String.class, String.class));
        return TokenService.Tokens.builder()
                .accessToken(result.get(ACCESS_TOKEN))
                .refreshToken(result.get(REFRESH_TOKEN))
                .rawTokenResponse(rawResponse)
                .build();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = AzureDevopsOauthResponse.AzureDevopsOauthResponseBuilder.class)
    public static class AzureDevopsOauthResponse {
        @JsonProperty("access_token")
        String accessToken;
        @JsonProperty("token_type")
        String tokenType;
        @JsonProperty("expires_in")
        String expires_in;
        @JsonProperty("refresh_token")
        String refreshToken;

    }

    @Getter
    @Builder
    public static class AzureDevopsSecrets {
        private final String clientId;
        private final String clientSecret;
        private final String code;

        @Override
        public String toString() {
            return "AzureDevopsSecrets{" +
                    "clientId='" + clientId + '\'' +
                    ", clientSecret='" + clientSecret + '\'' +
                    ", assertion='" + code + '\'' +
                    '}';
        }
    }
}
