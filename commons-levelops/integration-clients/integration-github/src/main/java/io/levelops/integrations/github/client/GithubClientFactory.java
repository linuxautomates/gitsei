package io.levelops.integrations.github.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.oauth.OauthTokenAuthenticator;
import io.levelops.commons.client.oauth.OauthTokenInterceptor;
import io.levelops.commons.client.oauth.OauthTokenProvider;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
import io.levelops.commons.client.oauth.StaticRoundRobinOauthTokenProvider;
import io.levelops.commons.client.retrying.RetryingInterceptor;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.databases.models.database.tokens.MultipleApiKeys;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.inventory.oauth.InventoryOAuthJwtTokenProvider;
import io.levelops.commons.inventory.oauth.InventoryOauthTokenProvider;
import io.levelops.commons.inventory.utils.InventoryHelper;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.inventory.utils.InventoryHelper.TokenHandler.forType;

@Log4j2
public class GithubClientFactory {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final int throttlingIntervalMs; // disabled if <= 0
    private final LoadingCache<Pair<IntegrationKey, Boolean>, GithubClient> clientCache;

    public GithubClientFactory(
            InventoryService inventoryService,
            ObjectMapper objectMapper,
            OkHttpClient okHttpClient,
            int throttlingIntervalMs) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.throttlingIntervalMs = throttlingIntervalMs;
        clientCache = CacheBuilder.from("maximumSize=250,expireAfterWrite=15m")
                .build(CacheLoader.from(this::getInternal));
    }

    public GithubClient get(IntegrationKey integrationKey, boolean realtime) throws GithubClientException {
        try {
            return clientCache.get(Pair.of(integrationKey, realtime));
        } catch (ExecutionException e) {
            throw new GithubClientException(e);
        }
    }

    private GithubClient getInternal(Pair<IntegrationKey, Boolean> cacheKey) {
        try {
            IntegrationKey key = cacheKey.getLeft();
            boolean realtime = cacheKey.getRight();
            return buildFromInventory(key, realtime);
        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    public GithubClient buildFromToken(String tenantId, Integration integration, Token token, boolean realtime) throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey key = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();

        OkHttpClient authenticatedHttpClient = InventoryHelper.handleTokens("Github", key, List.of(token),
                forType(OauthToken.TOKEN_TYPE, (Token t, OauthToken oauth) -> {
                    StaticOauthTokenProvider tokenProvider = StaticOauthTokenProvider.builder()
                            .token(oauth.getToken())
                            .build();
                    return this.okHttpClient.newBuilder()
                            .addInterceptor(new OauthTokenInterceptor(tokenProvider))
                            .authenticator(new OauthTokenAuthenticator(tokenProvider))
                            .addInterceptor(realtime ? RetryingInterceptor.buildDefaultRealtimeRetryer() :
                                    RetryingInterceptor.builder()
                                            .maxAttempts(3)
                                            .multiplierMs(400)
                                            .maxWait(1)
                                            .maxWaitTimeUnit(TimeUnit.MINUTES)
                                            .retryPredicate(response -> !response.isSuccessful())
                                            .build())
                            .build();
                }),
                forType(ApiKey.TOKEN_TYPE, (Token t, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                        .addInterceptor(new OauthTokenInterceptor(StaticOauthTokenProvider.builder()
                                .token(apiKey.getApiKey())
                                .build()))
                        .build()),
                forType(MultipleApiKeys.TOKEN_TYPE, (Token t, MultipleApiKeys apiKey) -> this.okHttpClient.newBuilder()
                        .addInterceptor(new OauthTokenInterceptor(StaticRoundRobinOauthTokenProvider.builder()
                                .apiKeys(CollectionUtils.emptyIfNull(apiKey.getKeys()).stream().map(k -> io.levelops.commons.client.models.ApiKey.builder().apiKey(k.getApiKey()).userName(k.getUserName()).build()).collect(Collectors.toList()))
                                .build()))
                        .build())
        );

        return GithubClient.builder()
                .objectMapper(objectMapper)
                .okHttpClient(authenticatedHttpClient)
                .urlSupplier(integration::getUrl)
                .throttlingIntervalMs(throttlingIntervalMs)
                .ingestCommitFiles(parseIngestCommitFiles(integration))
                .build();
    }

    private String mapJwtToken(String refreshToken, IntegrationKey integrationKey) {
        try {
            Integration integration = inventoryService.getIntegration(integrationKey);
            String appId = MapUtils.getString(integration.getMetadata(), "app_id");
            String jwtToken = GithubAppTokenService.generateGithubAppJwtToken(refreshToken, appId, Instant.now());
            return jwtToken;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private OauthTokenProvider getTokenProvider(IntegrationKey key, boolean useJwtClient, String tokenId) {
        if (useJwtClient) {
            return InventoryOAuthJwtTokenProvider.builder()
                    .inventoryService(inventoryService)
                    .integrationKey(key)
                    .tokenId(tokenId)
                    .jwtTokenMapper(this::mapJwtToken)
                    .build();
        } else {
            return InventoryOauthTokenProvider.builder()
                    .inventoryService(inventoryService)
                    .integrationKey(key)
                    .tokenId(tokenId)
                    .build();
        }
    }

    private OkHttpClient buildOkHttpClient(IntegrationKey key, List<Token> tokens, boolean useJwtClient, boolean realtime) throws InventoryException {
        return InventoryHelper.handleTokens("Github", key, tokens,
                forType(OauthToken.TOKEN_TYPE, (Token token, OauthToken oauth) -> {
                    OauthTokenProvider tokenProvider = getTokenProvider(key, useJwtClient, token.getId());
                    return this.okHttpClient.newBuilder()
                            .addInterceptor(new OauthTokenInterceptor(tokenProvider))
                            .authenticator(new OauthTokenAuthenticator(tokenProvider))
                            .addInterceptor(realtime ? RetryingInterceptor.buildDefaultRealtimeRetryer() : RetryingInterceptor.buildDefaultRetryer())
                            .build();
                }),
                forType(ApiKey.TOKEN_TYPE, (Token t, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                        .addInterceptor(new OauthTokenInterceptor(StaticOauthTokenProvider.builder()
                                .token(apiKey.getApiKey())
                                .build()))
                        .build()),
                forType(MultipleApiKeys.TOKEN_TYPE, (Token t, MultipleApiKeys apiKey) -> this.okHttpClient.newBuilder()
                        .addInterceptor(new OauthTokenInterceptor(StaticRoundRobinOauthTokenProvider.builder()
                                .apiKeys(CollectionUtils.emptyIfNull(apiKey.getKeys()).stream().map(k -> io.levelops.commons.client.models.ApiKey.builder().apiKey(k.getApiKey()).userName(k.getUserName()).build()).collect(Collectors.toList()))
                                .build()))
                        .build())
        );
    }

    public GithubClient buildFromInventory(IntegrationKey key, boolean realtime) throws InventoryException {
        Validate.notNull(key, "key cannot be null");

        List<Token> tokens = inventoryService.listTokens(key);
        Integration integration = inventoryService.getIntegration(key);

        boolean isGithubApp = integration.isGithubAppsIntegration();

        OkHttpClient authenticatedHttpClient = buildOkHttpClient(key, tokens, false, realtime);
        OkHttpClient jwtHttpClient = isGithubApp ? buildOkHttpClient(key, tokens, true, realtime) : null;
        Boolean shdIngestCommitFiles = parseIngestCommitFiles(integration);

        return GithubClient.builder()
                .objectMapper(objectMapper)
                .okHttpClient(authenticatedHttpClient)
                .jwtOkHttpClient(jwtHttpClient)
                .urlSupplier(InventoryHelper.integrationUrlSupplier(inventoryService, key, 5, TimeUnit.MINUTES))
                .throttlingIntervalMs(throttlingIntervalMs)
                .ingestCommitFiles(shdIngestCommitFiles)
                .build();
    }

    private static boolean parseIngestCommitFiles(Integration integration) {
        Object commitFiles = MapUtils.emptyIfNull(integration.getMetadata()).get("fetch_commit_files");
        if (commitFiles instanceof Boolean) {
            return BooleanUtils.isNotFalse(((Boolean) commitFiles));
        }
        if (commitFiles instanceof String) {
            return Boolean.parseBoolean((String) commitFiles);
        }
        return true;
    }
}
