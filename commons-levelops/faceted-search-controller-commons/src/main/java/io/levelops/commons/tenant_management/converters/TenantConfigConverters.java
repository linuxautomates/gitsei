package io.levelops.commons.tenant_management.converters;

import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.tenant_management.models.TenantConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.time.Instant;

@Log4j2
public class TenantConfigConverters {
    public static RowMapper<TenantConfig> mapTenantConfig() {
        return (rs, rowNumber) -> {
            Long id = rs.getLong("id");
            String tenantId = rs.getString("tenant_id");

            Boolean enabled = rs.getBoolean("enabled");
            Integer priority = rs.getInt("priority");

            Boolean markedForDeletion = rs.getBoolean("marked_for_deletion");
            Instant markedForDeletionAt = DateUtils.toInstant(rs.getTimestamp("marked_for_deletion_at"));

            Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));
            Instant updatedAt = DateUtils.toInstant(rs.getTimestamp("updated_at"));

            TenantConfig tenantConfig = TenantConfig.builder()
                    .id(id)
                    .tenantId(tenantId)
                    .enabled(enabled)
                    .priority(priority)
                    .markedForDeletion(markedForDeletion)
                    .markedForDeletionAt(markedForDeletionAt)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();
            return tenantConfig;
        };
    }
}
