package io.levelops.integrations.splunk.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.client.oauth.GenericTokenInterceptor;
import io.levelops.commons.client.oauth.OauthTokenAuthenticator;
import io.levelops.commons.client.oauth.OauthTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.inventory.oauth.InventoryOauthTokenProvider;
import io.levelops.commons.inventory.utils.InventoryHelper;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.levelops.commons.inventory.utils.InventoryHelper.TokenHandler.forType;

@Log4j2
public class SplunkClientFactory {
    private static final String API_KEY_HEADER = "Authorization";
    private static final String IS_SPLUNK_CLOUD = "is_splunk_cloud";
    private static final String IGNORE_SERVER_CERT = "ignore_server_cert";

    private InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private LoadingCache<IntegrationKey, SplunkClient> clientCache;

    private OkHttpClient sanitizeClient(OkHttpClient okHttpClient) {
        try {
            return ClientHelper.configureToIgnoreCertificate(okHttpClient.newBuilder()).build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            return okHttpClient;
        }
    }

    @Builder
    public SplunkClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.clientCache = CacheBuilder.from("maximumSize=250")
                .build(CacheLoader.from(this::getInternal));
    }

    public SplunkClient get(IntegrationKey integrationKey) throws SplunkClientException {
        try {
            return clientCache.get(integrationKey);
        } catch (ExecutionException e) {
            throw new SplunkClientException(e);
        }
    }

    private SplunkClient getInternal(IntegrationKey key) {
        Validate.notNull(key, "key cannot be null");
        try {
            List<Token> tokens = inventoryService.listTokens(key);
            Integration integration = inventoryService.getIntegration(key);
            Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());
            Boolean isSplunkCloud = (Boolean) metadata.getOrDefault(IS_SPLUNK_CLOUD, false);
            Boolean ignoreServerCert = (Boolean) metadata.getOrDefault(IGNORE_SERVER_CERT, false);
            SplunkClient splunkClient = InventoryHelper.handleTokens("Splunk", key, tokens,
                    forType(ApiKey.TOKEN_TYPE, (Token t, ApiKey apiKey) -> {
                        //ToDo: VA Fix later
//                        InventoryOauthTokenProvider tokenProvider = InventoryOauthTokenProvider.builder()
//                                .inventoryService(inventoryService)
//                                .integrationKey(key)
//                                .tokenId(t.getId())
//                                .build();
                        OkHttpClient authenticatedHttpClient = this.okHttpClient.newBuilder()
                                .addInterceptor(new OauthTokenInterceptor(StaticOauthTokenProvider.builder()
                                        .token(apiKey.getApiKey())
                                        .build()))
                                //ToDo: VA Fix later
//                                .addInterceptor(new OauthTokenInterceptor(tokenProvider))
//                                .authenticator(new OauthTokenAuthenticator(tokenProvider))
                                .build();
                        OkHttpClient okHttpClient = ignoreServerCert ? sanitizeClient(authenticatedHttpClient) :
                                authenticatedHttpClient;
                        return SplunkClient.builder()
                                .objectMapper(objectMapper)
                                .okHttpClient(okHttpClient)
                                .isSplunkCloud(isSplunkCloud)
                                .urlSupplier(InventoryHelper.integrationUrlSupplier(inventoryService, key, 5, TimeUnit.MINUTES))
                                .build();
                    })
            );
            return splunkClient;
        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }
}
