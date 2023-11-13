package io.levelops.integrations.circleci.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.oauth.BasicAuthInterceptor;
import io.levelops.commons.client.oauth.HeaderAddingInterceptor;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Use this class to obtain an instance of {@link CircleCIClient} for a {@link IntegrationKey}.
 */
public class CircleCIClientFactory {

    private static final String CIRCLECI = "CircleCI";
    private static final String CIRCLECI_TOKEN = "Circle-Token";
    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";

    private static final int MAX_ATTEMPTS = 10;
    private static final int MULTIPLIER_MS = 10000;
    private static final int MAX_WAIT = 90;

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final LoadingCache<IntegrationKey, CircleCIClient> clientCache;

    /**
     * constructor for {@link CircleCIClientFactory}
     *
     * @param inventoryService {@link InventoryService} for getting integration and authentication details
     * @param objectMapper     {@link ObjectMapper} for the {@link CircleCIClient}
     * @param okHttpClient     {@link OkHttpClient} for the  {@link CircleCIClient}
     */
    @Builder
    CircleCIClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.clientCache = CacheBuilder.from(SPEC).build(CacheLoader.from(this::getNewClient));
    }

    /**
     * Loads or a {@link CircleCIClient} corresponding to the {@link IntegrationKey}
     * from the {@link LoadingCache<IntegrationKey,CircleCIClient>}
     *
     * @param key {@link IntegrationKey} for the request
     * @return {@link CircleCIClient} corresponding to the {@code key}
     * @throws CircleCIClientException for any during creation of a client
     */
    public CircleCIClient get(final IntegrationKey key) throws CircleCIClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new CircleCIClientException(e);
        }
    }

    public CircleCIClient buildFromToken(String tenantId, Integration integration, Token token)
            throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, List.of(token));
        return CircleCIClient.builder()
                .objectMapper(objectMapper)
                .resourceUrl(integration.getUrl())
                .okHttpClient(authenticatedHttpClient)
                .build();
    }

    /**
     * creates a new {@link CircleCIClient} for the {@link IntegrationKey}. It uses the {@link Integration} and
     * {@link List < Token >} fetched from the {@link InventoryService} for creating the client.
     *
     * @param integrationKey {@link IntegrationKey} for which the new client is to be created
     * @return {@link CircleCIClient} created client
     */
    private CircleCIClient getNewClient(final IntegrationKey integrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(integrationKey);
            Integration integration = inventoryService.getIntegration(integrationKey);
            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, tokens);
            return CircleCIClient.builder()
                    .objectMapper(objectMapper)
                    .resourceUrl(integration.getUrl())
                    .okHttpClient(authenticatedHttpClient)
                    .build();
        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    /**
     * Builds an {@link OkHttpClient} for {@code integrationKey} with a {@link BasicAuthInterceptor}
     * with authentication credentials from {@code tokens}
     *
     * @param integrationKey the {@link IntegrationKey} for which to build the {@link CircleCIClient}
     * @param tokens         {@link List<Token>} list of different authentication tokens
     * @return {@link OkHttpClient} with authentication interceptor
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens)
            throws InventoryException {
        return InventoryHelper.handleTokens(CIRCLECI, integrationKey, tokens,
                InventoryHelper.TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token token, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                        .addInterceptor(new HeaderAddingInterceptor(CIRCLECI_TOKEN, apiKey.getApiKey()))
                        .addInterceptor(circleCiRetryer(MAX_ATTEMPTS, MULTIPLIER_MS, MAX_WAIT,
                                TimeUnit.SECONDS))
                        .build()));
    }

    public static RetryingInterceptor circleCiRetryer(int maxAttempts, long multiplierMs, long maxWait, TimeUnit maxWaitTimeUnit) {
        return RetryingInterceptor.builder()
                .maxAttempts(maxAttempts)
                .multiplierMs(multiplierMs)
                .maxWait(maxWait)
                .maxWaitTimeUnit(maxWaitTimeUnit)
                .retryPredicate(response -> response.code() == 429 || response.code() == 503)
                .waitMsExtractor(response -> {
                    if (StringUtils.isNotEmpty(response.header("Retry-After"))) {
                        if (response.code() == 429) {
                            return Long.parseLong(response.header("Retry-After", String.valueOf(multiplierMs)));
                        } else if (response.code() == 503) {
                            return Duration.between(response.headers().getDate("Retry-After").toInstant(), Instant.now()).toMillis();
                        } else {
                            return multiplierMs;
                        }
                    } else {
                        return multiplierMs;
                    }
                })
                .build();
    }
}
