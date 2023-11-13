package io.levelops.internal_api.services;

import io.harness.atlassian_connect.exceptions.AtlassianConnectServiceClientException;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.mappings.ProductIntegMapping;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.TokenDataService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.SecretsManagerServiceClient;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.exceptions.SecretsManagerServiceClientException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.NumberUtils;
import io.levelops.web.exceptions.NotFoundException;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class InventoryDBService implements InventoryService {
    private final IntegrationService integrationService;
    private final Boolean readTokensFromSecretsManagerService;
    private final Map<String, Set<String>> useSecretsManagerServiceForIntegrations;
    private final IntegrationSecretsService integrationSecretsService;
    private final TokenDataService tokenDataService;

    @Builder
    public InventoryDBService(final IntegrationService integrationService,
                              Boolean readTokensFromSecretsManagerService,
                              Map<String, Set<String>> useSecretsManagerServiceForIntegrations,
                              IntegrationSecretsService integrationSecretsService,
                              TokenDataService tokenDataService) {
        this.integrationService = integrationService;
        this.readTokensFromSecretsManagerService = readTokensFromSecretsManagerService;
        this.useSecretsManagerServiceForIntegrations = useSecretsManagerServiceForIntegrations;
        this.integrationSecretsService = integrationSecretsService;
        this.tokenDataService = tokenDataService;
    }

    @Override
    public void validateLQLs(List<String> lqls) throws InventoryException {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Tenant> listTenants() throws InventoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tenant getTenant(String tenantId) throws InventoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DbListResponse<Integration> listIntegrationsByApp(String tenantId, String application)
            throws InventoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DbListResponse<Integration> listIntegrationsFullFilter(String tenantId, DefaultListRequest listRequest)
            throws InventoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DbListResponse<Integration> listIntegrationsByFilters(String tenantId,
                                                                 String application,
                                                                 List<String> integrationIds,
                                                                 List<String> tagIds) throws InventoryException {
        try {
            return integrationService.listByFilter(tenantId, null, List.of(application), null,
                    toIntList(integrationIds), toIntList(tagIds), null, null);
        } catch (SQLException e) {
            throw new InventoryException(e);
        }
    }
    private static List<Integer> toIntList(List<String> strings) {
        return ListUtils.emptyIfNull(strings).stream().map(NumberUtils::toInteger).collect(Collectors.toList());
    }

    @Override
    public DbListResponse<Integration> listIntegrations(String tenantId) throws InventoryException {
        try {
            return integrationService.list(tenantId, 0, 100);
        } catch (SQLException e) {
            throw new InventoryException(e);
        }
    }

    @Override
    public Integration getIntegration(String tenantId, String integrationId) throws InventoryException {
        return integrationService.get(tenantId, integrationId).orElseThrow(() ->
                new InventoryException("Unable to find the integration '" + integrationId + "' for the tenant '" + tenantId + "'"));
    }

    @Override
    public Optional<String> updateIntegration(String tenantId, String integrationId, Integration integration)
            throws InventoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<String> postIntegration(String tenantId, Integration integration) throws InventoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteIntegration(String tenantId, String integrationId) throws InventoryException {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Object> deleteIntegrations(String tenantId, List<String> integrationIds) throws InventoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Token> listTokens(String tenantId, String integrationId) throws InventoryException {
        DbListResponse<Token> list;
        try {
            if (readTokensFromSecretsManagerService(tenantId, integrationId)) {
                String smConfigId = SecretsManagerServiceClient.DEFAULT_CONFIG_ID; // For Phase 2, this should be configurable.
                list = integrationSecretsService.get(tenantId, smConfigId, integrationId)
                        .map(token -> DbListResponse.of(List.of(token), 1))
                        .orElse(DbListResponse.of(List.of(), 0));
            } else {
                list = tokenDataService.listByIntegration(tenantId, integrationId, 0, 100);
            }
            return list.getRecords();
        } catch (SecretsManagerServiceClientException | SQLException | NotFoundException |
                 AtlassianConnectServiceClientException e) {
            log.error("listTokens: error fetching tokens for tenant: " + tenantId + " integration id: "
                    + integrationId, e);
            throw new InventoryException("error fetching tokens", e);
        }
    }

    private boolean readTokensFromSecretsManagerService(String company, String integrationId) {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        if (readTokensFromSecretsManagerService) {
            return true;
        }
        boolean read = useSecretsManagerServiceForIntegrations.getOrDefault(company.toLowerCase(), Set.of()).contains(integrationId.toLowerCase());
        if (read) {
            log.debug("Reads from secrets manager service have been turned ON for tenant={}, integrationId={}", company, integrationId);
        }
        return read;
    }

    @Override
    public Token getToken(String tenantId, String integrationId, String tokenId) throws InventoryException {
        try {
            Optional<Token> tokenOpt;
            if (readTokensFromSecretsManagerService(tenantId, integrationId)) {
                String smConfigId = SecretsManagerServiceClient.DEFAULT_CONFIG_ID; // For Phase 2, this should be discovered.
                tokenOpt = integrationSecretsService.get(tenantId, smConfigId, integrationId);
            } else {
                tokenOpt = tokenDataService.get(tenantId, tokenId);
            }
            return tokenOpt.orElseThrow(() -> new InventoryException("Could not find token with id = " + tokenId
                    + " for company= " + tenantId + " and integration id= " + integrationId));
        } catch (SecretsManagerServiceClientException | SQLException | NotFoundException |
                 AtlassianConnectServiceClientException e) {
            log.error("getToken: Error getting token for tenant: " + tenantId + " integration: " + integrationId
                    + " token id: " + tokenId, e);
            throw new InventoryException("error fetching token: " + tokenId, e);
        }
    }

    @Override
    public Optional<String> postToken(String tenantId, String integrationId, Token token) throws InventoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Token refreshToken(String tenantId, String integrationId, String tokenId) throws InventoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteTokensByIntegration(String tenantId, String integrationId) throws InventoryException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteToken(String tenantId, String integrationId, String tokenId) throws InventoryException {
        // TODO Auto-generated method stub

    }

    @Override
    public DbListResponse<IntegrationConfig> listConfigs(String tenantId, @Nullable List<String> integrationIds, Integer pageNumber, Integer pageSize) throws InventoryException {
        return null;
    }

    @Override
    public DbListResponse<ProductIntegMapping> listProducts(String tenantId, String integrationId, Integer pageNumber, Integer pageSize) throws InventoryException {
        return null;
    }

    @Override
    public Integration getIntegration(IntegrationKey integrationKey) throws InventoryException {
        return getIntegration(integrationKey.getTenantId(), integrationKey.getIntegrationId());
    }

    @Override
    public List<Token> listTokens(IntegrationKey integrationKey) throws InventoryException {
        return listTokens(integrationKey.getTenantId(), integrationKey.getIntegrationId());
    }

}