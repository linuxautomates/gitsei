package io.levelops.integrations.snyk.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.oauth.GenericTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
import io.levelops.commons.client.retrying.RetryingInterceptor;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.inventory.utils.InventoryHelper;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.levelops.commons.inventory.utils.InventoryHelper.TokenHandler.forType;

@Log4j2
public class SnykClientFactory {
    private static final String API_KEY_HEADER = "Authorization";

    private static final int MAX_ATTEMPTS = 10;
    private static final int MULTIPLIER_MS = 30000;
    private static final int MAX_WAIT = 90;

    private InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private LoadingCache<IntegrationKey, SnykClient> clientCache;

    @Builder
    public SnykClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.clientCache = CacheBuilder.from("maximumSize=250,expireAfterWrite=15m")
                .build(CacheLoader.from(this::getInternal));
    }

    public SnykClient get(IntegrationKey integrationKey) throws SnykClientException {
        try {
            return clientCache.get(integrationKey);
        } catch (ExecutionException e) {
            throw new SnykClientException(e);
        }
    }

    private SnykClient getInternal(IntegrationKey key) {
        try {
            return buildFromInventory(key);
        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    public SnykClient buildFromInventory(IntegrationKey key) throws InventoryException {
        Validate.notNull(key, "key cannot be null");
        List<Token> tokens = inventoryService.listTokens(key);
        return SnykClient.builder()
                .objectMapper(objectMapper)
                .okHttpClient(buildAuthenticatedClient(key, tokens))
                // TODO replace with commons
//                    .region(InventoryHelper.integrationUrlSupplier(inventoryService, key, 5, TimeUnit.MINUTES))
                .build();
    }

    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens) throws InventoryException {
        return InventoryHelper.handleTokens("Snyk", integrationKey, tokens,
                forType(ApiKey.TOKEN_TYPE, (Token t, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                        .addInterceptor(new GenericTokenInterceptor(API_KEY_HEADER, "token ", StaticOauthTokenProvider.builder()
                                .token(apiKey.getApiKey())
                                .build()))
                        .addInterceptor(RetryingInterceptor.build429Retryer(MAX_ATTEMPTS, MULTIPLIER_MS, MAX_WAIT,
                                TimeUnit.SECONDS))
                        .build()));
    }

    public SnykClient buildFromToken(String tenantId, Integration integration, Token token) throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey key = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        List<Token> tokens = List.of(token);
        OkHttpClient authenticatedHttpClient = InventoryHelper.handleTokens("Snyk", key, tokens,
                forType(ApiKey.TOKEN_TYPE, (Token t, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                        .addInterceptor(new GenericTokenInterceptor(API_KEY_HEADER, "token ", StaticOauthTokenProvider.builder()
                                .token(apiKey.getApiKey())
                                .build()))
                        .addInterceptor(RetryingInterceptor.build429Retryer(MAX_ATTEMPTS, MULTIPLIER_MS, MAX_WAIT,
                                TimeUnit.SECONDS))
                        .build()));

        return SnykClient.builder()
                .objectMapper(objectMapper)
                .okHttpClient(authenticatedHttpClient)
                // TODO replace with commons
//                    .region(InventoryHelper.integrationUrlSupplier(inventoryService, key, 5, TimeUnit.MINUTES))
                .build();
    }
}