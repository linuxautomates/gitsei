package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.runbooks.RunbookTemplate;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;

import org.springframework.jdbc.core.RowMapper;


@Log4j2
public class RunbookTemplateConverters {

    public static RowMapper<RunbookTemplate> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> RunbookTemplate.builder()
                .id(rs.getString("id"))
                .hidden(rs.getBoolean("hidden"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .category(rs.getString("category"))
                .metadata(ParsingUtils.parseJsonObject(objectMapper, "metadata", rs.getString("metadata")))
                .data(ParsingUtils.parseJsonObject(objectMapper, "data", rs.getString("data")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }

}
