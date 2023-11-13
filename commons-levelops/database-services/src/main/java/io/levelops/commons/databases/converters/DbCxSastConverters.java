package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.checkmarx.DbCxSastIssue;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastScan;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import org.springframework.jdbc.core.RowMapper;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public class DbCxSastConverters {

    public static RowMapper<DbCxSastIssue> issueRowMapper() {
        return (rs, rowNumber) -> DbCxSastIssue.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getString("integration_id"))
                .queryId(rs.getString("query_id"))
                .nodeId(rs.getString("node_id"))
                .status(rs.getString("status"))
                .falsePositive(rs.getBoolean("false_positive"))
                .severity(rs.getString("severity"))
                .assignee(rs.getString("assignee"))
                .state(rs.getString("state"))
                .detectionDate(rs.getDate("detection_date"))
                .build();
    }

    public static RowMapper<DbCxSastIssue> fileRowMapper() {
        return (rs, rowNumber) -> DbCxSastIssue.builder()
                .nodeId(rs.getString("node_id"))
                .fileName(rs.getString("file_name"))
                .line(rs.getInt("line_number"))
                .column(rs.getInt("column_number"))
                .build();
    }

    public static RowMapper<DbAggregationResult> distinctIssueRowMapper(String key, Optional<String> additionalKey) {
        return (rs, rowNumber) ->
                DbAggregationResult.builder()
                        .key(key.equals("none") ? null : rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .count(rs.getLong("ct"))
                        .build();

    }

    public static RowMapper<DbCxSastScan> scanRowMapper() {
        return (rs, rowNumber) -> DbCxSastScan.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getString("integration_id"))
                .scanPath(rs.getString("scan_path"))
                .owner(rs.getString("owner"))
                .isPublic(rs.getBoolean("is_public"))
                .languages(convertSQLArrayToList(rs.getArray("languages")))
                .initiatorName(rs.getString("initiator_name"))
                .scanStartedAt(Optional.ofNullable(rs.getTimestamp("scan_started_at"))
                        .map(Timestamp::toInstant).map(Instant::getEpochSecond).orElse(null))
                .scanFinishedAt(Optional.ofNullable(rs.getTimestamp("scan_finished_at"))
                        .map(Timestamp::toInstant).map(Instant::getEpochSecond).orElse(null))
                .scanId(rs.getString("scan_id"))
                .scanType(rs.getString("scan_type"))
                .status(rs.getString("status"))
                .projectId(rs.getString("project_id"))
                .scanRisk(rs.getInt("scan_risk"))
                .build();
    }

    public static RowMapper<DbAggregationResult> distinctScanRowMapper(String key) {
        return (rs, rowNumber) -> DbAggregationResult.builder()
                .key(key.equals("none") ? null : rs.getString(key))
                .count(rs.getLong("ct"))
                .build();
    }

    private static List<String> convertSQLArrayToList(java.sql.Array languages) throws SQLException {
        return (languages != null &&
                languages.getArray() != null) ?
                Arrays.asList((String[]) languages.getArray()) : List.of();
    }
}
