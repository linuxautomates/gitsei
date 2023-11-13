package io.levelops.commons.databases.converters.precalculation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.precalculation.WidgetPrecalculatedReport;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DefaultListRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

@Log4j2
public class WidgetPrecalculatedReportConverters {
    public static RowMapper<WidgetPrecalculatedReport> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> {
            return WidgetPrecalculatedReport.builder()
                    .id((UUID)rs.getObject("id"))
                    .widgetId((UUID)rs.getObject("widget_id"))
                    .widget(ParsingUtils.parseObject(objectMapper, "widget", Widget.class, rs.getString("widget")))
                    .listRequest(ParsingUtils.parseObject(objectMapper, "list_request", DefaultListRequest.class, rs.getString("list_request")))
                    .ouRefId(rs.getInt("ou_ref_id"))
                    .ouID((UUID)rs.getObject("ou_id"))
                    .reportSubType(rs.getString("report_sub_type"))
                    .report(rs.getString("report"))
                    .calculatedAt(DateUtils.toInstant(rs.getTimestamp("calculated_at")))
                    .interval(rs.getString("interval"))
                    .startTime(DateUtils.toInstant(rs.getTimestamp("start_time")))
                    .endTime(DateUtils.toInstant(rs.getTimestamp("end_time")))
                    .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                    .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                    .build();
        };
    }
}
