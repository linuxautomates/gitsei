package io.levelops.commons.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.mappings.ProductIntegMapping;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
public class InventoryServiceImpl implements InventoryService {

    private static final String TENANTS_PATH_SEGMENT = "tenants";
    private static final String INTEGRATIONS_PATH_SEGMENT = "integrations";
    private static final String TOKENS_PATH_SEGMENT = "tokens";
    private static final String LQL_PATH_SEGMENT = "lql";
    private static final String VALIDATE_PATH_SEGMENT = "validate";
    private static final String LISTING_PATH_SEGMENT = "list";

    private final String inventoryServiceUrl;
    private final ObjectMapper objectMapper;
    private final ClientHelper<InventoryException> clientHelper;

    @Builder
    public InventoryServiceImpl(String inventoryServiceUrl, OkHttpClient client, ObjectMapper objectMapper) {
        this.inventoryServiceUrl = inventoryServiceUrl;
        this.objectMapper = objectMapper;
        this.clientHelper = ClientHelper.<InventoryException>builder()
                .client(client)
                .objectMapper(objectMapper)
                .exception(InventoryException.class)
                .build();
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return HttpUrl.parse(inventoryServiceUrl).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1");
    }

    @Override
    public void validateLQLs(List<String> lqls) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(LQL_PATH_SEGMENT)
                .addPathSegment(VALIDATE_PATH_SEGMENT)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(
                        Collections.singletonMap("lqls", lqls)))
                .build();
        clientHelper.executeRequest(request);
    }

    @Override
    public List<Tenant> listTenants() throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(LISTING_PATH_SEGMENT)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(Collections.emptyMap()))
                .build();
        String rawResponse = clientHelper.executeRequest(request);
        DbListResponse<Tenant> response = clientHelper.parseResponse(rawResponse,
                objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, Tenant.class));
        return response.getRecords();
    }

    @Override
    public Tenant getTenant(String tenantId) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        String rawResponse = clientHelper.executeRequest(request);
        return clientHelper.parseResponse(rawResponse, Tenant.class);
    }

    @Override
    public DbListResponse<Integration> listIntegrationsByApp(String tenantId, String application)
            throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment(LISTING_PATH_SEGMENT)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(Map.of("filter",
                        Map.of("application", application))))
                .build();
        String rawResponse = clientHelper.executeRequest(request);
        return clientHelper.parseResponse(rawResponse,
                objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, Integration.class));
    }

    @Override
    public DbListResponse<Integration> listIntegrationsFullFilter(String tenantId, DefaultListRequest filter)
            throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment(LISTING_PATH_SEGMENT)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(filter))
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, Integration.class));
    }

    @Override
    public DbListResponse<Integration> listIntegrationsByFilters(String tenantId, String application,
                                                                 List<String> integrationIds, List<String> tagIds)
            throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment(LISTING_PATH_SEGMENT)
                .build();
        Map<String, Object> filter = new HashMap<>();
        if (StringUtils.isNotEmpty(application))
            filter.put("application", application);
        if (CollectionUtils.isNotEmpty(tagIds))
            filter.put("tag_ids", tagIds);
        if (CollectionUtils.isNotEmpty(integrationIds))
            filter.put("integration_ids", integrationIds);
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(
                        Map.of("filter", filter)))
                .build();
        String rawResponse = clientHelper.executeRequest(request);
        return clientHelper.parseResponse(rawResponse,
                objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, Integration.class));
    }

    @Override
    public DbListResponse<Integration> listIntegrations(String tenantId)
            throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment(LISTING_PATH_SEGMENT)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(Collections.emptyMap()))
                .build();
        String rawResponse = clientHelper.executeRequest(request);
        return clientHelper.parseResponse(rawResponse,
                objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, Integration.class));
    }

    @Override
    public Integration getIntegration(String tenantId, String integrationId) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment(integrationId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        String rawResponse = clientHelper.executeRequest(request);
        return clientHelper.parseResponse(rawResponse, Integration.class);
    }

    @Override
    public Optional<String> updateIntegration(String tenantId, String integrationId, Integration integration) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment(integrationId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .put(clientHelper.createJsonRequestBody(integration))
                .build();
        String rawResponse = clientHelper.executeRequest(request);
        HashMap<String, String> response = clientHelper.parseResponse(rawResponse,
                objectMapper.getTypeFactory().constructParametricType(HashMap.class, String.class, String.class));
        return Optional.ofNullable(response.getOrDefault("integration_id", null));
    }

    @Override
    public Optional<String> postIntegration(String tenantId, Integration integration) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(integration))
                .build();
        String rawResponse = clientHelper.executeRequest(request);
        HashMap<String, String> response = clientHelper.parseResponse(rawResponse,
                objectMapper.getTypeFactory().constructParametricType(HashMap.class, String.class, String.class));
        return Optional.ofNullable(response.getOrDefault("integration_id", null));
    }

    @Override
    public void deleteIntegration(String tenantId, String integrationId) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment(integrationId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        clientHelper.executeRequest(request); //if failed this will throw exception
    }

    @Override
    public Map<String, Object> deleteIntegrations(String tenantId, List<String> integrationIds) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .delete(clientHelper.createJsonRequestBody(integrationIds))
                .build();
        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory()
                .constructMapType(HashMap.class, String.class, Object.class));
    }

    @Override
    public List<Token> listTokens(String tenantId, String integrationId) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment(integrationId)
                .addPathSegment(TOKENS_PATH_SEGMENT)
                .addPathSegment(LISTING_PATH_SEGMENT)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(Collections.emptyMap()))
                .build();
        String rawResponse = clientHelper.executeRequest(request);
        DbListResponse<Token> response = clientHelper.parseResponse(rawResponse,
                objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, Token.class));
        return response.getRecords();
    }

    @Override
    public Token getToken(String tenantId, String integrationId, String tokenId) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment(integrationId)
                .addPathSegment(TOKENS_PATH_SEGMENT)
                .addPathSegment(tokenId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        String rawResponse = clientHelper.executeRequest(request);
        return clientHelper.parseResponse(rawResponse, Token.class);
    }

    @Override
    public Optional<String> postToken(String tenantId, String integrationId, Token token) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment(integrationId)
                .addPathSegment(TOKENS_PATH_SEGMENT)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(token))
                .build();
        String rawResponse = clientHelper.executeRequest(request);
        HashMap<String, String> response = clientHelper.parseResponse(rawResponse,
                objectMapper.getTypeFactory().constructParametricType(HashMap.class, String.class, String.class));
        return Optional.ofNullable(response.getOrDefault("token_id", null));
    }

    @Override
    public Token refreshToken(String tenantId, String integrationId, String tokenId) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment(integrationId)
                .addPathSegment(TOKENS_PATH_SEGMENT)
                .addPathSegment(tokenId)
                .addPathSegment("refresh")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        String rawResponse = clientHelper.executeRequest(request);
        return clientHelper.parseResponse(rawResponse, Token.class);
    }

    @Override
    public void deleteTokensByIntegration(String tenantId, String integrationId) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment(integrationId)
                .addPathSegment(TOKENS_PATH_SEGMENT)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        clientHelper.executeRequest(request); //if failed this will throw exception
    }

    @Override
    public void deleteToken(String tenantId, String integrationId, String tokenId) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment(integrationId)
                .addPathSegment(TOKENS_PATH_SEGMENT)
                .addPathSegment(tokenId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        clientHelper.executeRequest(request); //if failed this will throw exception
    }


    @Override
    public DbListResponse<IntegrationConfig> listConfigs(String tenantId, @Nullable List<String> integrationIds, Integer pageNumber, Integer pageSize) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment("configs")
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(DefaultListRequest.builder()
                        .filter(Map.of("integration_ids", ListUtils.emptyIfNull(integrationIds)))
                        .pageSize(pageSize)
                        .page(pageNumber)
                        .build()))
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, IntegrationConfig.class));
    }

    @Override
    public DbListResponse<ProductIntegMapping> listProducts(String tenantId, String integrationId, Integer pageNumber, Integer pageSize) throws InventoryException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TENANTS_PATH_SEGMENT)
                .addPathSegment(tenantId)
                .addPathSegment(INTEGRATIONS_PATH_SEGMENT)
                .addPathSegment(integrationId)
                .addPathSegment("products")
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(DefaultListRequest.builder()
                        .pageSize(pageSize)
                        .page(pageNumber)
                        .build()))
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, ProductIntegMapping.class));
    }
}
