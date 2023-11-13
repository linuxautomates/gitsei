package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.databases.models.database.runbooks.RunbookNodeTemplate;
import io.levelops.commons.databases.models.database.runbooks.RunbookNodeTemplate.RunbookOutputField;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;


@Log4j2
public class RunbookNodeTemplateConverters {

    public static RowMapper<RunbookNodeTemplate> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> RunbookNodeTemplate.builder()
                .id(rs.getString("id"))
                .type(rs.getString("type"))
                .nodeHandler(rs.getString("node_handler"))
                .hidden(rs.getBoolean("hidden"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .category(rs.getString("category"))
                .input(ParsingUtils.parseMap(objectMapper, "input", String.class, KvField.class, rs.getString("input")))
                .output(ParsingUtils.parseMap(objectMapper, "output", String.class, RunbookOutputField.class, rs.getString("output")))
                .options(ParsingUtils.parseList(objectMapper, "options", String.class, rs.getString("options")))
                .uiData(ParsingUtils.parseJsonObject(objectMapper, "uiData", rs.getString("ui_data")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }

}
