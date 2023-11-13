package io.levelops.integrations.bitbucket_server.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.oauth.BasicAuthInterceptor;
import io.levelops.commons.client.retrying.RetryingInterceptor;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.inventory.utils.InventoryHelper;
import io.levelops.ingestion.models.IntegrationType;
import lombok.Builder;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BitbucketServerClientFactory {

    static final int DEFAULT_PAGE_SIZE = 250;

    private InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private LoadingCache<Pair<IntegrationKey, Boolean>, BitbucketServerClient> clientCache;
    private final int pageSize;
    private final Boolean allowUnsafeSSL;

    @Builder
    public BitbucketServerClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                        OkHttpClient okHttpClient, int pageSize, @Nullable Boolean allowUnsafeSSL) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.allowUnsafeSSL = allowUnsafeSSL != null ? allowUnsafeSSL : true;
        this.pageSize = pageSize != 0 ? pageSize : DEFAULT_PAGE_SIZE;
        this.clientCache = CacheBuilder.from("maximumSize=250,expireAfterWrite=15m")
                .build(CacheLoader.from(this::getInternal));
    }

    public BitbucketServerClient get(IntegrationKey integrationKey, boolean realtime) throws BitbucketServerClientException {
        try {
            return clientCache.get(Pair.of(integrationKey, realtime));
        } catch (ExecutionException e) {
            throw new BitbucketServerClientException(e);
        }
    }

    private BitbucketServerClient getInternal(Pair<IntegrationKey, Boolean> cacheKey) {
        Validate.notNull(cacheKey, "cache key cannot be null");
        IntegrationKey key = cacheKey.getLeft();
        Boolean realtime = cacheKey.getRight();
        Validate.notNull(key, "key cannot be null");
        Validate.notNull(realtime, "realtime cannot be null");
        try {
            List<Token> tokens = inventoryService.listTokens(key);
            Integration integration = inventoryService.getIntegration(key);
            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(key, tokens, realtime);
            return BitbucketServerClient.builder()
                    .objectMapper(objectMapper)
                    .resourceUrl(integration.getUrl())
                    .okHttpClient(authenticatedHttpClient)
                    .allowUnsafeSSL(allowUnsafeSSL)
                    .pageSize(pageSize)
                    .build();

        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens, boolean realtime)
            throws InventoryException {
        return InventoryHelper.handleTokens(IntegrationType.BITBUCKET_SERVER.name(), integrationKey, tokens,
                InventoryHelper.TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token token, ApiKey apiKey) ->
                        this.okHttpClient
                                .newBuilder()
                                .addInterceptor(new BasicAuthInterceptor(apiKey.getAuthorizationHeader()))
                                .addInterceptor(realtime ? RetryingInterceptor.buildDefaultRealtimeRetryer() : RetryingInterceptor.buildDefaultRetryer())
                                .build()));
    }

    public BitbucketServerClient buildFromToken(String tenantId, Integration integration, Token token, boolean realtime)
            throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, List.of(token), realtime);
        return BitbucketServerClient.builder()
                .objectMapper(objectMapper)
                .resourceUrl(integration.getUrl())
                .okHttpClient(authenticatedHttpClient)
                .allowUnsafeSSL(allowUnsafeSSL)
                .pageSize(pageSize)
                .build();
    }
}
