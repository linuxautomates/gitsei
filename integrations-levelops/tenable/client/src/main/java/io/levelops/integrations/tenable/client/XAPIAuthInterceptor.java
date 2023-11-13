package io.levelops.integrations.tenable.client;

import lombok.AllArgsConstructor;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@AllArgsConstructor
public class XAPIAuthInterceptor implements Interceptor {
    private final String accessKey;

    private final String secretKey;

    private final String X_APIKEYS = "X-ApiKeys";

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        String apiKey = "accessKey=" + accessKey + "; " + "secretKey=" + secretKey + ";";
        return chain.proceed(chain.request().newBuilder()
                .header(X_APIKEYS, apiKey).build());
    }
}
