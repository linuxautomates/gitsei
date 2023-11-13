package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IndustryDevProductivityReport;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

@Log4j2
public class IndustryDevProductivityReportsConverters {
    public static RowMapper<IndustryDevProductivityReport> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> IndustryDevProductivityReport.builder()
                .id((UUID)rs.getObject("id"))
                .interval(ReportIntervalType.fromString(rs.getString("interval")))
                .score(rs.getInt("score"))
                .report(ParsingUtils.parseObject(objectMapper, "report", DevProductivityResponse.class, rs.getString("report")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                .build();
    }
}
