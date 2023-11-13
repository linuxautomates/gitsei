package io.levelops.commons.tenant_management.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.tenant_management.models.TaskStatus;
import io.levelops.commons.tenant_management.models.TaskTracker;
import io.levelops.commons.tenant_management.models.TaskType;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

public class TaskTrackersConverters {
    public static RowMapper<TaskTracker> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> TaskTracker.builder()
                .id((UUID)rs.getObject("id"))
                .type(TaskType.fromString(rs.getString("type")))
                .status(TaskStatus.fromString(rs.getString("status")))
                .frequency(rs.getInt("frequency"))
                .statusChangedAt(DateUtils.toInstant(rs.getTimestamp("status_changed_at")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                .build();
    }
}
