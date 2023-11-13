package io.levelops.integrations.tenable.client;

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

/**
 * Factory for obtaining {@link TenableClient} for given {@link IntegrationKey}
 */
public class TenableClientFactory {

    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";
    public static final int DEFAULT_PAGE_SIZE = 256;

    private static final int MAX_ATTEMPTS = 10;
    private static final int MULTIPLIER_MS = 10000;
    private static final int MAX_WAIT = 90;

    private static final String TENABLE = "tenable";

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final int pageSize;

    private final LoadingCache<IntegrationKey, TenableClient> clientCache;

    /**
     * @param inventoryService {@link InventoryService} for getting integration and authentication details
     * @param objectMapper     {@link ObjectMapper} for the {@link TenableClient}
     * @param okHttpClient     {@link OkHttpClient} for the  {@link TenableClient}
     * @param pageSize         response page size
     */
    @Builder
    public TenableClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                OkHttpClient okHttpClient, int pageSize) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.pageSize = pageSize != 0 ? pageSize : DEFAULT_PAGE_SIZE;
        this.clientCache = CacheBuilder.from(SPEC).build(CacheLoader.from(this::getNewClient));
    }

    /**
     * Loads a {@link TenableClient} corresponding to the {@link IntegrationKey}
     * from the {@see LoadingCache<IntegrationKey, TenableClient>}
     *
     * @param key {@link IntegrationKey} for the request
     * @return {@link TenableClient} corresponding to the {@link IntegrationKey}
     * @throws TenableClientException for any exception during creation of a client
     */
    public TenableClient get(final IntegrationKey key) throws TenableClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new TenableClientException(e);
        }
    }

    /**
     * Creates a new {@link TenableClient} for the {@link IntegrationKey}. It uses the {@link Integration} and
     * @link List<Token>} fetched from the {@link InventoryService} for creating the client.
     *
     * @param integrationKey {@link IntegrationKey} for which the new client is to be created
     * @return {@link TenableClient} created client
     */
    private TenableClient getNewClient(final IntegrationKey integrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(integrationKey);
            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, tokens);
            return TenableClient.builder()
                    .objectMapper(objectMapper)
                    .okHttpClient(authenticatedHttpClient)
                    .pageSize(pageSize)
                    .build();
        } catch (InventoryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds an {@link OkHttpClient} for {@code integrationKey} with a {@link XAPIAuthInterceptor}
     * with authentication credentials from {@code tokens}
     *
     * @param integrationKey        the {@link IntegrationKey} for which to build the {@link TenableClient}
     * @param tokens                {@link List<Token>} list of different authentication tokens
     * @return                      {@link OkHttpClient} with authentication interceptor
     * @throws InventoryException   if {@code tokens} is empty or if no supported token is found for the integration
     */
    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens) throws InventoryException {
        return InventoryHelper.handleTokens(TENABLE, integrationKey, tokens,
                InventoryHelper.TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token token, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                        .addInterceptor(new XAPIAuthInterceptor(apiKey.getUserName(), apiKey.getApiKey()))
                        .addInterceptor(RetryingInterceptor.build429Retryer(MAX_ATTEMPTS, MULTIPLIER_MS, MAX_WAIT,
                                TimeUnit.SECONDS))
                        .build()));
    }

    /**
     * builds a {@link TenableClient} with authenticated {@link OkHttpClient} for an {@link Integration}
     * with the authentication credentials from {@link Token}. Builds a new {@link TenableClient} on each call.
     *
     * @param tenantId    {@link String} id of the tenant
     * @param integration {@link Integration} for which to build the client for
     * @param token       {@link Token} containing the credentials
     * @return {@link TenableClient} built for the {@code integration} using {@code token}
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    public TenableClient buildFromToken(String tenantId, Integration integration, Token token) throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, List.of(token));
        return TenableClient.builder()
                .objectMapper(objectMapper)
                .okHttpClient(authenticatedHttpClient)
                .pageSize(pageSize)
                .build();
    }
}
