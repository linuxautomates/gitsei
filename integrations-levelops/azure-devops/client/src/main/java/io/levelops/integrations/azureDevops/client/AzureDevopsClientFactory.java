package io.levelops.integrations.azureDevops.client;

import com.amazonaws.util.NumberUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.oauth.AzureTokenInterceptor;
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
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static io.levelops.commons.inventory.utils.InventoryHelper.TokenHandler.forType;

/**
 * Use this class to obtain an instance of {@link AzureDevopsClient} for a {@link IntegrationKey}.
 */
@Log4j2
public class AzureDevopsClientFactory {

    public static final int DEFAULT_PAGE_SIZE = 100;

    private static final String AZURE_DEVOPS = "AzureDevops";
    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final LoadingCache<IntegrationKey, AzureDevopsClient> clientCache;
    private final int pageSize;
    private final Integer throttlingIntervalMs; // disabled if <= 0 or null

    /**
     * constructor for {@link AzureDevopsClientFactory}
     *
     * @param inventoryService {@link InventoryService} for getting integration and authentication details
     * @param objectMapper     {@link ObjectMapper} for the {@link AzureDevopsClient}
     * @param okHttpClient     {@link OkHttpClient} for the  {@link AzureDevopsClient}
     * @param pageSize         response page size
     */
    @Builder
    public AzureDevopsClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                    OkHttpClient okHttpClient, int pageSize, Integer throttlingIntervalMs) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.pageSize = pageSize != 0 ? pageSize : DEFAULT_PAGE_SIZE;
        this.throttlingIntervalMs = throttlingIntervalMs;
        this.clientCache = CacheBuilder.from(SPEC).build(CacheLoader.from(this::getNewClient));
    }

    /**
     * Loads or a {@link AzureDevopsClient} corresponding to the {@link IntegrationKey}
     * from the {@link LoadingCache<IntegrationKey,AzureDevopsClient>}
     *
     * @param key {@link IntegrationKey} for the request
     * @return {@link AzureDevopsClient} corresponding to the {@code key}
     * @throws AzureDevopsClientException for any during creation of a client
     */
    public AzureDevopsClient get(final IntegrationKey key) throws AzureDevopsClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new AzureDevopsClientException(e);
        }
    }

    /**
     * creates a new {@link AzureDevopsClient} for the {@link IntegrationKey}. It uses the {@link Integration} and
     * {@link List < Token >} fetched from the {@link InventoryService} for creating the client.
     *
     * @param integrationKey {@linkKey} for which the new client is to be created
     * @return {@link AzureDevopsClient} created client
     */
    private AzureDevopsClient getNewClient(final IntegrationKey integrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(integrationKey);
            Integration integration = inventoryService.getIntegration(integrationKey);
            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, tokens);
            Supplier<Map<String, Object>> metadataSupplier = InventoryHelper.integrationMetadataSupplier(inventoryService,
                    integrationKey, 5, TimeUnit.MINUTES);
            return AzureDevopsClient.builder()
                    .objectMapper(objectMapper)
                    .resourceUrl(integration.getUrl())
                    .okHttpClient(authenticatedHttpClient)
                    .pageSize(pageSize)
                    .metadataSupplier(metadataSupplier)
                    .throttlingIntervalMs(throttlingIntervalMs)
                    .build();
        } catch (InventoryException | AzureDevopsClientException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens)
            throws InventoryException {
        return InventoryHelper.handleTokens(AZURE_DEVOPS, integrationKey, tokens,
                forType(OauthToken.TOKEN_TYPE, (Token token, OauthToken oauth) -> {
                    InventoryOauthTokenProvider tokenProvider = InventoryOauthTokenProvider.builder()
                            .inventoryService(inventoryService)
                            .integrationKey(integrationKey)
                            .tokenId(token.getId())
                            .token(token)
                            .build();
                    return this.okHttpClient.newBuilder()
                            // WARNING: since we are not using an authenticator, the order is critical - retrying must be the first interceptor
                            .addInterceptor(RetryingInterceptor.builder()
                                    .maxAttempts(10)
                                    .multiplierMs(10000)
                                    .maxWait(350)
                                    .maxWaitTimeUnit(TimeUnit.SECONDS)
                                    .retryPredicate(response -> response.code() == 429 || response.code() == 503 || response.code() == 401)
                                    .waitMsExtractor(response -> {
                                        String resource = response.header("X-RateLimit-Resource"); // which service is impacted
                                        String limit = response.header("X-RateLimit-Limit");
                                        String remaining = response.header("X-RateLimit-Remaining");
                                        String reset = response.header("X-RateLimit-Reset"); // epoch second when quota resets
                                        String retryAfterString = response.header("Retry-After"); // duration in seconds

                                        log.info("X-RateLimit: Resource={}, Limit={}, Remaining={}, Reset={}, Retry-After={}", resource, limit, remaining, reset, retryAfterString);

                                        Integer retryAfterSeconds = NumberUtils.tryParseInt(retryAfterString);
                                        return retryAfterSeconds != null ? retryAfterSeconds * 1000L : null;
                                    })
                                    .waitCutoffMs(TimeUnit.MINUTES.toMillis(10))
                                    .build())
                            .addInterceptor(new AzureTokenInterceptor(ClientConstants.AUTHORIZATION,
                                    ClientConstants.BEARER_, tokenProvider))
                            .build();
                }));
    }

    /**
     * builds a {@link AzureDevopsClient} with authenticated {@link OkHttpClient}for an {@link Integration}
     * with the authentication credentials from {@link Token}. Builds a new {@link AzureDevopsClient} on each call.
     *
     * @param tenantId    {@link String} id of the tenant
     * @param integration {@link Integration} for which to build the client for
     * @param token       {@link Token} containing the credentials
     * @return {@link AzureDevopsClient} built for the {@code integration} using {@code token}
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    public AzureDevopsClient buildFromToken(String tenantId, Integration integration,
                                            Token token, int pageSize) throws InventoryException, AzureDevopsClientException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        Supplier<Map<String, Object>> metadataSupplier = InventoryHelper.integrationMetadataSupplier(inventoryService, integrationKey,
                5, TimeUnit.MINUTES);
        OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, List.of(token));
        return AzureDevopsClient.builder()
                .objectMapper(objectMapper)
                .resourceUrl(integration.getUrl())
                .okHttpClient(authenticatedHttpClient)
                .pageSize(pageSize)
                .metadataSupplier(metadataSupplier)
                .throttlingIntervalMs(throttlingIntervalMs)
                .build();
    }

    public static Boolean parseIngestWorkItemComments(Map<String, Object> metadata) {
        Boolean shouldIngestWorkItemComments = Boolean.TRUE;
        Object comments = metadata.get("comments");
        if (comments instanceof Boolean) {
            shouldIngestWorkItemComments = ((Boolean) comments);
        }
        if (comments instanceof String) {
            shouldIngestWorkItemComments = Boolean.parseBoolean((String) comments);
        }
        return shouldIngestWorkItemComments;
    }
}
