package io.levelops.commons.inventory;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.mappings.ProductIntegMapping;
import io.levelops.commons.databases.models.database.tokens.AdfsOauthToken;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.databases.models.database.tokens.DBAuth;
import io.levelops.commons.databases.models.database.tokens.MultipleApiKeys;
import io.levelops.commons.databases.models.database.tokens.Oauth1Token;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.databases.models.database.tokens.TokenData;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Data;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryInventoryService implements InventoryService {

    private final Inventory inventory;

    public InMemoryInventoryService(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public void validateLQLs(List<String> lqls) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Tenant> listTenants() throws InventoryException {
        return inventory.getTenants();
    }

    @Override
    public Tenant getTenant(String tenantId) throws InventoryException {
        return inventory.getTenants().stream()
                .filter(t -> tenantId.equals(t.getTenantName()))
                .findAny()
                .orElseThrow(() -> new InventoryException("Not found"));
    }

    @Override
    public DbListResponse<Integration> listIntegrationsByApp(String tenantId, String application)
            throws InventoryException {
        List<Integration> integrations = listIntegrations(tenantId).getRecords().stream()
                .filter(i -> application.equals(i.getApplication()))
                .collect(Collectors.toList());
        return DbListResponse.of(integrations, integrations.size());
    }

    @Override
    public DbListResponse<Integration> listIntegrationsFullFilter(String tenantId, DefaultListRequest listRequest)
            throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DbListResponse<Integration> listIntegrationsByFilters(String tenantId, String application,
                                                                 List<String> integrationIds, List<String> tagIds)
            throws InventoryException {
        List<Integration> integrations = listIntegrations(tenantId).getRecords().stream()
                .filter(i -> application.equals(i.getApplication()))
                .filter(i -> CollectionUtils.isEmpty(integrationIds) ||
                        (StringUtils.isNotEmpty(i.getId()) && integrationIds.contains(i.getId())))
                //tagids is empty or the item's tagids is not empty and any of them match any of the input tagids
                .filter(i -> CollectionUtils.isEmpty(tagIds) ||
                        (CollectionUtils.isNotEmpty(i.getTags()) && i.getTags().stream().anyMatch(tagIds::contains)))
                .collect(Collectors.toList());
        return DbListResponse.of(integrations, integrations.size());
    }

    @Override
    public DbListResponse<Integration> listIntegrations(String tenantId) throws InventoryException {
        List<Integration> integrations = inventory.getIntegrations().get(tenantId);
        if (integrations == null) {
            throw new InventoryException("Not found");
        }
        return DbListResponse.of(integrations, integrations.size());
    }

    @Override
    public Integration getIntegration(String tenantId, String integrationId) throws InventoryException {
        return listIntegrations(tenantId).getRecords().stream()
                .filter(i -> integrationId.equals(i.getId()))
                .findAny()
                .orElseThrow(() -> new InventoryException("Not found"));
    }

    @Override
    public Optional<String> updateIntegration(String tenantId, String integrationId, Integration integration) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> postIntegration(String tenantId, Integration integration) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteIntegration(String tenantId, String integrationId) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> deleteIntegrations(String tenantId, List<String> integrationIds) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Token> listTokens(String tenantId, String integrationId) throws InventoryException {
        List<Token> tokens = inventory.getTokens().get(IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integrationId)
                .build());
        if (tokens == null) {
            throw new InventoryException("Not found");
        }
        return tokens;
    }

    @Override
    public Token getToken(String tenantId, String integrationId, String tokenId) throws InventoryException {
        return listTokens(tenantId, integrationId).stream()
                .filter(t -> tokenId.equals(t.getId()))
                .findAny()
                .orElseThrow(() -> new InventoryException("Not found"));
    }

    @Override
    public Optional<String> postToken(String tenantId, String integrationId, Token token) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Token refreshToken(String tenantId, String integrationId, String tokenId) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteTokensByIntegration(String tenantId, String integrationId) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteToken(String tenantId, String integrationId, String tokenId) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DbListResponse<IntegrationConfig> listConfigs(String tenantId, @Nullable List<String> integrationIds, Integer pageNumber, Integer pageSize) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DbListResponse<ProductIntegMapping> listProducts(String tenantId, String integrationId, Integer pageNumber, Integer pageSize) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Data
    public static class Inventory {
        List<Tenant> tenants;
        Map<String, List<Integration>> integrations; // tenantId -> Integration
        Map<IntegrationKey, List<Token>> tokens;

        @Builder(toBuilder = true)
        public Inventory(List<Tenant> tenants, Map<String, List<Integration>> integrations, Map<IntegrationKey, List<Token>> tokens) {
            this.tenants = ListUtils.emptyIfNull(tenants);
            this.integrations = MapUtils.emptyIfNull(integrations);
            this.tokens = MapUtils.emptyIfNull(tokens);
        }

        public static class InventoryBuilder {

            public String nextTokenId(String tenantId, String integrationId) {
                IntegrationKey key = IntegrationKey.builder()
                        .tenantId(tenantId)
                        .integrationId(integrationId)
                        .build();
                if (this.tokens == null) {
                    return "0";
                }
                return String.valueOf(CollectionUtils.size(this.tokens.get(key)));
            }

            public InventoryBuilder apiKey(String tenantId, String integrationId, String application, String url, @Nullable Map<String, Object> metadata, String userName, String apiKey) {
                return tokenData(tenantId, integrationId, application, url, metadata, ApiKey.builder()
                        .userName(userName)
                        .apiKey(apiKey)
                        .build());
            }

            public InventoryBuilder multipleApiKeys(String tenantId, String integrationId, String application, String url, @Nullable Map<String, Object> metadata, List<MultipleApiKeys.Key> keys) {
                return tokenData(tenantId, integrationId, application, url, metadata, MultipleApiKeys.builder()
                        .keys(keys)
                        .build());
            }

            public InventoryBuilder oauthToken(String tenantId, String integrationId, String application, String url, @Nullable Map<String, Object> metadata, String token, String refreshToken, String botToken) {
                return tokenData(tenantId, integrationId, application, url, metadata, OauthToken.builder()
                        .token(token)
                        .refreshToken(refreshToken)
                        .botToken(botToken)
                        .build());
            }

            public InventoryBuilder dbAuth(String tenantId, String integrationId, String application, String url, @Nullable Map<String, Object> metadata, String dbServer, String userName, String password, String dbName) {
                return tokenData(tenantId, integrationId, application, url, metadata, DBAuth.builder()
                        .server(dbServer)
                        .userName(userName)
                        .password(password)
                        .databaseName(dbName)
                        .build()
                );
            }

            public InventoryBuilder oauth1Token(String tenantId, String integrationId, String application, String url, @Nullable Map<String, Object> metadata,
                                                 String privateKey, String consumerKey, String verificationCode, String accessToken) {
                return tokenData(tenantId, integrationId, application, url, metadata, Oauth1Token.builder()
                        .consumerKey(consumerKey)
                        .privateKey(privateKey)
                        .verificationCode(verificationCode)
                        .accessToken(accessToken)
                        .build());
            }

            public InventoryBuilder adfsOauthToken(String tenantId, String integrationId, String application, String url, @Nullable Map<String, Object> metadata,
                                                   String adfsUrl,
                                                   String adfsClientId,
                                                   String adfsResource,
                                                   String adfsUsername,
                                                   String adfsPassword) {
                return tokenData(tenantId, integrationId, application, url, metadata, AdfsOauthToken.builder()
                        .adfsUrl(adfsUrl)
                        .adfsClientId(adfsClientId)
                        .adfsResource(adfsResource)
                        .username(adfsUsername)
                        .password(adfsPassword)
                        .build());
            }

            public InventoryBuilder tokenData(String tenantId, String integrationId, String application, String url, @Nullable Map<String, Object> metadata, TokenData tokenData) {
                tenant(tenantId);
                integration(tenantId, Integration.builder()
                        .id(integrationId)
                        .url(url)
                        .application(application)
                        .metadata(MapUtils.emptyIfNull(metadata))
                        .build());
                token(tenantId, integrationId, Token.builder()
                        .id(nextTokenId(tenantId, integrationId))
                        .integrationId(integrationId)
                        .tokenData(tokenData)
                        .build());
                return this;
            }

            public InventoryBuilder tenant(String tenantId) {
                if (this.tenants == null) {
                    this.tenants = new ArrayList<>();
                }
                Tenant tenant = Tenant.builder().id(tenantId).build();
                if (!this.tenants.contains(tenant)) {
                    this.tenants.add(tenant);
                }
                return this;
            }

            public InventoryBuilder integration(String tenantId, Integration integration) {
                if (this.integrations == null) {
                    this.integrations = new HashMap<>();
                }
                List<Integration> tenantIntegrations = this.integrations.getOrDefault(tenantId, new ArrayList<>());
                if (!tenantIntegrations.contains(integration)) {
                    tenantIntegrations.add(integration);
                }
                this.integrations.put(tenantId, tenantIntegrations);
                return this;
            }

            public InventoryBuilder token(String tenantId, String integrationId, Token token) {
                if (this.tokens == null) {
                    this.tokens = new HashMap<>();
                }
                IntegrationKey key = IntegrationKey.builder()
                        .tenantId(tenantId)
                        .integrationId(integrationId)
                        .build();
                List<Token> integrationTokens = this.tokens.getOrDefault(key, new ArrayList<>());
                if (!integrationTokens.contains(token)) {
                    integrationTokens.add(token);
                }
                this.tokens.put(key, integrationTokens);
                return this;
            }

        }
    }
}
