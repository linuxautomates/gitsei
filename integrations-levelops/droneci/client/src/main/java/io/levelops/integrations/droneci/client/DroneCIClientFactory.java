package io.levelops.integrations.droneci.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.ClientConstants;
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
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Use this class to obtain an instance of {@link DroneCIClient} for a {@link IntegrationKey}.
 */
public class DroneCIClientFactory {

    private static final String DRONECI = "DroneCI";
    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";
    public static final int DEFAULT_PAGE_SIZE = 100;

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final LoadingCache<IntegrationKey, DroneCIClient> clientCache;
    private int pageSize;

    /**
     * constructor for {@link DroneCIClientFactory}
     *
     * @param inventoryService {@link InventoryService} for getting integration and authentication details
     * @param objectMapper     {@link ObjectMapper} for the {@link DroneCIClient}
     * @param okHttpClient     {@link OkHttpClient} for the  {@link DroneCIClient}
     */
    @Builder
    DroneCIClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient, int pageSize) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.pageSize = (pageSize != 0) ? pageSize : DEFAULT_PAGE_SIZE;
        this.clientCache = CacheBuilder.from(SPEC).build(CacheLoader.from(this::getNewClient));
    }

    /**
     * Loads or a {@link DroneCIClient} corresponding to the {@link IntegrationKey}
     * from the {@link LoadingCache<IntegrationKey,DroneCIClient>}
     *
     * @param key {@link IntegrationKey} for the request
     * @return {@link DroneCIClient} corresponding to the {@code key}
     * @throws DroneCIClientException for any during creation of a client
     */
    public DroneCIClient get(final IntegrationKey key) throws DroneCIClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new DroneCIClientException(e);
        }
    }

    public DroneCIClient buildFromToken(String tenantId, Integration integration, Token token)
            throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, List.of(token));
        return DroneCIClient.builder()
                .objectMapper(objectMapper)
                .resourceUrl(integration.getUrl())
                .okHttpClient(authenticatedHttpClient)
                .build();
    }

    /**
     * creates a new {@link DroneCIClient} for the {@link IntegrationKey}. It uses the {@link Integration} and
     * {@link List < Token >} fetched from the {@link InventoryService} for creating the client.
     *
     * @param integrationKey {@link IntegrationKey} for which the new client is to be created
     * @return {@link DroneCIClient} created client
     */
    private DroneCIClient getNewClient(final IntegrationKey integrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(integrationKey);
            Integration integration = inventoryService.getIntegration(integrationKey);
            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, tokens);
            return DroneCIClient.builder()
                    .objectMapper(objectMapper)
                    .resourceUrl(integration.getUrl())
                    .okHttpClient(authenticatedHttpClient)
                    .build();
        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    /**
     * Builds an {@link OkHttpClient} for {@code integrationKey} with a {@link GenericTokenInterceptor}
     * with authentication credentials from {@code tokens}
     *
     * @param integrationKey the {@link IntegrationKey} for which to build the {@link DroneCIClient}
     * @param tokens         {@link List<Token>} list of different authentication tokens
     * @return {@link OkHttpClient} with authentication interceptor
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens)
            throws InventoryException {
        return InventoryHelper.handleTokens(DRONECI, integrationKey, tokens,
                InventoryHelper.TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token token, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                        .addInterceptor(RetryingInterceptor.buildDefaultRetryer())
                        .addInterceptor(new GenericTokenInterceptor(ClientConstants.AUTHORIZATION, ClientConstants.BEARER_, StaticOauthTokenProvider.builder()
                            .token(apiKey.getApiKey())
                            .build()))
                        .build()));
    }
}
