package io.levelops.integrations.rapid7.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.oauth.GenericTokenAuthenticator;
import io.levelops.commons.client.oauth.GenericTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.levelops.commons.inventory.utils.InventoryHelper.TokenHandler.forType;

@Log4j2
public class Rapid7ClientFactory {

    private static final String API_KEY_HEADER = "X-Api-Key";

    private InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private LoadingCache<IntegrationKey, Rapid7Client> clientCache;

    @Builder
    public Rapid7ClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.clientCache = CacheBuilder.from("maximumSize=250")
                .build(CacheLoader.from(this::getInternal));
    }

    public Rapid7Client get(IntegrationKey integrationKey) throws Rapid7ClientException {
        try {
            return clientCache.get(integrationKey);
        } catch (ExecutionException e) {
            throw new Rapid7ClientException(e);
        }
    }

    private Rapid7Client getInternal(IntegrationKey key) {
        Validate.notNull(key, "key cannot be null");
        try {
            List<Token> tokens = inventoryService.listTokens(key);

            OkHttpClient authenticatedHttpClient = InventoryHelper.handleTokens("Rapid7", key, tokens,
//                    forType(OauthToken.TOKEN_TYPE, (Token token, OauthToken oauth) -> {
//                        InventoryOauthTokenProvider tokenProvider = InventoryOauthTokenProvider.builder()
//                                .inventoryService(inventoryService)
//                                .integrationKey(key)
//                                .tokenId(token.getId())
//                                .build();
//                        return this.okHttpClient.newBuilder()
//                                .addInterceptor(new GenericTokenInterceptor(API_KEY_HEADER, "", tokenProvider))
//                                .authenticator(new GenericTokenAuthenticator(API_KEY_HEADER, "", tokenProvider))
//                                .addInterceptor(RetryingInterceptor.buildDefaultRetryer())
//                                .build();
//                    }),
                    forType(ApiKey.TOKEN_TYPE, (Token t, ApiKey apiKey) -> this.okHttpClient.newBuilder()
                            .addInterceptor(new GenericTokenInterceptor(API_KEY_HEADER, "", StaticOauthTokenProvider.builder()
                                    .token(apiKey.getApiKey())
                                    .build()))
                            .build()));

            return Rapid7Client.builder()
                    .objectMapper(objectMapper)
                    .okHttpClient(authenticatedHttpClient)
                    // TODO replace with commons
//                    .region(InventoryHelper.integrationUrlSupplier(inventoryService, key, 5, TimeUnit.MINUTES))
                    .region(Suppliers.memoizeWithExpiration(() -> {
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
