package io.levelops.notification.clients.msteams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.oauth.OauthTokenAuthenticator;
import io.levelops.commons.client.oauth.OauthTokenInterceptor;
import io.levelops.commons.client.oauth.OauthTokenProvider;
import io.levelops.commons.client.retrying.RetryingInterceptor;
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

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MSTeamsBotClientFactory {

    private InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private LoadingCache<IntegrationKey, MSTeamsBotClient> clientCache;

    @Builder
    public MSTeamsBotClientFactory(InventoryService inventoryService,
                                   ObjectMapper objectMapper,
                                   OkHttpClient okHttpClient) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.clientCache = CacheBuilder.from("maximumSize=250,expireAfterWrite=15m")
                .build(CacheLoader.from(this::getInternal));
    }

    public MSTeamsBotClient get(IntegrationKey integrationKey) throws MSTeamsClientException {
        try {
            return clientCache.get(integrationKey);
        } catch (ExecutionException e) {
            throw new MSTeamsClientException(e);
        }
    }

    private MSTeamsBotClient getInternal(IntegrationKey key) {
        Validate.notNull(key, "key cannot be null");
        try {
            List<Token> tokens = inventoryService.listTokens(key);

            OkHttpClient authenticatedHttpClient = InventoryHelper.handleTokens("MS Teams", key, tokens,
                    InventoryHelper.TokenHandler.forType(OauthToken.TOKEN_TYPE, (Token token, OauthToken oauth) -> {
                        OauthTokenProvider tokenProvider = InventoryOauthTokenProvider.builder()
                                .integrationKey(key)
                                .tokenId(token.getId())
                                .token(token)
                                .inventoryService(inventoryService)
                                .build();
                        return this.okHttpClient.newBuilder()
                                .addInterceptor(RetryingInterceptor.buildDefaultRetryer())
                                .addInterceptor(new OauthTokenInterceptor(tokenProvider))
                                .authenticator(new OauthTokenAuthenticator(tokenProvider))
                                .build();
                    }));
            return CachedMSTeamsBotClient.builder()
                    .delegate(MSTeamsBotClientImpl.builder()
                            .objectMapper(objectMapper)
                            .okHttpClient(authenticatedHttpClient)
                            .build())
                    .build();

        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }
}
