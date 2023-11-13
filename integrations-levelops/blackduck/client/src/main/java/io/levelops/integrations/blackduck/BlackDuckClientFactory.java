package io.levelops.integrations.blackduck;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.oauth.BasicAuthInterceptor;
import io.levelops.commons.client.oauth.OauthTokenAuthenticator;
import io.levelops.commons.client.oauth.OauthTokenInterceptor;
import io.levelops.commons.client.retrying.RetryingInterceptor;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.inventory.oauth.InventoryOauthTokenProvider;
import io.levelops.commons.inventory.utils.InventoryHelper;
import lombok.Builder;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BlackDuckClientFactory {

    public static final int DEFAULT_PAGE_SIZE = 100;
    private static final String BLACKDUCK = "blackduck";
    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";
    private static final int MAX_ATTEMPTS = 10;
    private static final int MULTIPLIER_MS = 10000;
    private static final int MAX_WAIT = 90;

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final LoadingCache<IntegrationKey, BlackDuckClient> clientCache;
    private final int pageSize;
    private final Boolean allowUnsafeSSL;

    /**
     * constructor for {@link BlackDuckClientFactory}
     *
     * @param inventoryService {@link InventoryService} for getting integration and authentication details
     * @param objectMapper     {@link ObjectMapper} for the {@link BlackDuckClient}
     * @param okHttpClient     {@link OkHttpClient} for the  {@link BlackDuckClient}
     * @param pageSize         response page size
     */

    @Builder
    public BlackDuckClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                  OkHttpClient okHttpClient, int pageSize, @Nullable Boolean allowUnsafeSSL) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.allowUnsafeSSL = allowUnsafeSSL != null ? allowUnsafeSSL : true;
        this.pageSize = pageSize != 0 ? pageSize : DEFAULT_PAGE_SIZE;
        this.clientCache = CacheBuilder.from(SPEC).build(CacheLoader.from(this::getNewClient));
    }

    /**
     * Loads or a {@link BlackDuckClient} corresponding to the {@link IntegrationKey}
     * from the {@link LoadingCache<IntegrationKey,BlackDuckClient>}
     *
     * @param key {@link IntegrationKey} for the request
     * @return {@link BlackDuckClient} corresponding to the {@code key}
     * @throws BlackDuckClientException for any during creation of a client
     */
    public BlackDuckClient get(final IntegrationKey key) throws BlackDuckClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new BlackDuckClientException(e);
        }
    }

    /**
     * creates a new {@link BlackDuckClient} for the {@link IntegrationKey}. It uses the {@link Integration} and
     * {@link List < Token >} fetched from the {@link InventoryService} for creating the client.
     *
     * @param integrationKey {@linkKey} for which the new client is to be created
     * @return {@link BlackDuckClient} created client
     */
    private BlackDuckClient getNewClient(final IntegrationKey integrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(integrationKey);
            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, tokens);
            Integration integration = inventoryService.getIntegration(integrationKey);
            return BlackDuckClient.builder()
                    .objectMapper(objectMapper)
                    .okHttpClient(authenticatedHttpClient)
                    .resourceUrl(integration.getUrl())
                    .allowUnsafeSSL(allowUnsafeSSL)
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
     * @param integrationKey the {@link IntegrationKey} for which to build the {@link BlackDuckClient}
     * @param tokens         {@link List<Token>} list of different authentication tokens
     * @return {@link OkHttpClient} with authentication interceptor
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens)
            throws InventoryException {
        return InventoryHelper.handleTokens(BLACKDUCK, integrationKey, tokens,
                InventoryHelper.TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token token, ApiKey apiKey) -> {
                    InventoryOauthTokenProvider tokenProvider = InventoryOauthTokenProvider.builder()
                            .inventoryService(inventoryService)
                            .integrationKey(integrationKey)
                            .tokenId(token.getId())
                            .token(token)
                            .build();
                    return this.okHttpClient.newBuilder()
                            .addInterceptor(new OauthTokenInterceptor(tokenProvider))
                            .authenticator(new OauthTokenAuthenticator(tokenProvider))
                            .addInterceptor(RetryingInterceptor.build429Retryer(MAX_ATTEMPTS, MULTIPLIER_MS, MAX_WAIT,
                                    TimeUnit.SECONDS))
                            .build();
                }),
                InventoryHelper.TokenHandler.forType(OauthToken.TOKEN_TYPE, (Token token, OauthToken oauthToken) -> {
                    InventoryOauthTokenProvider tokenProvider = InventoryOauthTokenProvider.builder()
                            .inventoryService(inventoryService)
                            .integrationKey(integrationKey)
                            .tokenId(token.getId())
                            .token(token)
                            .build();
                    return this.okHttpClient.newBuilder()
                            .addInterceptor(new OauthTokenInterceptor(tokenProvider))
                            .authenticator(new OauthTokenAuthenticator(tokenProvider))
                            .addInterceptor(RetryingInterceptor.build429Retryer(MAX_ATTEMPTS, MULTIPLIER_MS, MAX_WAIT,
                                    TimeUnit.SECONDS))
                            .build();
                })
        );
    }

    /**
     * builds a {@link BlackDuckClient} with authenticated {@link OkHttpClient}for an {@link Integration}
     * with the authentication credentials from {@link Token}. Builds a new {@link BlackDuckClient} on each call.
     *
     * @param tenantId    {@link String} id of the tenant
     * @param integration {@link Integration} for which to build the client for
     * @param token       {@link Token} containing the credentials
     * @return {@link BlackDuckClient} built for the {@code integration} using {@code token}
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    public BlackDuckClient buildFromToken(String tenantId, Integration integration,
                                          Token token, int pageSize) throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, List.of(token));
        return BlackDuckClient.builder()
                .objectMapper(objectMapper)
                .resourceUrl(integration.getUrl())
                .okHttpClient(authenticatedHttpClient)
                .allowUnsafeSSL(allowUnsafeSSL)
                .pageSize(pageSize)
                .build();
    }
}
