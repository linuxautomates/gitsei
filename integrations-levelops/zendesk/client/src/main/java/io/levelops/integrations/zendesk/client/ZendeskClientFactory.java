package io.levelops.integrations.zendesk.client;

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
import io.levelops.commons.inventory.utils.InventoryHelper.TokenHandler;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.Validate;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Use this class to obtain an instance of {@link ZendeskClient} for a {@link IntegrationKey}.
 */
@Log4j2
public class ZendeskClientFactory {

    public static final int DEFAULT_PAGE_SIZE = 500;
    public static final String JIRALINKS_ENABLED = "jiralinks_enabled";

    private static final String ZENDESK = "Zendesk";
    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";
    private static final String TOKEN = "/token:";
    private static final String ENRICH = "enrich";

    private static final int MAX_ATTEMPTS = 10;
    private static final int MULTIPLIER_MS = 10000;
    private static final int MAX_WAIT = 90;

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final LoadingCache<IntegrationKey, ZendeskClient> clientCache;
    private final int pageSize;

    /**
     * constructor for {@link ZendeskClientFactory}
     *
     * @param inventoryService {@link InventoryService} for getting integration and authentication details
     * @param objectMapper     {@link ObjectMapper} for the {@link ZendeskClient}
     * @param okHttpClient     {@link OkHttpClient} for the  {@link ZendeskClient}
     * @param pageSize         response page size
     */
    @Builder
    public ZendeskClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                OkHttpClient okHttpClient, int pageSize) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.pageSize = pageSize != 0 ? pageSize : DEFAULT_PAGE_SIZE;
        this.clientCache = CacheBuilder.from(SPEC).build(CacheLoader.from(this::getNewClient));
    }

    /**
     * Loads or a {@link ZendeskClient} corresponding to the {@link IntegrationKey}
     * from the {@link LoadingCache<IntegrationKey, ZendeskClient>}
     *
     * @param key {@link IntegrationKey} for the request
     * @return {@link ZendeskClient} corresponding to the {@code key}
     * @throws ZendeskClientException for any during creation of a client
     */
    public ZendeskClient get(final IntegrationKey key) throws ZendeskClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new ZendeskClientException(e);
        }
    }

    /**
     * creates a new {@link ZendeskClient} for the {@link IntegrationKey}. It uses the {@link Integration} and
     * {@link List<Token>} fetched from the {@link InventoryService} for creating the client.
     *
     * @param integrationKey {@link IntegrationKey} for which the new client is to be created
     * @return {@link ZendeskClient} created client
     */
    private ZendeskClient getNewClient(final IntegrationKey integrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(integrationKey);
            Integration integration = inventoryService.getIntegration(integrationKey);
            Boolean enrichmentEnabled = integration.getMetadata() == null || (Boolean) integration.getMetadata().getOrDefault(ENRICH, true);
            boolean jiraLinksEnabled = integration.getMetadata() != null && (Boolean) integration.getMetadata().getOrDefault(JIRALINKS_ENABLED, false);
            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, tokens);
            return ZendeskClient.builder()
                    .objectMapper(objectMapper)
                    .resourceUrl(integration.getUrl())
                    .okHttpClient(authenticatedHttpClient)
                    .pageSize(pageSize)
                    .enrichmentEnabled(enrichmentEnabled)
                    .jiralinksEnabled(jiraLinksEnabled)
                    .build();
        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    /**
     * Builds an {@link OkHttpClient} for {@code integrationKey} with a {@link BasicAuthInterceptor}
     * with authentication credentials from {@code tokens}
     *
     * @param integrationKey the {@link IntegrationKey} for which to build the {@link ZendeskClient}
     * @param tokens         {@link List<Token>} list of different authentication tokens
     * @return {@link OkHttpClient} with authentication interceptor
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens)
            throws InventoryException {
        return InventoryHelper.handleTokens(ZENDESK, integrationKey, tokens,
                TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token token, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                        .addInterceptor(new BasicAuthInterceptor(createToken(apiKey)))
                        .addInterceptor(RetryingInterceptor.build429Retryer(MAX_ATTEMPTS, MULTIPLIER_MS, MAX_WAIT,
                                TimeUnit.SECONDS))
                        .build()));
    }

    /**
     * builds a {@link ZendeskClient} with authenticated {@link OkHttpClient}for an {@link Integration}
     * with the authentication credentials from {@link Token}. Builds a new {@link ZendeskClient} on each call.
     *
     * @param tenantId    {@link String} id of the tenant
     * @param integration {@link Integration} for which to build the client for
     * @param token       {@link Token} containing the credentials
     * @return {@link ZendeskClient} built for the {@code integration} using {@code token}
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    public ZendeskClient buildFromToken(String tenantId, Integration integration, Token token)
            throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, List.of(token));
        return ZendeskClient.builder()
                .objectMapper(objectMapper)
                .resourceUrl(integration.getUrl())
                .okHttpClient(authenticatedHttpClient)
                .pageSize(pageSize)
                .enrichmentEnabled(false)
                .build();
    }

    /**
     * creates the base64 encoded token string to be used with basic api authentication
     *
     * @param apiKey {@link ApiKey} object for which the token string is to be created
     * @return the base64 encoded token string for the {@code apiKey}
     */
    private String createToken(ApiKey apiKey) {
        String toEncode = apiKey.getUserName() + TOKEN + apiKey.getApiKey();
        return Base64.getEncoder().encodeToString(toEncode.getBytes());
    }
}