package io.levelops.commons.client.oauth;

import io.levelops.commons.client.CloseableUtils;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class MSTeamsTokenInterceptor implements Interceptor {

    private final String header;
    private final String valuePrefix;
    private final OauthTokenProvider tokenProvider;

    public MSTeamsTokenInterceptor(String header, String valuePrefix, OauthTokenProvider tokenProvider) {
        Validate.notBlank(header, "header cannot be null or empty.");
        Validate.notNull(tokenProvider, "tokenProvider cannot be null.");
        this.header = header;
        this.valuePrefix = valuePrefix;
        this.tokenProvider = tokenProvider;
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

        boolean servicenowTokenHasExpired = (response.code() == 401);
        if (! servicenowTokenHasExpired) {
            return response;
        }
        synchronized (this) {
            String newToken = tokenProvider.refreshToken();
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
