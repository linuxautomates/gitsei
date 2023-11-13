package io.levelops.integrations.sonarqube.client;

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

import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Use this class to obtain an instance of {@link SonarQubeClient} for a {@link IntegrationKey}.
 */
public class SonarQubeClientFactory {

    public static final int DEFAULT_PAGE_SIZE = 100;
    private static final String SONARQUBE = "SonarQube";
    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";

    private static final int MAX_ATTEMPTS = 10;
    private static final int MULTIPLIER_MS = 10000;
    private static final int MAX_WAIT = 90;

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final LoadingCache<IntegrationKey, SonarQubeClient> clientCache;
    private final int pageSize;

    /**
     * constructor for {@link SonarQubeClientFactory}
     *
     * @param inventoryService {@link InventoryService} for getting integration and authentication details
     * @param objectMapper     {@link ObjectMapper} for the {@link SonarQubeClient}
     * @param okHttpClient     {@link OkHttpClient} for the  {@link SonarQubeClient}
     * @param pageSize         response page size
     */
    @Builder
    public SonarQubeClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                  OkHttpClient okHttpClient, int pageSize) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.pageSize = pageSize != 0 ? pageSize : DEFAULT_PAGE_SIZE;
        this.clientCache = CacheBuilder.from(SPEC).build(CacheLoader.from(this::getNewClient));
    }

    /**
     * Loads or a {@link SonarQubeClient} corresponding to the {@link IntegrationKey}
     * from the {@link LoadingCache<IntegrationKey,SonarQubeClient>}
     *
     * @param key {@link IntegrationKey} for the request
     * @return {@link SonarQubeClient} corresponding to the {@code key}
     * @throws SonarQubeClientException for any during creation of a client
     */
    public SonarQubeClient get(final IntegrationKey key) throws SonarQubeClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new SonarQubeClientException(e);
        }
    }

    /**
     * creates a new {@link SonarQubeClient} for the {@link IntegrationKey}. It uses the {@link Integration} and
     * {@link List < Token >} fetched from the {@link InventoryService} for creating the client.
     *
     * @param integrationKey {@linkKey} for which the new client is to be created
     * @return {@link SonarQubeClient} created client
     */
    private SonarQubeClient getNewClient(final IntegrationKey integrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(integrationKey);
            Integration integration = inventoryService.getIntegration(integrationKey);
            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, tokens);
            String organization = integration.getMetadata() == null ? null :
                    (String) integration.getMetadata().getOrDefault("organization", null);
            String timezone = integration.getMetadata() == null ? null :
                    (String) integration.getMetadata().getOrDefault("timezone", null);
            return SonarQubeClient.builder()
                    .objectMapper(objectMapper)
                    .resourceUrl(integration.getUrl())
                    .okHttpClient(authenticatedHttpClient)
                    .organization(organization)
                    .timezone(timezone)
                    .pageSize(pageSize)
                    .build();
        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    /**
     * Builds an {@link OkHttpClient} for {@code integrationKey} with a {@link BasicAuthInterceptor}
     * with authentication credentials from {@code tokens}
     *
     * @param integrationKey the {@link IntegrationKey} for which to build the {@link SonarQubeClient}
     * @param tokens         {@link List<Token>} list of different authentication tokens
     * @return {@link OkHttpClient} with authentication interceptor
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens)
            throws InventoryException {
        return InventoryHelper.handleTokens(SONARQUBE, integrationKey, tokens,
                InventoryHelper.TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token token, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                        .addInterceptor(new BasicAuthInterceptor(createToken(apiKey)))
                        .addInterceptor(RetryingInterceptor.buildDefaultRetryer())
                        .build()));
    }

    /**
     * builds a {@link SonarQubeClient} with authenticated {@link OkHttpClient}for an {@link Integration}
     * with the authentication credentials from {@link Token}. Builds a new {@link SonarQubeClient} on each call.
     *
     * @param tenantId    {@link String} id of the tenant
     * @param integration {@link Integration} for which to build the client for
     * @param token       {@link Token} containing the credentials
     * @return {@link SonarQubeClient} built for the {@code integration} using {@code token}
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    public SonarQubeClient buildFromToken(String tenantId, Integration integration,
                                          Token token, int pageSize) throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, List.of(token));
        String organization = integration.getMetadata() == null ? null :
                (String) integration.getMetadata().getOrDefault("organization",null);
        String timezone = integration.getMetadata() == null ? null :
                (String) integration.getMetadata().getOrDefault("timezone", null);
        return SonarQubeClient.builder()
                .objectMapper(objectMapper)
                .resourceUrl(integration.getUrl())
                .okHttpClient(authenticatedHttpClient)
                .pageSize(pageSize)
                .organization(organization)
                .timezone(timezone)
                .build();
    }

    private String createToken(ApiKey apiKey) {
        String toEncode = apiKey.getApiKey()+":";
        return Base64.getEncoder().encodeToString(toEncode.getBytes());
    }
}
