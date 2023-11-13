package io.levelops.commons.client.oauth;

import okhttp3.Interceptor;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class GenericTokenInterceptor implements Interceptor {

    private final String header;
    private final String valuePrefix;
    private final OauthTokenProvider tokenProvider;

    public GenericTokenInterceptor(String header, String valuePrefix, OauthTokenProvider tokenProvider) {
        Validate.notBlank(header, "header cannot be null or empty.");
        Validate.notNull(tokenProvider, "tokenProvider cannot be null.");
        this.header = header;
        this.valuePrefix = StringUtils.defaultString(valuePrefix);
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
                .header(header, valuePrefix + token)
                .build();
        return chain.proceed(authenticatedRequest);
    }
}
