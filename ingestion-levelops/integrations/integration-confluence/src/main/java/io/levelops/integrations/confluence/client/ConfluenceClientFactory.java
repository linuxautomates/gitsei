package io.levelops.integrations.confluence.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.oauth.BasicAuthInterceptor;
import io.levelops.commons.client.oauth.OauthTokenAuthenticator;
import io.levelops.commons.client.oauth.OauthTokenInterceptor;
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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.levelops.commons.inventory.utils.InventoryHelper.TokenHandler.forType;

@Log4j2
public class ConfluenceClientFactory {

    private InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private LoadingCache<IntegrationKey, ConfluenceClient> clientCache;

    @Builder
    public ConfluenceClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.clientCache = CacheBuilder.from("maximumSize=250")
                .build(CacheLoader.from(this::getInternal));
    }

    public ConfluenceClient get(IntegrationKey integrationKey) throws ConfluenceClientException {
        try {
            return clientCache.get(integrationKey);
        } catch (ExecutionException e) {
            throw new ConfluenceClientException(e);
        }
    }

    private ConfluenceClient getInternal(IntegrationKey key) {
        Validate.notNull(key, "key cannot be null");
        try {
            List<Token> tokens = inventoryService.listTokens(key);

            OkHttpClient authenticatedHttpClient = InventoryHelper.handleTokens("Jira", key, tokens,
                    forType(OauthToken.TOKEN_TYPE, (Token token, OauthToken oauth) -> {
                        InventoryOauthTokenProvider tokenProvider = InventoryOauthTokenProvider.builder()
                                .inventoryService(inventoryService)
                                .integrationKey(key)
                                .tokenId(token.getId())
                                .build();
                        return this.okHttpClient.newBuilder()
                                .addInterceptor(new OauthTokenInterceptor(tokenProvider))
                                .authenticator(new OauthTokenAuthenticator(tokenProvider))
                                .build();
                    }),
                    forType(ApiKey.TOKEN_TYPE, (Token t, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                            .addInterceptor(new BasicAuthInterceptor(apiKey.getAuthorizationHeader()))
                            .build()));

            return ConfluenceClient.builder()
                    .objectMapper(objectMapper)
                    .okHttpClient(authenticatedHttpClient)
                    // TODO replace with commons
//                    .confluenceUrl(InventoryHelper.integrationUrlSupplier(inventoryService, key, 5, TimeUnit.MINUTES))
                    .confluenceUrl(Suppliers.memoizeWithExpiration(() -> {
                        try {
                            return inventoryService.getIntegration(key).getUrl();
                        } catch (InventoryException e) {
                            log.warn("Failed to get integration url for key={}", key, e);
                            return null;
                        }
                    }, 5, TimeUnit.MINUTES))
                    .build();

        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }
}
