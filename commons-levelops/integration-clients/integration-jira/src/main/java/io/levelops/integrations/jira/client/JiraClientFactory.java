package io.levelops.integrations.jira.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.oauth.BasicAuthInterceptor;
import io.levelops.commons.client.oauth.OauthTokenAuthenticator;
import io.levelops.commons.client.oauth.OauthTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
import io.levelops.commons.client.oauth.oauth1.Oauth1Credentials;
import io.levelops.commons.client.oauth.oauth1.Oauth1SigningInterceptor;
import io.levelops.commons.client.oauth.oauth1.Oauth1SigningMethod;
import io.levelops.commons.client.retrying.RetryingInterceptor;
import io.levelops.commons.client.throttling.ThrottlingInterceptor;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Integration.Authentication;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.AdfsOauthToken;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.databases.models.database.tokens.AtlassianConnectJwtToken;
import io.levelops.commons.databases.models.database.tokens.Oauth1Token;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.adfs.AdfsClient;
import io.levelops.commons.inventory.adfs.InMemoryAdfsOauthTokenProvider;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.inventory.oauth.InventoryOauthTokenProvider;
import io.levelops.commons.inventory.utils.InventoryHelper;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.inventory.utils.InventoryHelper.TokenHandler.forType;

@Log4j2
public class JiraClientFactory {

    private static final String SENSITIVE_FIELDS = "sensitive_fields";
    private static final String ADFS_URL_FIELD = "adfs_url";
    private static final String ADFS_CLIENT_ID_FIELD = "adfs_client_id";
    private static final String ADFS_RESOURCE_FIELD = "adfs_resource";

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    @Nullable
    private final Boolean allowUnsafeSSL;
    @Nullable
    private final Double rateLimitPerSecond; // Can be fractional
    private LoadingCache<IntegrationKey, JiraClient> clientCache;

