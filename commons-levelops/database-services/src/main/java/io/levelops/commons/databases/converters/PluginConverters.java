package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Plugin;
import io.levelops.commons.databases.models.database.Plugin.PluginClass;
import org.springframework.jdbc.core.RowMapper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class PluginConverters {

    public static RowMapper<Plugin> pluginRowMapper(ObjectMapper objectMapper) {
        return (rs, row) -> {
            try {
                return Plugin.builder()
                        .id(String.valueOf(rs.getObject("id")))
                        .custom(rs.getBoolean("custom"))
                        .pluginClass(PluginClass.fromString(rs.getString("class")))
                        .tool(rs.getString("tool"))
                        .version(rs.getString("version"))
                        .name(rs.getString("name"))
                        .description(rs.getString("description"))
                        .readme(parseReadme(objectMapper, rs.getString("readme")))
                        .gcsPath(rs.getString("gcs_path"))
                        .createdAt(rs.getLong("created_at"))
                        .build();
            } catch (IOException e) {
                throw new SQLException("Failed to deserialize plugin with id=" + rs.getString("id"), e);
            }
        };
    }

    public static Map<String, Object> parseReadme(ObjectMapper objectMapper, String readmeString) throws JsonProcessingException {
        return objectMapper.readValue(readmeString, objectMapper.getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class));
    }
}
