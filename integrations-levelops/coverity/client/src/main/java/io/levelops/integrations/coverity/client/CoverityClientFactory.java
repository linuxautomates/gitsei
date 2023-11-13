package io.levelops.integrations.coverity.client;

import com.coverity.ws.v9.ConfigurationService;
import com.coverity.ws.v9.DefectService;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.inventory.utils.InventoryHelper;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Use this class to obtain an instance of {@link CoverityClient} for a {@link IntegrationKey}.
 */
@Log4j2
public class CoverityClientFactory {

    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";
    private static final String COVERITY = "coverity";

    private final InventoryService inventoryService;
    private final LoadingCache<IntegrationKey, CoverityClient> clientCache;
    private final Boolean allowUnsafeSSL;

    /**
     * constructor for {@link CoverityClientFactory}
     *
     * @param inventoryService {@link InventoryService} for getting integration and authentication details
     */
    @Builder
    public CoverityClientFactory(InventoryService inventoryService, @Nullable Boolean allowUnsafeSSL) {
        this.inventoryService = inventoryService;
        this.clientCache = CacheBuilder.from(SPEC).build(CacheLoader.from(this::getNewClient));
        this.allowUnsafeSSL = allowUnsafeSSL != null ? allowUnsafeSSL : true;
    }

    /**
     * Loads or a {@link CoverityClient} corresponding to the {@link IntegrationKey}
     * from the {@link LoadingCache<IntegrationKey, CoverityClient >}
     *
     * @param key {@link IntegrationKey} for the request
     * @return {@link CoverityClient} corresponding to the {@code key}
     * @throws CoverityClientException for any during creation of a client
     */
    public CoverityClient get(final IntegrationKey key) throws CoverityClientException {
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new CoverityClientException(e);
        }
    }

    /**
     * creates a new {@link CoverityClient} for the {@link IntegrationKey}. It uses the {@link Integration} and
     * {@link List<Token>} fetched from the {@link InventoryService} for creating the client.
     *
     * @param integrationKey {@link IntegrationKey} for which the new client is to be created
     * @return {@link CoverityClient} created client
     */
    private CoverityClient getNewClient(final IntegrationKey integrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(integrationKey);
            Integration integration = inventoryService.getIntegration(integrationKey);
            ConfigurationService configurationService = buildConfigurationServiceClient(integrationKey, integration, tokens);
            DefectService defectService = buildDefectServiceClient(integrationKey, integration, tokens);
            return CoverityClient.builder()
                    .configurationService(configurationService)
                    .defectService(defectService)
                    .allowUnsafeSSL(allowUnsafeSSL)
                    .build();
        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    /**
     * Builds an {@link ConfigurationService} for {@code integrationKey} with authentication credentials from {@code tokens}
     *
     * @param integrationKey the {@link IntegrationKey} for which to build the {@link CoverityClient}
     * @param tokens         {@link List<Token>} list of different authentication tokens
     * @return {@link ConfigurationService} with authentication interceptor
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    private ConfigurationService buildConfigurationServiceClient(IntegrationKey integrationKey,
                                                                 Integration integration, List<Token> tokens) throws InventoryException {
        return InventoryHelper.handleTokens(COVERITY, integrationKey, tokens,
                InventoryHelper.TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token token, ApiKey apiKey) ->
                        createConfigurationService(integration.getUrl(), apiKey.getUserName(), apiKey.getApiKey())));
    }

    /**
     * Builds an {@link DefectService} for {@code integrationKey} with authentication credentials from {@code tokens}
     *
     * @param integrationKey the {@link IntegrationKey} for which to build the {@link CoverityClient}
     * @param tokens         {@link List<Token>} list of different authentication tokens
     * @return {@link DefectService} with authentication interceptor
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    private DefectService buildDefectServiceClient(IntegrationKey integrationKey,
                                                   Integration integration, List<Token> tokens) throws InventoryException {
        return InventoryHelper.handleTokens(COVERITY, integrationKey, tokens,
                InventoryHelper.TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token token, ApiKey apiKey) ->
                        createDefectService(integration.getUrl(), apiKey.getUserName(), apiKey.getApiKey())));
    }

    /**
     * builds a {@link CoverityClient} with authenticated {@link ConfigurationService},{@link DefectService}for an {@link Integration}
     * with the authentication credentials from {@link Token}. Builds a new {@link CoverityClient} on each call.
     *
     * @param tenantId    {@link String} id of the tenant
     * @param integration {@link Integration} for which to build the client for
     * @param token       {@link Token} containing the credentials
     * @return {@link CoverityClient} built for the {@code integration} using {@code token}
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    public CoverityClient buildFromToken(String tenantId, Integration integration, Token token)
            throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        ConfigurationService configurationService = buildConfigurationServiceClient(integrationKey, integration, List.of(token));
        DefectService defectService = buildDefectServiceClient(integrationKey, integration, List.of(token));
        return CoverityClient.builder()
                .defectService(defectService)
                .configurationService(configurationService)
                .build();
    }

    private ConfigurationService createConfigurationService(String serverAddr, String user, String password) {
        URL url;
        try {
            url = new URL(sanitizeUrl(serverAddr) + "/ws/v9/configurationservice?wsdl");
        } catch (MalformedURLException e) {
            throw new RuntimeException("operation was canceled", e);
        }
        com.coverity.ws.v9.ConfigurationServiceService dss = new com.coverity.ws.v9.ConfigurationServiceService(url,
                new QName("http://ws.coverity.com/v9", "ConfigurationServiceService"));
        ConfigurationService ds = dss.getConfigurationServicePort();
        // Attach an authentication handler to it
        BindingProvider bindingProvider = (BindingProvider) ds;
        bindingProvider.getBinding().setHandlerChain(
                List.of(new ClientAuthenticationHandlerWSS(user, password)));
        return ds;
    }

    private DefectService createDefectService(String serverAddr, String user, String password) {
        URL url;
        try {
            url = new URL(sanitizeUrl(serverAddr) + "/ws/v9/defectservice?wsdl");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        com.coverity.ws.v9.DefectServiceService dss = new com.coverity.ws.v9.DefectServiceService(url,
                new QName("http://ws.coverity.com/v9", "DefectServiceService"));
        DefectService ds = dss.getDefectServicePort();
        // Attach an authentication handler to it
        BindingProvider bindingProvider = (BindingProvider) ds;
        bindingProvider.getBinding().setHandlerChain(
                List.of(new ClientAuthenticationHandlerWSS(user, password)));
        return ds;
    }

    @NotNull
    private String sanitizeUrl(String serverAddr) {
        return (serverAddr.startsWith("http://") ||
                serverAddr.startsWith("https://")) ? serverAddr : "http://" + serverAddr;
    }
}