    @Builder
    public JiraClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient, @Nullable Boolean allowUnsafeSSL, @Nullable Double rateLimitPerSecond) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.allowUnsafeSSL = allowUnsafeSSL;
        this.rateLimitPerSecond = rateLimitPerSecond;
        this.clientCache = CacheBuilder.from("maximumSize=250,expireAfterWrite=15m")
                .build(CacheLoader.from(this::getInternal));
    }

    public JiraClient get(IntegrationKey integrationKey) throws JiraClientException {
        try {
            return clientCache.get(integrationKey);
        } catch (ExecutionException e) {
            throw new JiraClientException(e);
        }
    }

    private JiraClient getInternal(IntegrationKey key) {
        try {
            return buildFromInventory(key);
        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    public JiraClient buildFromInventory(IntegrationKey key) throws InventoryException {
        Validate.notNull(key, "key cannot be null");

        Integration integration = inventoryService.getIntegration(key);
        List<Token> tokens = inventoryService.listTokens(key);

        OkHttpClient.Builder okHttpClientBuilder = this.okHttpClient.newBuilder()
                .addInterceptor(RetryingInterceptor.buildDefaultRetryer());

        OkHttpClient authenticatedHttpClient = InventoryHelper.handleTokens("Jira", key, tokens,
                forType(OauthToken.TOKEN_TYPE, (Token token, OauthToken oauth) -> {
                    InventoryOauthTokenProvider tokenProvider = InventoryOauthTokenProvider.builder()
                            .inventoryService(inventoryService)
                            .integrationKey(key)
                            .tokenId(token.getId())
                            .build();
                    return okHttpClientBuilder
                            .addInterceptor(new OauthTokenInterceptor(tokenProvider))
                            .authenticator(new OauthTokenAuthenticator(tokenProvider))
                            .build();
                }),
                forType(ApiKey.TOKEN_TYPE, (Token t, ApiKey apiKey) -> {
                    // We support 3 options for api key / basic auth:
                    // - cloud: username=email, apikey=PAT (basic auth with PAT: Basic $BASE64(email:PAT) )
                    // - on prem: username=login, apikey=password (basic auth: Basic $BASE64(login:pwd) )
                    // - both? username=<empty>, apikey=TOKEN (Bearer token API key: Bearer TOKEN)
                    if (StringUtils.isNotBlank(apiKey.getUserName())) {
                        return okHttpClientBuilder
                                .addInterceptor(new BasicAuthInterceptor(apiKey.getAuthorizationHeader()))
                                .build();
                    } else {
                        return okHttpClientBuilder
                                .addInterceptor(new OauthTokenInterceptor(StaticOauthTokenProvider.builder()
                                        .token(apiKey.getApiKey())
                                        .build()))
                                .build();
                    }
                }),
                forType(Oauth1Token.TOKEN_TYPE, (Token t, Oauth1Token oauth1) -> okHttpClientBuilder
                        .addInterceptor(Oauth1SigningInterceptor.builder()
                                .signingMethod(Oauth1SigningMethod.RSA_SHA1)
                                .credentials(Oauth1Credentials.builder()
                                        .consumerKey(oauth1.getConsumerKey())
                                        .consumerSecret(oauth1.getPrivateKey())
                                        .accessToken(oauth1.getAccessToken())
                                        .accessSecret(oauth1.getVerificationCode())
                                        .build())
                                .clockInSeconds(true)
                                .includeBodyInSignature(false) // OAuth1 standard only requires form-data payloads to be signed; JSON data must not be included.
                                .build())
                        .build()),
                forType(AtlassianConnectJwtToken.TOKEN_TYPE, (Token t, AtlassianConnectJwtToken jwtToken) -> okHttpClientBuilder
                        .addInterceptor(AtlassianConnectJwtInterceptor.builder()
                                .appKey(jwtToken.getAppKey())
                                .sharedSecret(jwtToken.getSharedSecret())
                                .build())
                        .build()),
                forType(AdfsOauthToken.TOKEN_TYPE, (Token t, AdfsOauthToken adfsToken) -> {
                    AdfsClient adfsClient = new AdfsClient(this.okHttpClient,
                            adfsToken.getAdfsUrl(),
                            adfsToken.getAdfsClientId(),
                            adfsToken.getAdfsResource(),
                            adfsToken.getUsername(),
                            adfsToken.getPassword());
                    // For the satellite use case, we will be storing and refreshing the token in memory
                    InMemoryAdfsOauthTokenProvider tokenProvider = new InMemoryAdfsOauthTokenProvider(adfsClient);
                    return okHttpClientBuilder
                            .addInterceptor(new OauthTokenInterceptor(tokenProvider))
                            .authenticator(new OauthTokenAuthenticator(tokenProvider))
                            .build();
                })
        );

        List<String> sensitiveFields = getSensitiveFields(integration);

        if (rateLimitPerSecond != null && rateLimitPerSecond > 0.01) {
            log.info("Jira rate limit enabled for tenant={}, integrationId={}: {} request(s) per second", key.getTenantId(), key.getIntegrationId(), rateLimitPerSecond);
            authenticatedHttpClient = authenticatedHttpClient.newBuilder()
                    .addInterceptor(new ThrottlingInterceptor(rateLimitPerSecond))
                    .build();
        }
        var jiraClientBuilder = JiraClient.builder()
                .authType(integration.getAuthentication())
                .objectMapper(objectMapper)
                .okHttpClient(authenticatedHttpClient)
                .jiraUrlSupplier(InventoryHelper.integrationUrlSupplier(inventoryService, key, 5, TimeUnit.MINUTES))
                .disableUrlSanitation(allowUnsafeSSL)
                .allowUnsafeSSL(allowUnsafeSSL)
                .sensitiveFields(sensitiveFields);

        if (Authentication.ATLASSIAN_CONNECT_JWT.equals(integration.getAuthentication())) {
            jiraClientBuilder
                    .jiraUrlSupplier(() -> ((AtlassianConnectJwtToken) tokens.get(0).getTokenData()).getBaseUrl());
        } else {
            jiraClientBuilder
                    .jiraUrlSupplier(InventoryHelper.integrationUrlSupplier(inventoryService, key, 5, TimeUnit.MINUTES));
        }

        return jiraClientBuilder.build();
    }

    public JiraClient buildFromToken(String tenantId, Integration integration, Token token) throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey key = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();

        OkHttpClient.Builder okHttpClientBuilder = this.okHttpClient.newBuilder()
                .addInterceptor(RetryingInterceptor.buildDefaultRetryer());

        List<Token> tokens = List.of(token);
        OkHttpClient authenticatedHttpClient = InventoryHelper.handleTokens("Jira", key, tokens,
                forType(OauthToken.TOKEN_TYPE, (Token t, OauthToken oauth) -> {
                    StaticOauthTokenProvider tokenProvider = StaticOauthTokenProvider.builder()
                            .token(oauth.getToken())
                            .build();
                    return okHttpClientBuilder
                            .addInterceptor(RetryingInterceptor.buildDefaultRetryer())
                            .addInterceptor(new OauthTokenInterceptor(tokenProvider))
                            .authenticator(new OauthTokenAuthenticator(tokenProvider))
                            .build();
                }),
                forType(ApiKey.TOKEN_TYPE, (Token t, ApiKey apiKey) -> {
                    // We support 3 options for api key / basic auth:
                    // - cloud: username=email, apikey=PAT (basic auth with PAT: Basic $BASE64(email:PAT) )
                    // - on prem: username=login, apikey=password (basic auth: Basic $BASE64(login:pwd) )
                    // - both? username=<empty>, apikey=TOKEN (Bearer token API key: Bearer TOKEN)
                    if (StringUtils.isNotBlank(apiKey.getUserName())) {
                        return okHttpClientBuilder
                                .addInterceptor(new BasicAuthInterceptor(apiKey.getAuthorizationHeader()))
                                .build();
                    } else {
                        return okHttpClientBuilder
                                .addInterceptor(new OauthTokenInterceptor(StaticOauthTokenProvider.builder()
                                        .token(apiKey.getApiKey())
                                        .build()))
                                .build();
                    }
                }),
                forType(Oauth1Token.TOKEN_TYPE, (Token t, Oauth1Token oauth1) -> okHttpClientBuilder
                        .addInterceptor(Oauth1SigningInterceptor.builder()
                                .signingMethod(Oauth1SigningMethod.RSA_SHA1)
                                .credentials(Oauth1Credentials.builder()
                                        .consumerKey(oauth1.getConsumerKey())
                                        .consumerSecret(oauth1.getPrivateKey())
                                        .accessToken(oauth1.getAccessToken())
                                        .accessSecret(oauth1.getVerificationCode())
                                        .build())
                                .clockInSeconds(true)
                                .includeBodyInSignature(false) // OAuth1 standard only requires form-data payloads to be signed; JSON data must not be included.
                                .build())
                        .build()),
                forType(AtlassianConnectJwtToken.TOKEN_TYPE, (Token t, AtlassianConnectJwtToken jwtToken) -> okHttpClientBuilder
                        .addInterceptor(AtlassianConnectJwtInterceptor.builder()
                                .appKey(jwtToken.getAppKey())
                                .sharedSecret(jwtToken.getSharedSecret())
                                .build())
                        .build())
        );

        List<String> sensitiveFields = getSensitiveFields(integration);

        if (rateLimitPerSecond != null && rateLimitPerSecond > 0.01) {
            log.info("Jira rate limit enabled for tenant={}, integrationId={}: {} request(s) per second", key.getTenantId(), key.getIntegrationId(), rateLimitPerSecond);
            authenticatedHttpClient = authenticatedHttpClient.newBuilder()
                    .addInterceptor(new ThrottlingInterceptor(rateLimitPerSecond))
                    .build();
        }

        var jiraClientBuilder = JiraClient.builder()
                .authType(integration.getAuthentication())
                .objectMapper(objectMapper)
                .okHttpClient(authenticatedHttpClient)
                .jiraUrlSupplier(integration::getUrl)
                .allowUnsafeSSL(allowUnsafeSSL)
                .disableUrlSanitation(allowUnsafeSSL)
                .sensitiveFields(sensitiveFields);

        if (integration.getAuthentication().equals(Authentication.ATLASSIAN_CONNECT_JWT)) {
            jiraClientBuilder
                    .jiraUrlSupplier(() -> ((AtlassianConnectJwtToken) token.getTokenData()).getBaseUrl());
        } else {
            jiraClientBuilder
                    .jiraUrlSupplier(integration::getUrl);
        }
        return jiraClientBuilder.build();
    }

    @SuppressWarnings("unchecked")
    protected static List<String> getSensitiveFields(Integration integration) {
        Map<String, Object> metadata = integration.getMetadata();
        List<String> sensitiveFields = null;
        if (metadata != null && metadata.containsKey(SENSITIVE_FIELDS)) {
            Object fields = metadata.get(SENSITIVE_FIELDS);
            if (fields instanceof List) {
                sensitiveFields = (List<String>) fields;
            } else if (fields instanceof String) {
                sensitiveFields = Arrays.asList(fields.toString().trim().split("\\s*,\\s*"));
            } else if (fields instanceof Map) {
                sensitiveFields = new ArrayList<>(((Map<?, String>) fields).values());
            }
        }
        if (CollectionUtils.isNotEmpty(sensitiveFields)) {
            sensitiveFields = sensitiveFields.stream()
                    .map(StringUtils::trimToNull)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            log.info("Will not ingest sensitive fields for Jira integration id={}: {}", integration.getId(), sensitiveFields);
        }
        return sensitiveFields;
    }
}
