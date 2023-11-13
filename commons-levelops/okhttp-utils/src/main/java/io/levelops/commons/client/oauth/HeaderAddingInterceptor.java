package io.levelops.commons.client.oauth;

import lombok.AllArgsConstructor;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@AllArgsConstructor
public class HeaderAddingInterceptor implements Interceptor {
    private final String header;
    private final String value;

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        return chain.proceed(chain.request().newBuilder()
                .header(header, value)
                .build());
    }
}