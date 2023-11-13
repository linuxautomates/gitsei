package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DbWorkItemHistoryConverters {

    public static DbWorkItemHistory mapToDbWorkItemHistory(ResultSet rs) throws SQLException {
        return DbWorkItemHistory.builder()
                .id(UUID.fromString(rs.getString("id")))
                .fieldType(rs.getString("field_type"))
                .fieldValue(rs.getString("field_value"))
                .workItemId(rs.getString("workitem_id"))
                .integrationId(rs.getString("integration_id"))
                .startDate(rs.getTimestamp("start_date"))
                .endDate(rs.getTimestamp("end_date"))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .build();
    }


    public static RowMapper<DbWorkItemHistory> workItemHistoricalMapper() {
        return (rs, rowNumber) -> DbWorkItemHistory.builder()
                .id(UUID.fromString(rs.getString("id")))
                .fieldType(rs.getString("field_type"))
                .fieldValue(rs.getString("field_value"))
                .workItemId(rs.getString("workitem_id"))
                .integrationId(rs.getString("integration_id"))
                .startDate(rs.getTimestamp("start_date"))
                .endDate(rs.getTimestamp("end_date"))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .build();
    }

    public static RowMapper<DbWorkItemHistory> listRowMapper() {
        return (rs, rowNumber) -> mapToDbWorkItemHistory(rs);
    }

    public static List<DbWorkItemHistory> mapToDbWorkItemHistoryList(ResultSet rs) throws SQLException {
        List<DbWorkItemHistory> workItemHistories = new ArrayList<>();
        while (rs.next()) {
            workItemHistories.add(
                    DbWorkItemHistory.builder()
                            .id(UUID.fromString(rs.getString("id")))
                            .fieldType(rs.getString("field_type"))
                            .fieldValue(rs.getString("field_value"))
                            .workItemId(rs.getString("workitem_id"))
                            .integrationId(rs.getString("integration_id"))
                            .startDate(rs.getTimestamp("start_date"))
                            .endDate(rs.getTimestamp("end_date"))
                            .createdAt(rs.getTimestamp("created_at"))
                            .updatedAt(rs.getTimestamp("updated_at"))
                            .build()
            );
        }
        return workItemHistories;
    }

    public static List<DbWorkItemHistory> sanitizeEventList(List<DbWorkItemHistory> events, Instant now) {
        if (CollectionUtils.isEmpty(events)) {
            return List.of();
        }
        ArrayList<DbWorkItemHistory> sanitizedEvents = new ArrayList<>();
        for (int i = 0; i < events.size() - 1; i++) {
            DbWorkItemHistory current = events.get(i);
            DbWorkItemHistory next = events.get(i + 1);
            if (current.getEndDate() == null) {
                current = current.toBuilder()
                        .endDate(next.getStartDate())
                        .build();
            }
            sanitizedEvents.add(current);
        }
        DbWorkItemHistory last = events.get(events.size() - 1);
        if (last.getEndDate() == null) {
            last = last.toBuilder()
                    .endDate(Timestamp.from(now))
                    .build();
        }
        sanitizedEvents.add(last);
        return sanitizedEvents;
    }
}
