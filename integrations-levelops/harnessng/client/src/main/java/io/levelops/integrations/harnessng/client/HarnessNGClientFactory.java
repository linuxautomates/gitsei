package io.levelops.integrations.harnessng.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.oauth.GenericTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
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
 * Use this class to obtain an instance of {@link HarnessNGClient} for a {@link IntegrationKey}.
 */
public class HarnessNGClientFactory {

    private static final String HARNESSNG = "HarnessNG";
    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";

    public static final String API_KEY_HEADER = "x-api-key";

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final LoadingCache<IntegrationKey, HarnessNGClient> clientCache;

    /**
     * constructor for {@link HarnessNGClientFactory}
     *
     * @param inventoryService {@link InventoryService} for getting integration and authentication details
     * @param objectMapper     {@link ObjectMapper} for the {@link HarnessNGClient}
     * @param okHttpClient     {@link OkHttpClient} for the  {@link HarnessNGClient}
     */
    @Builder
    HarnessNGClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.clientCache = CacheBuilder.from(SPEC).build(CacheLoader.from(this::getNewClient));
    }

    /**
     * Loads or a {@link HarnessNGClient} corresponding to the {@link IntegrationKey}
     * from the {@link LoadingCache<IntegrationKey,HarnessNGClient>}
     *
     * @param key {@link IntegrationKey} for the request
     * @return {@link HarnessNGClient} corresponding to the {@code key}
     * @throws HarnessNGClientException for any during creation of a client
     */
    public HarnessNGClient get(final IntegrationKey key) throws HarnessNGClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new HarnessNGClientException(e);
        }
    }

    public HarnessNGClient buildFromToken(String tenantId, Integration integration, Token token)
            throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, List.of(token));
        return HarnessNGClient.builder()
                .objectMapper(objectMapper)
                .resourceUrl(integration.getUrl())
                .okHttpClient(authenticatedHttpClient)
                .build();
    }

    /**
     * creates a new {@link HarnessNGClient} for the {@link IntegrationKey}. It uses the {@link Integration} and
     * {@link List < Token >} fetched from the {@link InventoryService} for creating the client.
     *
     * @param integrationKey {@link IntegrationKey} for which the new client is to be created
     * @return {@link HarnessNGClient} created client
     */
    private HarnessNGClient getNewClient(final IntegrationKey integrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(integrationKey);
            Integration integration = inventoryService.getIntegration(integrationKey);
            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, tokens);
            return HarnessNGClient.builder()
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
     * @param integrationKey the {@link IntegrationKey} for which to build the {@link HarnessNGClient}
     * @param tokens         {@link List<Token>} list of different authentication tokens
     * @return {@link OkHttpClient} with authentication interceptor
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens)
            throws InventoryException {
        return InventoryHelper.handleTokens(HARNESSNG, integrationKey, tokens,
                InventoryHelper.TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token token, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                        .addInterceptor(new GenericTokenInterceptor(API_KEY_HEADER, null, StaticOauthTokenProvider.builder()
                            .token(apiKey.getApiKey())
                            .build()))
                        .build()));
    }
}
