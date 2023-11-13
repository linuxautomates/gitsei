package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.CiCDPreProcessTask;
import io.levelops.commons.dates.DateUtils;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

public class CiCdPreProcessTaskConverters {

    public static RowMapper<CiCDPreProcessTask> mapCiCdPreProcessTask() {
        return (rs, rowNumber) -> {
            CiCDPreProcessTask ciCDPreProcessTask = CiCDPreProcessTask.builder()
                    .id(rs.getObject("id", UUID.class))
                    .tenantId(rs.getString("tenant_id"))
                    .metaData(rs.getString("metaData"))
                    .status(rs.getString("status"))
                    .attemptCount(rs.getInt("attempts_count"))
                    .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                    .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                    .statusChangedAt(DateUtils.toInstant(rs.getTimestamp("status_changed_at")))
                    .build();
            return ciCDPreProcessTask;
        };
    }
}

