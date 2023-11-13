package io.levelops.commons.client.oauth;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GenericTokenAuthenticator implements Authenticator {

    private final String header;
    private final String valuePrefix;
    private final OauthTokenProvider tokenProvider;

    public GenericTokenAuthenticator(String header, String valuePrefix, OauthTokenProvider tokenProvider) {
        Validate.notBlank(header, "header cannot be null or empty.");
        Validate.notNull(tokenProvider, "tokenProvider cannot be null.");
        this.header = header;
        this.valuePrefix = StringUtils.defaultString(valuePrefix);
        this.tokenProvider = tokenProvider;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NotNull Response response) {
        // We need to have a token in order to refresh it.
        String token = tokenProvider.getToken();
        if (token == null) {
            return null;
        }

        synchronized (this) {
            String newToken = tokenProvider.getToken();

            // Check if the request made was previously made as an authenticated request.
            if (response.request().header(header) == null) {
                return null;
            }

            // If the token has changed since the request was made, use the new token.
            if (!newToken.equals(token)) {
                return response.request()
                        .newBuilder()
                        .header(header, valuePrefix + newToken)
                        .build();
            }

            String updatedToken = tokenProvider.refreshToken();
            if (updatedToken == null) {
                return null;
            }

            // Retry the request with the new token.
            return response.request()
                    .newBuilder()
                    .header(header, valuePrefix + updatedToken)
                    .build();
        }
    }
}
