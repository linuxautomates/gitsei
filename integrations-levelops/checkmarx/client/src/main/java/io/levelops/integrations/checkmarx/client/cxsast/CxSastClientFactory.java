package io.levelops.integrations.checkmarx.client.cxsast;

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
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.inventory.oauth.InventoryOauthTokenProvider;
import io.levelops.commons.inventory.utils.InventoryHelper;
import lombok.Builder;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.Validate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CxSastClientFactory {
    private static final String CX_SAST = "CxSast";
    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";

    private static final int MAX_ATTEMPTS = 10;
    private static final int MULTIPLIER_MS = 10000;
    private static final int MAX_WAIT = 90;

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final LoadingCache<IntegrationKey, CxSastClient> clientCache;

    /**
     * constructor for {@link CxSastClientFactory}
     *
     * @param inventoryService {@link InventoryService} for getting integration and authentication details
     * @param objectMapper     {@link ObjectMapper} for the {@link CxSastClient}
     * @param okHttpClient     {@link OkHttpClient} for the  {@link CxSastClient}
     */
    @Builder
    public CxSastClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                               OkHttpClient okHttpClient) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.clientCache = CacheBuilder.from(SPEC)
                //ToDo: Need to decide the expiration for CxSast
                //.expireAfterWrite(Duration.ofHours(1))//since the token expires in one hour
                .build(CacheLoader.from(this::getNewClient));
    }

    /**
     * Loads or a {@link CxSastClient} corresponding to the {@link IntegrationKey}
     * from the {@see LoadingCache<IntegrationKey, CxSastClient>}
     *
     * @param key {@link IntegrationKey} for the request
     * @return {@link CxSastClient} corresponding to the {@code key}
     * @throws CxSastClientException for any during creation of a client
     */
    public CxSastClient get(final IntegrationKey key) throws CxSastClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new CxSastClientException(e);
        }
    }

    /**
     * creates a new {@link CxSastClient} for the {@link IntegrationKey}. It uses the {@link Integration} and
     * {@link List < Token >} fetched from the {@link InventoryService} for creating the client.
     *
     * @param integrationKey {@link IntegrationKey} for which the new client is to be created
     * @return {@link CxSastClient} created client
     */
    private CxSastClient getNewClient(final IntegrationKey integrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(integrationKey);
            Integration integration = inventoryService.getIntegration(integrationKey);
            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, tokens);
            String organization = integration.getMetadata() == null ? null :
                    (String) integration.getMetadata().getOrDefault("organization", null);
            return CxSastClient.builder()
                    .objectMapper(objectMapper)
                    .resourceUrl(integration.getUrl())
                    .okHttpClient(authenticatedHttpClient)
                    .organization(organization)
                    .build();
        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    /**
     * Builds an {@link OkHttpClient} for {@code integrationKey} with a {@link BasicAuthInterceptor}
     * with authentication credentials from {@code tokens}
     *
     * @param integrationKey the {@link IntegrationKey} for which to build the {@link CxSastClient}
     * @param tokens         {@link List<Token>} list of different authentication tokens
     * @return {@link OkHttpClient} with authentication interceptor
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens)
            throws InventoryException {
        return InventoryHelper.handleTokens(CX_SAST, integrationKey, tokens,
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
                }));
    }

    /**
     * builds a {@link CxSastClient} with authenticated {@link OkHttpClient}for an {@link Integration}
     * with the authentication credentials from {@link Token}. Builds a new {@link CxSastClient} on each call.
     *
     * @param tenantId    {@link String} id of the tenant
     * @param integration {@link Integration} for which to build the client for
     * @param token       {@link Token} containing the credentials
     * @return {@link CxSastClient} built for the {@code integration} using {@code token}
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    public CxSastClient buildFromToken(String tenantId, Integration integration,
                                       Token token) throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, List.of(token));
        String organization = integration.getMetadata() == null ? null :
                (String) integration.getMetadata().getOrDefault("organization", null);
        return CxSastClient.builder()
                .objectMapper(objectMapper)
                .resourceUrl(integration.getUrl())
                .okHttpClient(authenticatedHttpClient)
                .organization(organization)
                .build();
    }

}
