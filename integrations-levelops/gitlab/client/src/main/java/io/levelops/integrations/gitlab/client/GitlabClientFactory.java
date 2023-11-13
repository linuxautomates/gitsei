package io.levelops.integrations.gitlab.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.oauth.HeaderAddingInterceptor;
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
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.levelops.commons.inventory.utils.InventoryHelper.TokenHandler.forType;

/**
 * Use this class to obtain an instance of {@link GitlabClient} for a {@link IntegrationKey}.
 */
@Log4j2
public class GitlabClientFactory {

    private static final String FETCH_FILE_DIFFS = "fetch_file_diffs";
    private static final int DEFAULT_PAGE_SIZE = 250;
    private static final String GITLAB = "Gitlab";
    private static final String PRIVATE_TOKEN = "PRIVATE-TOKEN";
    private static final String SWALLOW_ERRORS_METADATA_FIELD = "swallow_errors";

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final InventoryService inventoryService;
    private final LoadingCache<Pair<IntegrationKey, Boolean>, GitlabClient> clientCache;
    private final int pageSize;
    private final Boolean allowUnsafeSSL;

    @Builder
    public GitlabClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                               OkHttpClient okHttpClient, int pageSize, @Nullable Boolean allowUnsafeSSL) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.pageSize = pageSize != 0 ? pageSize : DEFAULT_PAGE_SIZE;
        this.allowUnsafeSSL = allowUnsafeSSL != null ? allowUnsafeSSL : true;
        this.clientCache = CacheBuilder.from("maximumSize=250,expireAfterWrite=15m")
                .build(CacheLoader.from(this::getInternal));
    }

    public GitlabClient get(final IntegrationKey key, boolean realtime) throws GitlabClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(Pair.of(key, realtime));
        } catch (ExecutionException e) {
            throw new GitlabClientException(e);
        }
    }

    private GitlabClient getInternal(Pair<IntegrationKey, Boolean> cacheKey) {
        Validate.notNull(cacheKey, "cache key cannot be null");
        IntegrationKey key = cacheKey.getLeft();
        Validate.notNull(key, "key cannot be null");
        Boolean realtime = cacheKey.getRight();
        try {
            List<Token> tokens = inventoryService.listTokens(key);
            Integration integration = inventoryService.getIntegration(key);
            OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(key, tokens, realtime);
            Boolean fetchFileDiff = getFetchFileDiff(integration, true);
            Boolean swallowErrors = getSwallowErrors(integration, false);
            if (swallowErrors) {
                log.info("Initializing gitlab client with swallow errors enabled");
            }
            return GitlabClient.builder()
                    .objectMapper(objectMapper)
                    .okHttpClient(authenticatedHttpClient)
                    .resourceUrl(integration.getUrl())
                    .allowUnsafeSSL(allowUnsafeSSL)
                    .fetchFileDiff(fetchFileDiff)
                    .pageSize(pageSize)
                    .swallowExceptions(swallowErrors)
                    .build();
        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    private Boolean getSwallowErrors(Integration integration, Boolean defaultValue) {
        Map<String, Object> metadata = integration.getMetadata();
        if (metadata != null && metadata.containsKey(SWALLOW_ERRORS_METADATA_FIELD)) {
            return (Boolean) metadata.getOrDefault(SWALLOW_ERRORS_METADATA_FIELD, false);
        }
        return defaultValue;
    }

    private Boolean getFetchFileDiff(Integration integration, Boolean defaultValue) {
        Map<String, Object> metadata = integration.getMetadata();
        if (metadata != null && metadata.containsKey(FETCH_FILE_DIFFS)) {
            return (Boolean) metadata.get(FETCH_FILE_DIFFS);
        }
        return defaultValue;
    }

    private OkHttpClient buildAuthenticatedClient(IntegrationKey integrationKey, List<Token> tokens, boolean realtime)
            throws InventoryException {
        RetryingInterceptor retryingInterceptor = RetryingInterceptor.buildDefaultRetryer();
        if (realtime) {
            retryingInterceptor = RetryingInterceptor.buildDefaultRealtimeRetryer();
        }
        RetryingInterceptor finalRetryingInterceptor = retryingInterceptor;
        return InventoryHelper.handleTokens(GITLAB, integrationKey, tokens,
                forType(OauthToken.TOKEN_TYPE, (Token token, OauthToken oauth) -> {
                    InventoryOauthTokenProvider tokenProvider = InventoryOauthTokenProvider.builder()
                            .inventoryService(inventoryService)
                            .integrationKey(integrationKey)
                            .tokenId(token.getId())
                            .token(token)
                            .build();
                    return this.okHttpClient.newBuilder()
                            .addInterceptor(new OauthTokenInterceptor(tokenProvider))
                            .authenticator(new OauthTokenAuthenticator(tokenProvider))
                            .addInterceptor(finalRetryingInterceptor)
                            .build();
                }),
                forType(ApiKey.TOKEN_TYPE,
                        (Token token, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                                .addInterceptor(new HeaderAddingInterceptor(PRIVATE_TOKEN, apiKey.getApiKey()))
                                .addInterceptor(finalRetryingInterceptor)
                                .build()));
    }

    public GitlabClient buildFromToken(String tenantId, Integration integration, Token token, int pageSize, boolean realtime)
            throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        OkHttpClient authenticatedHttpClient = buildAuthenticatedClient(integrationKey, List.of(token), realtime);
        Boolean fetchFileDiff = getFetchFileDiff(integration, true);
        return GitlabClient.builder()
                .objectMapper(objectMapper)
                .okHttpClient(authenticatedHttpClient)
                .resourceUrl(integration.getUrl())
                .allowUnsafeSSL(allowUnsafeSSL)
                .fetchFileDiff(fetchFileDiff)
                .pageSize(pageSize)
                .build();
    }
}
