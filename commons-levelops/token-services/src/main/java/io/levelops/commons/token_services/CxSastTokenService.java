package io.levelops.commons.token_services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.token_exceptions.InvalidTokenDataException;
import io.levelops.commons.token_exceptions.TokenException;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;


@Log4j2
public class CxSastTokenService extends TokenService {

    private static final String AUTHENTICATE = "cxrestapi/auth/identity/connect/token";
    private static final String ACCESS_TOKEN = "access_token";

    private final String redirectHost;
    private final CxSastTokenService.Secrets secrets;

    @Builder
    public CxSastTokenService(OkHttpClient httpClient, ObjectMapper mapper,
                              CxSastTokenService.Secrets secrets, String redirectHost) {
        super(httpClient, mapper);
        this.secrets = secrets;
        this.redirectHost = redirectHost;
    }

    @Override
    public TokenService.Tokens getTokensFromCode(String code, String state) throws TokenException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TokenService.Tokens getTokensFromRefreshToken(String refreshToken) throws TokenException {
        throw new UnsupportedOperationException();
    }

    public TokenService.Tokens generateToken(String refreshToken, String oauthUrl) throws TokenException {
        String[] split = refreshToken.split(":");
        String username = split[0];
        String password = split[1];
        log.debug("Get Token From Code... username={}", username);
        if (StringUtils.isEmpty(username)) {
            throw new InvalidTokenDataException("Bad request. Missing username in request.");
        }
        if (StringUtils.isEmpty(password)) {
            throw new InvalidTokenDataException("Bad request. Missing password in request.");
        }
        HttpUrl.Builder urlBuilder = HttpUrl.parse(oauthUrl).newBuilder()
                .addPathSegment(AUTHENTICATE);
        RequestBody body = new FormBody.Builder()
                .addEncoded("username", username)
                .addEncoded("password", password)
                .addEncoded("grant_type", "password")
                .addEncoded("scope", "sast_rest_api")
                .addEncoded("client_id", secrets.getClientId())
                .addEncoded("client_secret", secrets.getClientSecret())
                .build();
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .post(body)
                .build();
        String rawResponse = getClientHelper().executeRequest(request);
        Map<String, String> result = getClientHelper().parseResponse(rawResponse,
                DefaultObjectMapper.get().getTypeFactory().constructParametricType(HashMap.class,
                        String.class, String.class));
        return TokenService.Tokens.builder()
                .accessToken(result.get(ACCESS_TOKEN))
                .refreshToken(refreshToken)
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
