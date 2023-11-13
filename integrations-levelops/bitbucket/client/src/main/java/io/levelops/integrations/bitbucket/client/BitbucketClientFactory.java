package io.levelops.integrations.bitbucket.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.oauth.OauthTokenAuthenticator;
import io.levelops.commons.client.oauth.OauthTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
import io.levelops.commons.client.retrying.RetryingInterceptor;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.inventory.oauth.InventoryOauthTokenProvider;
import io.levelops.commons.inventory.utils.InventoryHelper;
import lombok.Builder;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static io.levelops.commons.inventory.utils.InventoryHelper.TokenHandler.forType;

public class BitbucketClientFactory {
    private InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private LoadingCache<Pair<IntegrationKey, Boolean>, BitbucketClient> clientCache;

    @Builder
    public BitbucketClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.clientCache = CacheBuilder.from("maximumSize=250,expireAfterWrite=15m")
                .build(CacheLoader.from(this::getInternal));
    }

    public BitbucketClient get(IntegrationKey integrationKey, boolean realtime) throws BitbucketClientException {
        try {
            return clientCache.get(Pair.of(integrationKey, realtime));
        } catch (ExecutionException e) {
            throw new BitbucketClientException(e);
        }
    }

    private BitbucketClient getInternal(Pair<IntegrationKey, Boolean> cacheKey) {
        Validate.notNull(cacheKey, "key cannot be null");
        IntegrationKey key = cacheKey.getLeft();
        Boolean realtime = cacheKey.getRight();
        try {
//            Integration integration = inventoryService.getIntegration(key);
            List<Token> tokens = inventoryService.listTokens(key);

            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(key, tokens, realtime);
            return BitbucketClient.builder()
                    .objectMapper(objectMapper)
                    .okHttpClient(authenticatedHttpClient)
                    .build();

        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens, boolean realtime)
            throws InventoryException {
        RetryingInterceptor retryingInterceptor = RetryingInterceptor.buildDefaultRetryer();
        if (realtime) {
            retryingInterceptor = RetryingInterceptor.buildDefaultRealtimeRetryer();
        }
        RetryingInterceptor finalRetryingInterceptor = retryingInterceptor;
        return InventoryHelper.handleTokens("Bitbucket", integrationKey, tokens,
                forType(OauthToken.TOKEN_TYPE, (Token token, OauthToken oauth) -> {
                    InventoryOauthTokenProvider tokenProvider = InventoryOauthTokenProvider.builder()
                            .inventoryService(inventoryService)
                            .integrationKey(integrationKey)
                            .tokenId(token.getId())
                            .build();
                    return this.okHttpClient.newBuilder()
                            .addInterceptor(new OauthTokenInterceptor(tokenProvider))
                            .authenticator(new OauthTokenAuthenticator(tokenProvider))
                            .addInterceptor(finalRetryingInterceptor)
                            .build();
                }));
    }

    public BitbucketClient buildFromToken(String tenantId, Integration integration, Token token, boolean realtime)
            throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        List<Token> tokens = List.of(token);
        OkHttpClient authenticatedHttpClient = InventoryHelper.handleTokens("Bitbucket", integrationKey, tokens,
                forType(OauthToken.TOKEN_TYPE, (Token t, OauthToken oauth) -> {
                    StaticOauthTokenProvider tokenProvider = StaticOauthTokenProvider.builder()
                            .token(oauth.getToken())
                            .build();
                    return this.okHttpClient.newBuilder()
                            .addInterceptor(new OauthTokenInterceptor(tokenProvider))
                            .authenticator(new OauthTokenAuthenticator(tokenProvider))
                            .addInterceptor(RetryingInterceptor.buildDefaultRetryer())
                            .build();
                }));
        return BitbucketClient.builder()
                .objectMapper(objectMapper)
                .okHttpClient(authenticatedHttpClient)
                .build();
    }

}
