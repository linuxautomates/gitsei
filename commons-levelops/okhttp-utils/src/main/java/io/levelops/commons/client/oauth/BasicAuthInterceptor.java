package io.levelops.commons.client.oauth;

import io.levelops.commons.client.ClientConstants;
import lombok.AllArgsConstructor;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@AllArgsConstructor
public class BasicAuthInterceptor implements Interceptor {
    private final String token;

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        return chain.proceed(chain.request().newBuilder()
                .header(ClientConstants.AUTHORIZATION, ClientConstants.BASIC_ + token)
                .build());
    }
}