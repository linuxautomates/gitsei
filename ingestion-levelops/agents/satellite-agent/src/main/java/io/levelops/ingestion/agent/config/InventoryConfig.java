package io.levelops.ingestion.agent.config;

import io.levelops.commons.databases.models.database.tokens.MultipleApiKeys;
import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Configuration
public class InventoryConfig {

    private static final String OAUTH1_AUTH = "oauth1";
    private static final String OAUTH_AUTH = "oauth";
    private static final String BEARER_AUTH = "bearer";
    private static final String API_KEY_AUTH = "api_key";
    private static final String MULTIPLE_API_KEYS_AUTH = "multiple_api_keys";
    private static final String ADFS = "adfs";

    @Bean
    public InventoryService inventoryService(SatelliteConfigFileProperties configProperties) {
        InMemoryInventoryService.Inventory.InventoryBuilder inventoryBuilder = InMemoryInventoryService.Inventory.builder();
        configProperties.getIntegrations().forEach(integration -> {
            String authentication = StringUtils.defaultIfBlank(integration.getAuthentication(), API_KEY_AUTH).trim().toLowerCase();
            log.info("Integration: id={}, tenant={}, application={}, auth={}", integration.getId(), configProperties.getSatellite().getTenant(), integration.getApplication(), authentication);
            switch (authentication) {
                case BEARER_AUTH:
                case OAUTH_AUTH:
                    // NOTE: satellite doesn't support refreshing tokens,
                    // but we can still use this auth method for static ones
                    inventoryBuilder.oauthToken(
                            configProperties.getSatellite().getTenant(),
                            integration.getId(),
                            integration.getApplication(),
                            integration.getUrl(),
                            integration.getMetadata(),
                            integration.getToken(),
                            null,
                            null
                    );
                    break;
                case OAUTH1_AUTH:
                    inventoryBuilder.oauth1Token(
                            configProperties.getSatellite().getTenant(),
                            integration.getId(),
                            integration.getApplication(),
                            integration.getUrl(),
                            integration.getMetadata(),
                            integration.getPrivateKey(),
                            integration.getConsumerKey(),
                            integration.getVerificationCode(),
                            integration.getAccessToken()
                    );
                    break;
                case ADFS:
                    inventoryBuilder.adfsOauthToken(
                            configProperties.getSatellite().getTenant(),
                            integration.getId(),
                            integration.getApplication(),
                            integration.getUrl(),
                            integration.getMetadata(),
                            integration.getAdfsUrl(),
                            integration.getAdfsClientId(),
                            integration.getAdfsResource(),
                            integration.getAdfsUsername(),
                            integration.getAdfsPassword()
                    );
                    break;
                case MULTIPLE_API_KEYS_AUTH:
                    List<MultipleApiKeys.Key> keys = CollectionUtils.emptyIfNull(integration.keys).stream()
                            .map(k -> MultipleApiKeys.Key.builder()
                                    .apiKey(k.getApiKey())
                                    .userName(k.getUserName())
                                    .build())
                            .collect(Collectors.toList());
                    inventoryBuilder.multipleApiKeys(
                            configProperties.getSatellite().getTenant(),
                            integration.getId(),
                            integration.getApplication(),
                            integration.getUrl(),
                            integration.getMetadata(),
                            keys
                    );
                    break;
                case API_KEY_AUTH:
                default:
                    inventoryBuilder.apiKey(
                            configProperties.getSatellite().getTenant(),
                            integration.getId(),
                            integration.getApplication(),
                            integration.getUrl(),
                            integration.getMetadata(),
                            integration.getUserName(),
                            integration.getApiKey());
                    break;
            }
        });
        return new InMemoryInventoryService(inventoryBuilder.build());
    }
}
