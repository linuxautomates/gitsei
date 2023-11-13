package io.levelops.commons.token_services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.token_exceptions.TokenException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import okhttp3.OkHttpClient;

@Getter
public abstract class TokenService {
    private final ObjectMapper objectMapper;
    private final ClientHelper<TokenException> clientHelper;

    TokenService(OkHttpClient httpClient, ObjectMapper mapper) {
        this.objectMapper = mapper;
        this.clientHelper = ClientHelper.<TokenException>builder()
                .client(httpClient)
                .objectMapper(mapper)
                .exception(TokenException.class)
                .build();
    }

    public abstract Tokens getTokensFromCode(String code, String state) throws TokenException;

    public abstract Tokens getTokensFromRefreshToken(String refreshToken) throws TokenException;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @EqualsAndHashCode
    @ToString
    public static class Tokens {
        private String accessToken;
        private String botToken;
        private String refreshToken;
        private String instanceUrl;
        private String rawTokenResponse;
        private String teamId;
    }

}
