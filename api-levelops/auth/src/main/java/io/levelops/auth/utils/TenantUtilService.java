package io.levelops.auth.utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.services.TenantService;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TenantUtilService {
    @Autowired
    private TenantService tenantService;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final String ALL_ACTIVE_TENANTS_KEY = "all_active_tenants";
    private static final Set<String> EXCLUDED_TENANTS_FOR_VALIDATION = Set.of("_levelops");

    private final LoadingCache<String, List<String>> activeTenantsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1)
            .build(new CacheLoader<>() {
                @NotNull
                @Override
                public List<String> load(@Nonnull String key) {
                    return PaginationUtils.stream(0, 1, RuntimeStreamException.wrap(
                            page -> tenantService.list(
                                    "",
                                    page,
                                    DEFAULT_PAGE_SIZE
                            ).getRecords())).map(Tenant::getId).collect(Collectors.toList());
                }
            });
    public List<String> getActiveTenants() throws ExecutionException {
        return activeTenantsCache.get(ALL_ACTIVE_TENANTS_KEY);
    }

    public void reloadCache() {
        activeTenantsCache.refresh(ALL_ACTIVE_TENANTS_KEY);
    }

    public void validateTenant(String company) throws IllegalAccessException, ExecutionException {
        if (EXCLUDED_TENANTS_FOR_VALIDATION.contains(company)) {
            return;
        }
        List<String> activeTenants = getActiveTenants();
        if (!activeTenants.contains(company)) {
            throw new IllegalAccessException("org " + company + " doesn't exist");
        }
    }
}
