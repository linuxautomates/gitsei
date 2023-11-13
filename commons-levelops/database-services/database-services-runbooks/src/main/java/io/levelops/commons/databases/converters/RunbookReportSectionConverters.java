package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.runbooks.RunbookReportSection;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;

import org.springframework.jdbc.core.RowMapper;

@Log4j2
public class RunbookReportSectionConverters {

    public static RowMapper<RunbookReportSection> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> RunbookReportSection.builder()
                .id(rs.getString("id"))
                .source(rs.getString("source"))
                .reportId(rs.getString("report_id"))
                .gcsPath(rs.getString("gcs_path"))
                .pageCount(rs.getInt("page_count"))
                .pageSize(rs.getInt("page_size"))
                .totalCount(rs.getInt("total_count"))
                .title(rs.getString("title"))
                .metadata(ParsingUtils.parseJsonObject(objectMapper, "metadata", rs.getString("metadata")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }

}
