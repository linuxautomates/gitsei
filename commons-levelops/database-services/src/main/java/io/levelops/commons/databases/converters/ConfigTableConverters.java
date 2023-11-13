package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.services.ConfigTableDatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import org.springframework.jdbc.core.RowMapper;

import java.util.EnumSet;

public class ConfigTableConverters {

    public static RowMapper<ConfigTable> rowMapper(ObjectMapper objectMapper, EnumSet<ConfigTableDatabaseService.Field> expand) {
        return (rs, rowNumber) -> ConfigTable.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .schema(expand.contains(ConfigTableDatabaseService.Field.SCHEMA) ? ParsingUtils.parseObject(objectMapper, "schema", ConfigTable.Schema.class, rs.getString("schema")) : null)
                .totalRows(rs.getInt("total_rows"))
                .rows(expand.contains(ConfigTableDatabaseService.Field.ROWS) ? ParsingUtils.parseMap(objectMapper, "rows", String.class, ConfigTable.Row.class, rs.getString("rows")) : null)
                .version(rs.getString("version"))
                .history(expand.contains(ConfigTableDatabaseService.Field.HISTORY) ? ParsingUtils.parseMap(objectMapper, "history", String.class, ConfigTable.Revision.class, rs.getString("history")) : null)
                .updatedBy(rs.getString("updated_by"))
                .createdBy(rs.getString("created_by"))
                .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }

}
