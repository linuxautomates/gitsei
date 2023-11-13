package io.harness.authz.acl.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Builder;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ACLClientFactory {

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final String aclUrl;
    private LoadingCache<String, ACLClient> cache;

    @Builder
    public ACLClientFactory(ObjectMapper objectMapper, OkHttpClient okHttpClient, String aclUrl) {
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.aclUrl = aclUrl;
        this.cache = CacheBuilder.from("maximumSize=500,expireAfterWrite=15m")
                .build(CacheLoader.from(this::getACLClient));
    }

    public ACLClient get(String token){
        try {
            return cache.get(token);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private ACLClient getACLClient(String token) {
        return buildACLClient(token);
    }

    private ACLClient buildACLClient(String token) {

        OkHttpClient httpClient = this.okHttpClient.newBuilder()
                .addInterceptor(new Interceptor() {
                    @NotNull
                    @Override
                    public Response intercept(@NotNull Chain chain) throws IOException {
                        Request request = chain.request()
                                .newBuilder()
                                .addHeader("Authorization", "IdentityService " + token)
                                .build();
                        return chain.proceed(request);
                    }
                }).build();

        return ACLClient.builder()
                .okHttpClient(httpClient)
                .objectMapper(objectMapper)
                .aclUrl(aclUrl)
                .build();
    }
}
