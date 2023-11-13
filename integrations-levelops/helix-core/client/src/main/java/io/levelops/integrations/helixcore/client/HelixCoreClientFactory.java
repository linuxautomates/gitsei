package io.levelops.integrations.helixcore.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import lombok.Builder;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelixCoreClientFactory {

    private static final String SPEC = "maximumSize=250,expireAfterWrite=15m";
    private static final String ENRICH = "enrich";
    private static final Pattern URL_PATTERN = Pattern.compile("(?:(.*)://)?(.*):(.*)", Pattern.CASE_INSENSITIVE);
    private static final String SSL_ENABLED = "ssl_enabled";
    private static final String SSL_FINGERPRINT = "ssl_fingerprint";
    private static final String SSL_AUTO_ACCEPT = "ssl_auto_accept";

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
            Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());
            Boolean sslEnabled = (Boolean) metadata.getOrDefault(SSL_ENABLED, false);
            Boolean sslAutoAccept = (Boolean) metadata.getOrDefault(SSL_AUTO_ACCEPT, false);
            String sslFingerprint = (String) metadata.getOrDefault(SSL_FINGERPRINT, null);
            boolean enrichmentEnabled = integration.getMetadata() == null ? true :
                    (Boolean) integration.getMetadata().getOrDefault(ENRICH, true);
            IOptionsServer server = ServerFactory.getOptionsServer(ensureValidProtocol(integration.getUrl(), sslEnabled), null);
            ApiKey key = tokens.stream().map(Token::getTokenData)
                    .filter(tokenData -> ApiKey.TOKEN_TYPE.equalsIgnoreCase(tokenData.getType()))
                    .findFirst()
                    .map(tokenData -> (ApiKey) tokenData)
                    .orElseThrow();
            if (sslEnabled) {
                server.addTrust(sslFingerprint, new TrustOptions().setAutoAccept(sslAutoAccept));
            }
            server.connect();
            server.setUserName(key.getUserName());
            server.login(key.getApiKey());
            return HelixCoreClient.builder()
                    .server(server)
                    .enrichmentEnabled(enrichmentEnabled)
                    .password(key.getApiKey())
                    .build();
        } catch (InventoryException | URISyntaxException | P4JavaException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    public HelixCoreClient buildFromToken(String tenantId, Integration integration, Token token) {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notNull(token, "token cannot be null.");
        try {
            Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());
            Boolean sslEnabled = (Boolean) metadata.getOrDefault(SSL_ENABLED, false);
            Boolean sslAutoAccept = (Boolean) metadata.getOrDefault(SSL_AUTO_ACCEPT, false);
            String sslFingerprint = (String) metadata.getOrDefault(SSL_FINGERPRINT, null);
            IOptionsServer server = ServerFactory.getOptionsServer(ensureValidProtocol(integration.getUrl(), sslEnabled), null);
            ApiKey key = (ApiKey) token.getTokenData();
            if (sslEnabled) {
                server.addTrust(sslFingerprint, new TrustOptions().setAutoAccept(sslAutoAccept));
            }
            server.connect();
            server.setUserName(key.getUserName());
            server.login(key.getApiKey());
            return HelixCoreClient.builder()
                    .server(server)
                    .enrichmentEnabled(true)
                    .build();
        } catch (URISyntaxException | P4JavaException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }

    private String ensureValidProtocol(String url, Boolean sslEnabled) {
        Matcher urlMatcher = URL_PATTERN.matcher(url);
        if (urlMatcher.find()) {
            String protocol = urlMatcher.group(1);
            if (StringUtils.isEmpty(protocol)) {
                if (!sslEnabled)
                    return "p4java://" + url;
                else
                    return "p4javassl://" + url;
            } else if (protocol.equals("p4java") || protocol.equals("p4javassl")) {
                return url;
            }
        }
        return url;
    }
}
