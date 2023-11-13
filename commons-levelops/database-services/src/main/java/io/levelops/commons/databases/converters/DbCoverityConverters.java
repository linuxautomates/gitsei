package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.coverity.DbCoverityDefect;
import io.levelops.commons.databases.models.database.coverity.DbCoveritySnapshot;
import io.levelops.commons.databases.models.database.coverity.DbCoverityStream;
import io.levelops.commons.databases.models.filters.CoverityDefectFilter;
import io.levelops.commons.databases.models.filters.CoveritySnapshotFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.util.Optional;

@Log4j2
public class DbCoverityConverters {

    public static RowMapper<DbCoverityStream> streamRowMapper() {
        return (rs, rowNumber) -> DbCoverityStream.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getInt("integration_id"))
                .name(rs.getString("name"))
                .language(rs.getString("language"))
                .project(rs.getString("project"))
                .triageStoreId(rs.getString("triage_store_id"))
                .createdAt(rs.getDate("created_at"))
                .updatedAt(rs.getDate("updated_at"))
                .build();
    }

    public static RowMapper<DbCoveritySnapshot> snapshotRowMapper() {
        return (rs, rowNumber) -> DbCoveritySnapshot.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getInt("integration_id"))
                .streamId(rs.getString("stream_id"))
                .snapshotId(rs.getInt("snapshot_id"))
                .analysisHost(rs.getString("analysis_host"))
                .analysisVersion(rs.getString("analysis_version"))
                .timeTaken(rs.getInt("time_taken"))
                .buildFailureCount(rs.getInt("build_failure_count"))
                .buildSuccessCount(rs.getInt("build_success_count"))
                .commitUser(rs.getString("commit_user"))
                .snapshotCreatedAt(rs.getTimestamp("snapshot_created_at"))
                .createdAt(rs.getDate("created_at"))
                .updatedAt(rs.getDate("updated_at"))
                .build();
    }

    public static RowMapper<DbCoverityDefect> defectRowMapper() {
        return (rs, rowNumber) -> DbCoverityDefect.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getInt("integration_id"))
                .snapshotId(rs.getString("snapshot_id"))
                .cid(rs.getInt("cid"))
                .checkerName(rs.getString("checker_name"))
                .componentName(rs.getString("component_name"))
                .cwe(rs.getInt("cid"))
                .dbAttributes(ParsingUtils.parseJsonObject(DefaultObjectMapper.get(),
                        "defect",
                        rs.getString("attributes")))
                .category(rs.getString("category"))
                .impact(rs.getString("impact"))
                .kind(rs.getString("kind"))
                .type(rs.getString("type"))
                .domain(rs.getString("domain"))
                .filePath(rs.getString("file_path"))
                .functionName(rs.getString("function_name"))
                .firstDetectedAt(rs.getTimestamp("first_detected_at"))
                .firstDetectedBy(rs.getString("first_detected_by"))
                .firstDetectedStream(rs.getString("first_detected_stream"))
                .firstDetectedSnapshotId(rs.getInt("first_detected_snapshot_id"))
                .lastDetectedAt(rs.getTimestamp("last_detected_at"))
                .lastDetectedStream(rs.getString("last_detected_stream"))
                .lastDetectedSnapshotId(rs.getInt("last_detected_snapshot_id"))
                .mergeKey(rs.getString("merge_key"))
                .misraCategory(rs.getString("misra_category"))
                .occurrenceCount(rs.getInt("occurrence_count"))
                .createdAt(rs.getDate("created_at"))
                .updatedAt(rs.getDate("updated_at"))
                .build();
    }

    public static RowMapper<DbAggregationResult> distinctRowMapper(String key,
                                                                   CoverityDefectFilter.CALCULATION calc,
                                                                   Optional<String> additionalKey) {
        return (rs, rowNumber) -> {
            switch (calc) {
                default:
                    return DbAggregationResult.builder()
                            .key(rs.getString(key))
                            .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                            .totalDefects(rs.getLong("ct"))
                            .build();
            }
        };
    }

    public static RowMapper<DbAggregationResult> snapshotRowMapper(String key,
                                                                   CoveritySnapshotFilter.CALCULATION calc) {
        return (rs, rowNumber) -> {
            switch (calc) {
                case count:
                    return DbAggregationResult.builder()
                            .key(rs.getString(key))
                            .totalDefects(rs.getLong("ct"))
                            .build();

                default:
                    return DbAggregationResult.builder()
                            .key(rs.getString(key))
                            .totalDefects(rs.getLong("ct"))
                            .max(rs.getLong("mx"))
                            .min(rs.getLong("mn"))
                            .median(rs.getLong("median"))
                            .build();
            }
        };
    }
}
