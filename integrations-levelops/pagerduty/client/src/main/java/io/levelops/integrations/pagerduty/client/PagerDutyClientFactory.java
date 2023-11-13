package io.levelops.integrations.pagerduty.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.oauth.GenericTokenInterceptor;
import io.levelops.commons.client.oauth.OauthTokenProvider;
import io.levelops.commons.client.retrying.RetryingInterceptor;
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

import java.util.List;
import java.util.concurrent.ExecutionException;

@Log4j2
public class PagerDutyClientFactory {

    private InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private LoadingCache<IntegrationKey, PagerDutyClient> clientCache;

    @Builder
    public PagerDutyClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
            OkHttpClient okHttpClient) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.clientCache = CacheBuilder.from("maximumSize=250,expireAfterWrite=15m").build(CacheLoader.from(this::getNewClient));
    }

    public PagerDutyClient get(final IntegrationKey key) throws PagerDutyClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new PagerDutyClientException(e);
        }
    }
    
    private PagerDutyClient getNewClient(final IntegrationKey key){
        try {
            List<Token> tokens = inventoryService.listTokens(key);

            OkHttpClient authenticatedHttpClient = InventoryHelper.handleTokens("PagerDuty", key, tokens,
                TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token token, ApiKey apiKey) -> {
                        OauthTokenProvider tokenProvider = new OauthTokenProvider(){
                            @Override
                            public String getToken() {
                                return apiKey.getApiKey();
                            }

                            @Override
                            public String refreshToken() {
                                return apiKey.getApiKey();
                            }
                        };
                        return this.okHttpClient.newBuilder()
                                .addInterceptor(new GenericTokenInterceptor(ClientConstants.AUTHORIZATION, "Token token=", tokenProvider))
                                .addInterceptor(RetryingInterceptor.buildDefaultRetryer())
                                .build();
                    }));

            return PagerDutyClient.builder()
                    .objectMapper(objectMapper)
                    .okHttpClient(authenticatedHttpClient)
                    .build();

        } catch (InventoryException e) {
            log.debug("Exception getting a new client", e);
            throw new RuntimeException(e); // for cache loader
        }
    }
}