package io.levelops.integrations.helix_swarm.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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

public class HelixSwarmClientFactory {

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";
    private static final String HELIX_SWARM = "helix_swarm";
    private static final int MULTIPLIER_MS = 100;
    private static final int MAX_WAIT = 5;
    private static final int MAX_ATTEMPTS = 10;

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final LoadingCache<IntegrationKey, HelixSwarmClient> clientCache;
    private final int pageSize;

    @Builder
    public HelixSwarmClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                   OkHttpClient okHttpClient, int pageSize) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.pageSize = pageSize != 0 ? pageSize : DEFAULT_PAGE_SIZE;
        this.clientCache = CacheBuilder.from(SPEC).build(CacheLoader.from(this::getNewClient));
    }

    public HelixSwarmClient get(final IntegrationKey key) throws HelixSwarmClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new HelixSwarmClientException(e);
        }
    }

    private HelixSwarmClient getNewClient(final IntegrationKey integrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(integrationKey);
            Integration integration = inventoryService.getIntegration(integrationKey);
            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, tokens, integration);
            return HelixSwarmClient.builder()
                    .objectMapper(objectMapper)
                    .resourceUrl(String.valueOf(integration.getMetadata().get("helix_swarm_url")))
                    .okHttpClient(authenticatedHttpClient)
                    .pageSize(pageSize)
                    .build();
        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    public HelixSwarmClient buildFromToken(String tenantId, Integration integration, Token token)
            throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, List.of(token), integration);
        return HelixSwarmClient.builder()
                .objectMapper(objectMapper)
                .resourceUrl(String.valueOf(integration.getMetadata().get("helix_swarm_url")))
                .okHttpClient(authenticatedHttpClient)
                .pageSize(pageSize)
                .build();
    }

    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens, Integration integration)
            throws InventoryException {
        return InventoryHelper.handleTokens(HELIX_SWARM, integrationKey, tokens,
                InventoryHelper.TokenHandler.forType(ApiKey.TOKEN_TYPE,
                        (Token token, ApiKey apiKey) -> {
                            HelixCoreTicketProvider ticketProvider = HelixCoreTicketProvider.builder()
                                    .swarmUsername(apiKey.getUserName())
                                    .swarmPassword(apiKey.getApiKey())
                                    .ticket(apiKey.getAuthorizationHeader())
                                    .metadata(integration.getMetadata())
                                    .url(integration.getUrl())
                                    .integrationId(integration.getId() != null ? integration.getId() : "")
                                    .build();
                            return this.okHttpClient.newBuilder()
                                    .addInterceptor(new HelixSwarmTokenInterceptor(ticketProvider))
                                    .addInterceptor(RetryingInterceptor.build429Retryer(MAX_ATTEMPTS,
                                            MULTIPLIER_MS, MAX_WAIT, TimeUnit.SECONDS))
                                    .build();
                        }));
    }
}
