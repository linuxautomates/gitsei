package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.functional.IterableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class DefaultConfigService {

    private final TenantConfigService tenantConfigService;

    @Autowired
    public DefaultConfigService(TenantConfigService configService) {
        this.tenantConfigService = configService;
    }

    public void handleDefault(String company, String resultId, String configName)
            throws SQLException {
        TenantConfig config = IterableUtils.getFirst(tenantConfigService.listByFilter(company, configName, 0, 1)
                .getRecords()).orElse(null);
        if (config != null && resultId.equals(config.getValue())) {
            return;
        }
        if (config == null) {
            tenantConfigService.insert(company, TenantConfig.builder()
                    .name(configName)
                    .value(resultId)
                    .build());
        } else {
            tenantConfigService.update(company, TenantConfig.builder()
                    .id(config.getId())
                    .name(configName)
                    .value(resultId)
                    .build());
        }
    }

    public void deleteIfDefault(String company, String resultId, String configName)
            throws SQLException {
        TenantConfig config = IterableUtils.getFirst(tenantConfigService.listByFilter(company, configName, 0, 1)
                .getRecords()).orElse(null);
        if (config != null && resultId.equals(config.getValue())) {
            tenantConfigService.update(company, TenantConfig.builder()
                    .id(config.getId())
                    .name(configName)
                    .value("")
                    .build());
        }
    }

    public String getDefaultId(String company, String configName) throws SQLException {
        return IterableUtils.getFirst(tenantConfigService.listByFilter(company, configName, 0, 1).getRecords())
                .map(TenantConfig::getValue)
                .orElse("");
    }
}
