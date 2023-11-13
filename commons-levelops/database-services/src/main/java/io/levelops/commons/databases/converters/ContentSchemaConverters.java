package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.ContentSchema;
import org.springframework.jdbc.core.RowMapper;

import java.sql.SQLException;

public class ContentSchemaConverters {

    public static RowMapper<ContentSchema> rowMapper(ObjectMapper objectMapper) {
        return (rs, row) -> {
            try {
                return objectMapper.readValue(rs.getString("content_schema"), ContentSchema.class).toBuilder()
                        .key(rs.getString("id"))
                        .build();
            } catch (JsonProcessingException e) {
                throw new SQLException("Failed to deserialize content schema with id=" + rs.getString("id"));
            }
        };
    }
}
