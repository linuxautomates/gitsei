package io.levelops.integrations.prometheus.client;

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
import lombok.Builder;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PrometheusClientFactory {

    private static final String PROMETHEUS = "Prometheus";
    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";
    private static final int MAX_ATTEMPTS = 10;
    private static final int MULTIPLIER_MS = 10000;
    private static final int MAX_WAIT = 90;

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final LoadingCache<IntegrationKey, PrometheusClient> clientCache;

    @Builder
    public PrometheusClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                   OkHttpClient okHttpClient) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.clientCache = CacheBuilder.from(SPEC).build(CacheLoader.from(this::getNewClient));
    }

    public PrometheusClient buildFromToken(String tenantId, Integration integration, Token token)
            throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, List.of(token));
        return PrometheusClient.builder()
                .objectMapper(objectMapper)
                .resourceUrl(integration.getUrl())
                .okHttpClient(authenticatedHttpClient)
                .build();
    }

    public PrometheusClient get(final IntegrationKey key) throws PrometheusClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new PrometheusClientException(e);
        }
    }

    private PrometheusClient getNewClient(final IntegrationKey integrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(integrationKey);
            Integration integration = inventoryService.getIntegration(integrationKey);
            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, tokens);
            return PrometheusClient.builder()
                    .objectMapper(objectMapper)
                    .okHttpClient(authenticatedHttpClient)
                    .resourceUrl(integration.getUrl())
                    .build();
        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens)
            throws InventoryException {
        return InventoryHelper.handleTokens(PROMETHEUS, integrationKey, tokens,
                InventoryHelper.TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token token, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                        .addInterceptor(new BasicAuthInterceptor(apiKey.getAuthorizationHeader()))
                        .addInterceptor(RetryingInterceptor.build429Retryer(MAX_ATTEMPTS, MULTIPLIER_MS, MAX_WAIT,
                                TimeUnit.SECONDS))
                        .build()));
    }
}