package io.levelops.commons.token_services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.token_exceptions.InvalidTokenDataException;
import io.levelops.commons.token_exceptions.TokenException;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Log4j2
public class BlackDuckTokenService extends TokenService {

    private static final String API = "api";
    private static final String TOKENS = "tokens";
    private static final String AUTHENTICATE = "authenticate";
    private static final String ACCESS_TOKEN = "bearerToken";

    private final ObjectMapper objectMapper;

    private final ClientHelper<TokenException> clientHelper;

    @Builder
    public BlackDuckTokenService(OkHttpClient httpClient, ObjectMapper mapper) {
        super(httpClient, mapper);
        try {
            httpClient = ClientHelper.configureToIgnoreCertificate(httpClient.newBuilder()).build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.warn("Failed to configure BlackDuck client to ignore SSL certificate validation", e);
        }
        this.objectMapper = mapper;
        this.clientHelper = ClientHelper.<TokenException>builder()
                .client(httpClient)
                .objectMapper(objectMapper)
                .exception(TokenException.class)
                .build();
    }

    @Override
    public Tokens getTokensFromCode(String code, String state) throws TokenException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Tokens getTokensFromRefreshToken(String refreshToken) throws TokenException {
        throw new UnsupportedOperationException();
    }

    public TokenService.Tokens generateToken(String apiKey, String resourceURL) throws TokenException {
        log.debug("Get Token From Apikey...");
        if (StringUtils.isEmpty(apiKey)) {
            throw new InvalidTokenDataException("Bad request. Missing apikey in request.");
        }
        if (StringUtils.isEmpty(resourceURL)) {
            throw new InvalidTokenDataException("Bad request. Missing resourceURL in request.");
        }
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(resourceURL)).newBuilder()
                .addPathSegment(API)
                .addPathSegment(TOKENS)
                .addPathSegment(AUTHENTICATE);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .addHeader(ClientConstants.AUTHORIZATION, "token " + apiKey)
                .post(new FormBody.Builder().build())
                .build();
        String rawResponse = clientHelper.executeRequest(request);
        Map<String, String> result = clientHelper.parseResponse(rawResponse,
                objectMapper.getTypeFactory().constructParametricType(HashMap.class,
                        String.class, String.class));
        return TokenService.Tokens.builder()
                .accessToken(result.get(ACCESS_TOKEN))
                .refreshToken(apiKey)
                .rawTokenResponse(rawResponse)
                .build();
    }
}
