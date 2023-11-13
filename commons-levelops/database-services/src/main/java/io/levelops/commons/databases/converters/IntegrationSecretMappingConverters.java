package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.IntegrationSecretMapping;
import io.levelops.commons.dates.DateUtils;
import org.springframework.jdbc.core.RowMapper;

public class IntegrationSecretMappingConverters {

    public static RowMapper<IntegrationSecretMapping> rowMapper() {
        return (rs, rowNumber) -> IntegrationSecretMapping.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getString("integration_id"))
                .name(rs.getString("name"))
                .smConfigId(rs.getString("sm_config_id"))
                .smKey(rs.getString("sm_key"))
                .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }
}
