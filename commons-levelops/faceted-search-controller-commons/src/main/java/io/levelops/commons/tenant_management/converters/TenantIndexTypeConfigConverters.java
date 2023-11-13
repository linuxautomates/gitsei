package io.levelops.commons.tenant_management.converters;

import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.faceted_search.models.IndexType;
import io.levelops.commons.tenant_management.models.TenantIndexTypeConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.time.Instant;
import java.util.UUID;

@Log4j2
public class TenantIndexTypeConfigConverters {
    public static RowMapper<TenantIndexTypeConfig> mapTenantIndexTypeConfig() {
        return (rs, rowNumber) -> {
            UUID id = (UUID)rs.getObject("id");

            String tenantId = rs.getString("tenant_id");
            Long tenantConfigId = rs.getLong("tenant_config_id");

            IndexType indexType = IndexType.fromString(rs.getString("index_type"));

            Boolean enabled = rs.getBoolean("enabled");
            Integer priority = rs.getInt("priority");

            Long frequencyInMins = rs.getLong("frequency_in_mins");

            Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));
            Instant updatedAt = DateUtils.toInstant(rs.getTimestamp("updated_at"));

            TenantIndexTypeConfig tenantIndexTypeConfig = TenantIndexTypeConfig.builder()
                    .id(id)
                    .tenantId(tenantId)
                    .tenantConfigId(tenantConfigId)
                    .indexType(indexType)
                    .enabled(enabled)
                    .priority(priority)
                    .frequencyInMins(frequencyInMins)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();
            return tenantIndexTypeConfig;
        };
    }
}
