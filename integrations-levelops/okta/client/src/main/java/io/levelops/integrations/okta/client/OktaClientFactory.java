package io.levelops.integrations.okta.client;

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

/**
 * Factory for obtaining {@link OktaClient} for given {@link IntegrationKey}
 */
public class OktaClientFactory {

    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";
    public static final int DEFAULT_PAGE_SIZE = 200;

    private static final int MAX_ATTEMPTS = 10;
    private static final int MULTIPLIER_MS = 10000;
    private static final int MAX_WAIT = 90;

    private static final String OKTA = "okta";
    private static final String SSWS = "SSWS ";
    private static final String AUTHORIZATION = "Authorization";
    private static final String ENRICH = "enrich";

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final int pageSize;

    private final LoadingCache<IntegrationKey, OktaClient> clientCache;

    /**
     * @param inventoryService {@link InventoryService} for getting integration and authentication details
     * @param objectMapper     {@link ObjectMapper} for the {@link OktaClient}
     * @param okHttpClient     {@link OkHttpClient} for the  {@link OktaClient}
     * @param pageSize         response page size
     */
    @Builder
    public OktaClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                             OkHttpClient okHttpClient, int pageSize) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.pageSize = pageSize != 0 ? pageSize : DEFAULT_PAGE_SIZE;
        this.clientCache = CacheBuilder.from(SPEC).build(CacheLoader.from(this::getNewClient));
    }

    /**
     * Loads a {@link OktaClient} corresponding to the {@link IntegrationKey}
     * from the {@see LoadingCache<IntegrationKey, TenableClient>}
     *
     * @param key {@link IntegrationKey} for the request
     * @return {@link OktaClient} corresponding to the {@link IntegrationKey}
     * @throws OktaClientException for any exception during creation of a client
     */
    public OktaClient get(final IntegrationKey key) throws OktaClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new OktaClientException(e);
        }
    }

    /**
     * Creates a new {@link OktaClient} for the {@link IntegrationKey}. It uses the {@link Integration} and
     *
     * @param integrationKey {@link IntegrationKey} for which the new client is to be created
     * @return {@link OktaClient} created client
     * @link List<Token>} fetched from the {@link InventoryService} for creating the client.
     */
    private OktaClient getNewClient(final IntegrationKey integrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(integrationKey);
            Integration integration = inventoryService.getIntegration(integrationKey);
            Boolean enrichmentEnabled = integration.getMetadata() == null ? true :
                    (Boolean) integration.getMetadata().getOrDefault(ENRICH, true);
            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, tokens);
            return OktaClient.builder()
                    .objectMapper(objectMapper)
                    .resourceUrl(integration.getUrl())
                    .okHttpClient(authenticatedHttpClient)
                    .enrichmentEnabled(enrichmentEnabled)
                    .pageSize(pageSize)
                    .build();
        } catch (InventoryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds an {@link OkHttpClient} for {@code integrationKey} with a {@link BasicAuthInterceptor}
     * with authentication credentials from {@code tokens}
     *
     * @param integrationKey the {@link IntegrationKey} for which to build the {@link OktaClient}
     * @param tokens         {@link List<Token>} list of different authentication tokens
     * @return {@link OkHttpClient} with authentication interceptor
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens)
            throws InventoryException {
        return InventoryHelper.handleTokens(OKTA, integrationKey, tokens,
                InventoryHelper.TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token token, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                        .addInterceptor(chain -> {
                            String key = SSWS + apiKey.getApiKey();
                            return chain.proceed(chain.request().newBuilder()
                                    .header(AUTHORIZATION, key).build());
                        })
                        .addInterceptor(RetryingInterceptor.build429Retryer(MAX_ATTEMPTS, MULTIPLIER_MS, MAX_WAIT,
                                TimeUnit.SECONDS))
                        .build()));
    }

    public OktaClient buildFromToken(String tenantId, Integration integration, Token token)
            throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, List.of(token));
        return OktaClient.builder()
                .objectMapper(objectMapper)
                .resourceUrl(integration.getUrl())
                .okHttpClient(authenticatedHttpClient)
                .pageSize(1)
                .enrichmentEnabled(false)
                .build();
    }

}
