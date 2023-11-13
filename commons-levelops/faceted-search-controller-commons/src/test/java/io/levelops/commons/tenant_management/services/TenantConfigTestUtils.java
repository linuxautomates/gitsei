package io.levelops.commons.tenant_management.services;

import io.levelops.commons.tenant_management.models.TenantConfig;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TenantConfigTestUtils {
    public static TenantConfig createTenantConfig(TenantConfigDBService tenantConfigDBService, int i) throws SQLException {
        TenantConfig tenantConfig = TenantConfig.builder()
                .tenantId("foo-" + i)
                .priority(i)
                .build();
        String id = tenantConfigDBService.insert(null, tenantConfig);
        return tenantConfig.toBuilder()
                .id(Long.valueOf(id))
                .build();
    }
    public static List<TenantConfig> createTenantConfigs(TenantConfigDBService tenantConfigDBService, int n) throws SQLException {
        List<TenantConfig> tenantConfigs = new ArrayList<>();
        for (int i =0; i<n; i++) {
            TenantConfig tenantConfig = createTenantConfig(tenantConfigDBService, i);
            tenantConfigs.add(tenantConfig);
        }
        return tenantConfigs;
    }
}
