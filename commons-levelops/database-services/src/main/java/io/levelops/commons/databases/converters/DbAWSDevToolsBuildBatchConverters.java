package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsBuildBatch;
import io.levelops.commons.databases.models.filters.AWSDevToolsBuildBatchesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DbAWSDevToolsBuildBatchConverters {

    public static RowMapper<DbAWSDevToolsBuildBatch> listRowMapper() {
        return (((rs, rowNum) -> buildDbAWSDevToolsBuildBatch(rs)));
    }

    private static DbAWSDevToolsBuildBatch buildDbAWSDevToolsBuildBatch(ResultSet rs) throws SQLException {
        return DbAWSDevToolsBuildBatch.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getString("integration_id"))
                .buildBatchId(rs.getString("build_batch_id"))
                .arn(rs.getString("arn"))
                .buildBatchStartedAt(rs.getTimestamp("build_batch_started_at"))
                .buildBatchEndedAt(rs.getTimestamp("build_batch_ended_at"))
                .buildBatchComplete(rs.getString("build_batch_complete"))
                .buildBatchNumber(rs.getLong("build_batch_number"))
                .lastPhase(rs.getString("last_phase"))
                .lastPhaseStatus(rs.getString("last_phase_status"))
                .status(rs.getString("status"))
                .projectName(rs.getString("project_name"))
                .projectArn(rs.getString("project_arn"))
                .initiator(rs.getString("initiator"))
                .sourceType(rs.getString("source_type"))
                .sourceLocation(rs.getString("source_location"))
                .sourceVersion(rs.getString("source_version"))
                .resolvedSourceVersion(rs.getString("resolved_source_version"))
                .region(rs.getString("region"))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .build();
    }

    public static RowMapper<DbAggregationResult> aggRowMapper(String key, AWSDevToolsBuildBatchesFilter.CALCULATION CALCULATION) {
        return ((rs, rowNum) -> {
            if (AWSDevToolsBuildBatchesFilter.CALCULATION.build_batch_count == CALCULATION) {
                return DbAggregationResult.builder()
                        .key(rs.getString(key))
                        .total(rs.getLong("ct"))
                        .build();
            } else {
                return DbAggregationResult.builder()
                        .key(rs.getString(key))
                        .total(rs.getLong("ct"))
                        .max(rs.getLong("mx"))
                        .min(rs.getLong("mn"))
                        .median(rs.getLong("percentile_disc"))
                        .build();
            }
        });
    }
}
