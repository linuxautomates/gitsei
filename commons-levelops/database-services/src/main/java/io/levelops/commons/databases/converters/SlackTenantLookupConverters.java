package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.SlackTenantLookup;
import io.levelops.commons.dates.DateUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

@Log4j2
public class SlackTenantLookupConverters {
    public static RowMapper<SlackTenantLookup> rowMapper() {
        return (rs, rowNumber) -> SlackTenantLookup.builder()
                .id((UUID) rs.getObject("id"))
                .teamId(rs.getString("team_id"))
                .tenantName(rs.getString("tenant_name"))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }
}
