package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.GlobalTracker;
import io.levelops.commons.dates.DateUtils;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

public class GlobalTrackersConverters {
    public static RowMapper<GlobalTracker> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> GlobalTracker.builder()
                .id((UUID)rs.getObject("id"))
                .type(rs.getString("type"))
                .status(rs.getString("status"))
                .frequency(rs.getInt("frequency"))
                .statusChangedAt(DateUtils.toInstant(rs.getTimestamp("status_changed_at")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                .build();
    }
}
