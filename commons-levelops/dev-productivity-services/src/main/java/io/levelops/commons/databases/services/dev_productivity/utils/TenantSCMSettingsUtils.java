package io.levelops.commons.databases.services.dev_productivity.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.SQLException;

@Log4j2
public class TenantSCMSettingsUtils {
    public static final TenantSCMSettings EMPTY_TENANT_SCM_SETTINGS = TenantSCMSettings.builder().build();

    public static TenantSCMSettings getTenantSCMSettings(final TenantConfigService tenantConfigService, final ObjectMapper mapper, final String company) throws SQLException {
        DbListResponse<TenantConfig> tenantConfigs = tenantConfigService.listByFilter(company, "SCM_GLOBAL_SETTINGS", 0, 1);
        if(CollectionUtils.isEmpty(tenantConfigs.getRecords())) {
            return EMPTY_TENANT_SCM_SETTINGS;
        }
        TenantConfig tenantConfig = tenantConfigs.getRecords().get(0);
        try {
            TenantSCMSettings tenantSCMSettings = mapper.readValue(tenantConfig.getValue(), TenantSCMSettings.class);
            log.debug("tenantSCMSettings = {}", tenantSCMSettings);
            return tenantSCMSettings;
        } catch (JsonProcessingException e) {
            log.debug("Error deserializing TenantSCMSettings!", e);
            return EMPTY_TENANT_SCM_SETTINGS;
        }
    }
}
