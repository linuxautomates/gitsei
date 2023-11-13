package io.levelops.commons.client.throttling;

import java.io.IOException;

import com.google.common.util.concurrent.RateLimiter;

import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class ThrottlingInterceptor implements Interceptor {

    private final RateLimiter rateLimiter;

    public ThrottlingInterceptor(double operationsPerSecond) {
        this.rateLimiter = RateLimiter.create(operationsPerSecond);
    }

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        rateLimiter.acquire(1);
        return chain.proceed(chain.request());
    }
}