package io.levelops.integrations.github.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.models.GithubOrganization;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
public class GithubOrganizationService {

    private final boolean enableCaching;
    private final GithubClientFactory clientFactory;
    private final LoadingCache<IntegrationKey, List<String>> orgCache;
    private final InventoryService inventoryService;

    public GithubOrganizationService(boolean enableCaching,
                                     long cacheMaxSize,
                                     long cacheExpiryInHours,
                                     GithubClientFactory clientFactory,
                                     InventoryService inventoryService) {
        this.enableCaching = enableCaching;
        this.clientFactory = clientFactory;
        this.orgCache = initOrgCache(cacheMaxSize, cacheExpiryInHours);
        this.inventoryService = inventoryService;
    }

    private LoadingCache<IntegrationKey, List<String>> initOrgCache(long cacheMaxSize,
                                                                    long cacheExpiryInHours) {
        return CacheBuilder.newBuilder()
                .maximumSize(cacheMaxSize) // # of integrations
                .expireAfterWrite(cacheExpiryInHours, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                    @Override
                    public List<String> load(@NotNull IntegrationKey key) throws Exception {
                        return getOrganizationsLive(key);
                    }
                });
    }

    public List<String> getOrganizations(IntegrationKey integrationKey) throws FetchException {
        log.debug("Getting orgs for {}", integrationKey);
        if (enableCaching) {
            return getOrganizationsCached(integrationKey);
        }
        return getOrganizationsLive(integrationKey);
    }

    private List<String> getOrganizationsCached(IntegrationKey integrationKey) throws FetchException {
        Validate.notNull(integrationKey, "integrationKey cannot be null.");
        Validate.notBlank(integrationKey.getTenantId(), "integrationKey.getTenantId() cannot be null or empty.");
        Validate.notBlank(integrationKey.getIntegrationId(), "integrationKey.getIntegrationId() cannot be null or empty.");
        try {
            return orgCache.get(integrationKey);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FetchException) {
                throw (FetchException) e.getCause();
            }
            throw new FetchException("Failed to get organizations for " + integrationKey, e);
        }
    }

    private List<String> getOrganizationsLive(IntegrationKey integrationKey) throws FetchException {
        log.debug("Fetching orgs from cloud for {}", integrationKey);
        boolean isGithubApp = false;
        try {
            Integration integration = inventoryService.getIntegration(integrationKey);
            isGithubApp = integration.isGithubAppsIntegration();
        } catch (InventoryException e) {
            log.error("Failed to get integration from inventory for {}", integrationKey, e);
        }
        try {
            if (isGithubApp) {
                return clientFactory.get(integrationKey, false).streamAppInstallationOrgs()
                        .map(GithubOrganization::getLogin)
                        .filter(StringUtils::isNotEmpty)
                        .collect(Collectors.toList());
            } else {
                return clientFactory.get(integrationKey, false).streamOrganizations()
                        .map(GithubOrganization::getLogin)
                        .filter(StringUtils::isNotEmpty)
                        .collect(Collectors.toList());
            }
        } catch (GithubClientException e) {
            throw new FetchException("Failed to fetch Github organizations for integration=" + integrationKey, e);
        }
    }

}
