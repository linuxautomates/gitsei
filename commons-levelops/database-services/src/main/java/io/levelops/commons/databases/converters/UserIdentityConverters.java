package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.scm.DbScmUser.MappingStatus;
import io.levelops.commons.databases.utils.DatabaseUtils;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UserIdentityConverters {
    public static RowMapper<DbScmUser> userRowMapper() {
        return (rs, rowNumber) -> {
            MappingStatus mappingStatus = MappingStatus.AUTO; // By default, infer this as auto
            if (rs.getString("mapping_status") != null) {
                mappingStatus = MappingStatus.fromString(rs.getString("mapping_status"));
            }
            return DbScmUser.builder()
                    .id(rs.getString("id"))
                    .integrationId(rs.getString("integration_id"))
                    .cloudId(rs.getString("cloud_id"))
                    .displayName(rs.getString("display_name"))
                    .originalDisplayName(rs.getString("original_display_name"))
                    .emails(DatabaseUtils.fromSqlArray(rs.getArray("emails"), String.class).collect(Collectors.toList()))
                    .mappingStatus(mappingStatus)
                    .createdAt(rs.getLong("created_at"))
                    .updatedAt(rs.getLong("updated_at"))
                    .build();
        };
    }
}
