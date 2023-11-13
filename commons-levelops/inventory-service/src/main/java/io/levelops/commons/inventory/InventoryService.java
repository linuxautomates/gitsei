package io.levelops.commons.inventory;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.mappings.ProductIntegMapping;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface InventoryService {

    void validateLQLs(List<String> lqls) throws InventoryException;

    List<Tenant> listTenants() throws InventoryException;

    Tenant getTenant(String tenantId) throws InventoryException;

    DbListResponse<Integration> listIntegrationsByApp(String tenantId, String application) throws InventoryException;

    DbListResponse<Integration> listIntegrationsFullFilter(String tenantId, DefaultListRequest filter) throws InventoryException;

    DbListResponse<Integration> listIntegrationsByFilters(String tenantId, String application, List<String> integrationIds, List<String> tagIds) throws InventoryException;

    DbListResponse<Integration> listIntegrations(String tenantId) throws InventoryException;

    Integration getIntegration(String tenantId, String integrationId) throws InventoryException;

    Optional<String> updateIntegration(String tenantId, String integrationId, Integration integration) throws InventoryException;

    Optional<String> postIntegration(String tenantId, Integration integration) throws InventoryException;

    void deleteIntegration(String tenantId, String integrationId) throws InventoryException;

    Map<String, Object> deleteIntegrations(String tenantId, List<String> integrationIds) throws InventoryException;

    List<Token> listTokens(String tenantId, String integrationId) throws InventoryException;

    Token getToken(String tenantId, String integrationId, String tokenId) throws InventoryException;

    Optional<String> postToken(String tenantId, String integrationId, Token token) throws InventoryException;

    Token refreshToken(String tenantId, String integrationId, String tokenId) throws InventoryException;

    void deleteTokensByIntegration(String tenantId, String integrationId) throws InventoryException;

    void deleteToken(String tenantId, String integrationId, String tokenId) throws InventoryException;

    DbListResponse<IntegrationConfig> listConfigs(String tenantId, @Nullable List<String> integrationIds, Integer pageNumber, Integer pageSize) throws InventoryException;

    DbListResponse<ProductIntegMapping> listProducts(String tenantId, String integrationId, Integer pageNumber, Integer pageSize) throws InventoryException;

    // region convenience methods

    default Integration getIntegration(IntegrationKey integrationKey) throws InventoryException {
        return getIntegration(integrationKey.getTenantId(), integrationKey.getIntegrationId());
    }

    default List<Token> listTokens(IntegrationKey integrationKey) throws InventoryException {
        return listTokens(integrationKey.getTenantId(), integrationKey.getIntegrationId());
    }

    // endregion

}
