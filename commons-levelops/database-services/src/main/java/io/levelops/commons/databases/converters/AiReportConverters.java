package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.AiReport;
import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.databases.models.database.runbooks.RunbookNodeTemplate;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

public class AiReportConverters {
    public static RowMapper<AiReport> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> AiReport.builder()
                .id(UUID.fromString(rs.getString("id")))
                .type(rs.getString("type"))
                .key(rs.getString("key"))
                .data(ParsingUtils.parseJsonObject(objectMapper, "data", rs.getString("data")))
                .error(ParsingUtils.parseJsonObject(objectMapper, "error", rs.getString("error")))
                .dataUpdatedAt(DateUtils.toInstant(rs.getTimestamp("data_updated_at")))
                .errorUpdatedAt(DateUtils.toInstant(rs.getTimestamp("error_updated_at")))
                .build();
    }
}
