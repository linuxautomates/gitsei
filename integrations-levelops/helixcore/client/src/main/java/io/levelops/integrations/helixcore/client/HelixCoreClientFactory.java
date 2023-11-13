package io.levelops.integrations.helixcore.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.perforce.p4java.exception.*;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import lombok.Builder;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class HelixCoreClientFactory {

    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";
    private static final String ENRICH = "enrich";

    private final InventoryService inventoryService;
    private final LoadingCache<IntegrationKey, HelixCoreClient> clientCache;

    @Builder
    public HelixCoreClientFactory(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
        this.clientCache = CacheBuilder.from(SPEC)
                .build(CacheLoader.from(this::getNewClient));
    }

    /**
     * Loads a {@link HelixCoreClient} corresponding to the {@link IntegrationKey}
     * from the {@see LoadingCache<IntegrationKey, HelixCoreClient>}
     *
     * @param key {@link IntegrationKey} for the request
     * @return {@link HelixCoreClient} corresponding to the {@link IntegrationKey}
     * @throws HelixCoreClientException for any exception during creation of a client
     */
    public HelixCoreClient get(IntegrationKey key) throws HelixCoreClientException {
        try {
            return clientCache.get(key);
        } catch (ExecutionException e) {
            throw new HelixCoreClientException(e);
        }
    }

    /**
     * Creates a new {@link HelixCoreClient} for the {@link IntegrationKey}. It uses the {@link Integration} and
     *
     * @param integrationKey {@link IntegrationKey} for which the new client is to be created
     * @return {@link HelixCoreClient} created client
     * @link List<Token>} fetched from the {@link InventoryService} for creating the client.
     */
    private HelixCoreClient getNewClient(final IntegrationKey integrationKey) {
        try {
            List<Token> tokens = inventoryService.listTokens(integrationKey);
            Integration integration = inventoryService.getIntegration(integrationKey);
            boolean enrichmentEnabled = integration.getMetadata() == null ? true :
                    (Boolean) integration.getMetadata().getOrDefault(ENRICH, true);
            IOptionsServer server = ServerFactory.getOptionsServer(integration.getUrl(), null);
            ApiKey key = tokens.stream().map(Token::getTokenData)
                    .filter(tokenData -> ApiKey.TOKEN_TYPE.equalsIgnoreCase(tokenData.getType()))
                    .findFirst()
                    .map(tokenData -> (ApiKey) tokenData)
                    .orElseThrow();
            server.connect();
            server.setUserName(key.getUserName());
            server.login(key.getApiKey());
            return HelixCoreClient.builder()
                    .server(server)
                    .enrichmentEnabled(enrichmentEnabled)
                    .build();
        } catch (InventoryException | URISyntaxException | ConnectionException | NoSuchObjectException |
                ConfigException | ResourceException | RequestException | AccessException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }
}
