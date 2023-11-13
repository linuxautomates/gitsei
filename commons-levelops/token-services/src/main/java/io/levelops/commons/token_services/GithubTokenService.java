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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

import static io.levelops.commons.client.ClientConstants.ACCEPT;
import static io.levelops.commons.client.ClientConstants.APPLICATION_JSON;

public class GithubTokenService extends TokenService {
    private static final Log LOGGER = LogFactory.getLog(GithubTokenService.class);

    private static final String GITHUB_CLOUD_OAUTH_URL = "https://github.com/login/oauth/access_token";
    private final GithubSecrets secrets;

    @Builder
    public GithubTokenService(OkHttpClient httpClient, ObjectMapper mapper, GithubSecrets secrets) {
        super(httpClient, mapper);
        this.secrets = secrets;
    }

    @Override
    public Tokens getTokensFromCode(String code, String state) throws TokenException {
        if (StringUtils.isEmpty(code)) {
            throw new InvalidTokenDataException("Bad request. Missing code in request.");
        }
        if (StringUtils.isEmpty(state)) {
            throw new InvalidTokenDataException("Bad request. Missing state in request.");
        }
        HttpUrl url = HttpUrl.parse(GITHUB_CLOUD_OAUTH_URL);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("client_id", secrets.getClientId())
                .addFormDataPart("client_secret", secrets.getClientSecret())
                .addFormDataPart("code", code)
                .addFormDataPart("state", state)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ACCEPT, APPLICATION_JSON.toString())
                .post(requestBody)
                .build();
        String rawResponse = getClientHelper().executeRequest(request);
        Map result = getClientHelper().parseResponse(rawResponse,
                DefaultObjectMapper.get().getTypeFactory().constructParametricType(HashMap.class, String.class, String.class));
        return Tokens.builder()
                .accessToken((String) result.get("access_token"))
                .rawTokenResponse(rawResponse)
                .build();
    }

    @Override
    public Tokens getTokensFromRefreshToken(String refreshToken) {
        throw new UnsupportedOperationException();
    }

    @Getter
    @Builder
    public static class GithubSecrets {
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
