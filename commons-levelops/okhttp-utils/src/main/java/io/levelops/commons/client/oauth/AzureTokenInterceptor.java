package io.levelops.commons.client.oauth;

import io.levelops.commons.client.CloseableUtils;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@Log4j2
public class AzureTokenInterceptor implements Interceptor {

    private final String header;
    private final String valuePrefix;
    private final OauthTokenProvider tokenProvider;

    public AzureTokenInterceptor(String header, String valuePrefix, OauthTokenProvider tokenProvider) {
        Validate.notBlank(header, "header cannot be null or empty.");
        Validate.notNull(tokenProvider, "tokenProvider cannot be null.");
        this.header = header;
        this.valuePrefix = StringUtils.defaultString(valuePrefix);
        this.tokenProvider = tokenProvider;
    }

    /*
    If token service has updated token return it
    Else, force token service to refresh token
    If  refreshed token is available return it
    If refreshed token is not avaialable return null
     */
    private String getNewToken(final String existingToken) {
        String latestToken = tokenProvider.getToken();
        if((StringUtils.isNotBlank(latestToken)) && !latestToken.equals(existingToken)) {
            // If the token has changed since the request was made, use the new token.
            return latestToken;
        }
        String updatedToken = tokenProvider.refreshToken();
        return updatedToken;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        var token = tokenProvider.getToken();
        if (token == null) {
            return chain.proceed(chain.request());
        }
        var authenticatedRequest = chain.request()
                .newBuilder()
                .addHeader(header, valuePrefix + token)
                .build();
        Response response = chain.proceed(authenticatedRequest);
        // If response code is 203 or 401 the azure token has expired
        boolean azureTokenHasExpired = (response.code() == 203) || (response.code() == 401);
        if (! azureTokenHasExpired) {
            //If azure token has not expired return response
            return response;
        }
        synchronized (this) {
            String newToken = getNewToken(token);
            if(newToken == null) {
                return response;
            }
            CloseableUtils.closeQuietly(response);
            Request newRequest = response.request()
                    .newBuilder()
                    .header(header, valuePrefix + newToken)
                    .build();
            return chain.proceed(newRequest);
        }
    }
}
