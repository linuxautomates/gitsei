package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.dates.DateUtils;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

public class DbWorkItemFieldConverters {

    public static RowMapper<DbWorkItemField> workItemFieldRowMapper() {
        return (rs, rowNumber) -> DbWorkItemField.builder()
                .id(rs.getObject("id", UUID.class))
                .custom(rs.getBoolean("custom"))
                .integrationId(rs.getString("integration_id"))
                .name(rs.getString("name"))
                .fieldKey(rs.getString("field_key"))
                .fieldType(rs.getString("field_type"))
                .itemsType(rs.getString("items_type"))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }
}
