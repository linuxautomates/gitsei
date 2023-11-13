package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.sql.SQLException;


@Log4j2
public class TicketCategorizationSchemeConverters {

    public static RowMapper<TicketCategorizationScheme> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> TicketCategorizationScheme.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .defaultScheme(rs.getBoolean("default_scheme"))
                .config(ParsingUtils.parseObject(objectMapper, "config", TicketCategorizationScheme.TicketCategorizationConfig.class, rs.getString("config")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                .build();
    }

    public static RowMapper<TicketCategorizationScheme.TicketCategorization> categoryRowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> {
            TicketCategorizationScheme.TicketCategorization ret = null;
            try {
                ret = objectMapper.readValue(rs.getString("config"), TicketCategorizationScheme.TicketCategorization.class);
            } catch (JsonProcessingException e) {
                throw new SQLException("could not de-serialize category");
            }
            ret = ret.toBuilder().id(rs.getString("id")).build();
            return ret;
        };
    }

}
