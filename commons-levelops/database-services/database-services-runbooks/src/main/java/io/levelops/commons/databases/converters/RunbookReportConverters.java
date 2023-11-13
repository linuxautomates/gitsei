package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.runbooks.RunbookReport;
import io.levelops.commons.dates.DateUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

@Log4j2
public class RunbookReportConverters {

    public static RowMapper<RunbookReport> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> RunbookReport.builder()
                .id(rs.getString("id"))
                .runbookId(rs.getString("runbook_id"))
                .runId(rs.getString("run_id"))
                .source(rs.getString("source"))
                .title(rs.getString("title"))
                .gcsPath(rs.getString("gcs_path"))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }

}
