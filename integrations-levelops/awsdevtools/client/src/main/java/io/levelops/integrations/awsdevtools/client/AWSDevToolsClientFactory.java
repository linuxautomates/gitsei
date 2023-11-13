package io.levelops.integrations.awsdevtools.client;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.codebuild.AWSCodeBuildClientBuilder;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.client.oauth.BasicAuthInterceptor;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.inventory.utils.InventoryHelper;
import io.levelops.integrations.awsdevtools.models.AWSDevToolsQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Use this class to obtain an instance of {@link AWSDevToolsClient} for a {@link AWSDevToolsQuery.RegionIntegrationKey}.
 */
@Log4j2
public class AWSDevToolsClientFactory {

    protected static final int DEFAULT_PAGE_SIZE = 100;

    private static final String AWS_DEV_TOOLS = "Awsdevtools";
    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";

    private final InventoryService inventoryService;
    private final LoadingCache<AWSDevToolsQuery.RegionIntegrationKey, AWSDevToolsClient> clientCache;
    private final int pageSize;

    /**
     * constructor for {@link AWSDevToolsClientFactory}
     *
     * @param inventoryService {@link InventoryService} for getting integration and authentication details
     * @param pageSize         response page size
     */
    @Builder
    public AWSDevToolsClientFactory(InventoryService inventoryService, int pageSize) {
        this.inventoryService = inventoryService;
        this.pageSize = pageSize != 0 ? pageSize : DEFAULT_PAGE_SIZE;
        this.clientCache = CacheBuilder.from(SPEC).build(CacheLoader.from(this::getNewClient));
    }

    /**
     * Loads or a {@link AWSDevToolsClient} corresponding to the {@link AWSDevToolsQuery.RegionIntegrationKey}
     * from the {@link LoadingCache<AWSDevToolsQuery.RegionIntegrationKey, AWSDevToolsClient>}
     *
     * @param key {@link AWSDevToolsQuery.RegionIntegrationKey} for the request
     * @return {@link } corresponding to the {@code key}
     * @throws AWSDevToolsClientException for any during creation of a client
     */
    public AWSDevToolsClient get(final AWSDevToolsQuery.RegionIntegrationKey key) throws AWSDevToolsClientException {
        Validate.notNull(key.getRegion(), "region cannot be null");
        Validate.notNull(key.getIntegrationKey(), "integration key cannot be null");
        Validate.notNull(key, "key cannot be null");
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new AWSDevToolsClientException(e);
        }
    }

    /**
     * creates a new {@link AWSDevToolsClient} for the {@link AWSDevToolsQuery.RegionIntegrationKey}.
     * It uses the {@link Integration} and
     * {@link List<Token>} fetched from the {@link InventoryService} for creating the client.
     *
     * @param regionIntegrationKey {@link AWSDevToolsQuery.RegionIntegrationKey} for which the new client is to be created
     * @return {@link AWSDevToolsClient} created client
     */
    private AWSDevToolsClient getNewClient(final AWSDevToolsQuery.RegionIntegrationKey regionIntegrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(regionIntegrationKey.getIntegrationKey());
            return buildAuthenticatedClient(regionIntegrationKey, tokens);
        } catch (InventoryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * builds an authenticated {@link AWSDevToolsClient} client for an {@link Integration}
     * with the authentication credentials from {@link Token}. Builds a new {@link AWSDevToolsClient} on each call.
     *
     * @param tenantId    {@link String} id of the tenant
     * @param integration {@link Integration} for which to build the client for
     * @param token       {@link Token} containing the credentials
     * @param region      {@link String} containing the region for which the client is to be build.
     * @return {@link AWSDevToolsClient} built for the {@code integration} using {@code token}
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    public AWSDevToolsClient buildFromToken(String tenantId, Integration integration, Token token, String region)
            throws InventoryException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        Validate.notNull(region, "region cannot be null.");
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integration.getId())
                .build();
        AWSDevToolsQuery.RegionIntegrationKey regionIntegrationKey = AWSDevToolsQuery.RegionIntegrationKey.builder()
                .integrationKey(integrationKey)
                .region(region)
                .build();
        return buildAuthenticatedClient(regionIntegrationKey, List.of(token));
    }

    /**
     * Builds an {@link AWSDevToolsClient} for {@code regionIntegrationKey} with a {@link BasicAuthInterceptor}
     * with authentication credentials from {@code tokens}
     *
     * @param regionIntegrationKey the {@link AWSDevToolsQuery.RegionIntegrationKey} for which to build the
     *                             {@link AWSDevToolsClient}
     * @param tokens               {@link List<Token>} list of different authentication tokens
     * @throws InventoryException if {@code tokens} is empty or if no supported token is found for the integration
     */
    private AWSDevToolsClient buildAuthenticatedClient(AWSDevToolsQuery.RegionIntegrationKey regionIntegrationKey,
                                                       List<Token> tokens)
            throws InventoryException {
        return InventoryHelper.handleTokens(AWS_DEV_TOOLS, regionIntegrationKey.getIntegrationKey(), tokens,
                InventoryHelper.TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token token, ApiKey apiKey) -> {
                    AWSCredentials awsCredentials = new BasicAWSCredentials(apiKey.getUserName(), apiKey.getApiKey());
                    AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
                    AWSCodeBuildClientBuilder codeBuildClientBuilder = AWSCodeBuildClientBuilder.standard()
                            .withCredentials(credentialsProvider);
                    codeBuildClientBuilder.setRegion(String.valueOf(Region.getRegion(
                            Regions.valueOf(regionIntegrationKey.getRegion()))));
                    return new AWSDevToolsClient(codeBuildClientBuilder.build(), regionIntegrationKey.getRegion(), pageSize);
                }));
    }
}
