package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsBuild;
import io.levelops.commons.databases.models.filters.AWSDevToolsBuildsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DbAWSDevToolsBuildConverters {

    public static RowMapper<DbAWSDevToolsBuild> listRowMapper() {
        return (((rs, rowNum) -> buildDbAWSDevToolsBuild(rs)));
    }

    private static DbAWSDevToolsBuild buildDbAWSDevToolsBuild(ResultSet rs) throws SQLException {
        return DbAWSDevToolsBuild.builder()
                .id(rs.getString("id"))
                .buildId(rs.getString("build_id"))
                .integrationId(rs.getString("integration_id"))
                .arn(rs.getString("arn"))
                .buildNumber(rs.getLong("build_number"))
                .buildComplete(rs.getString("build_complete"))
                .buildStartedAt(rs.getTimestamp("build_started_at"))
                .buildEndedAt(rs.getTimestamp("build_ended_at"))
                .status(rs.getString("status"))
                .lastPhase(rs.getString("last_phase"))
                .lastPhaseStatus(rs.getString("last_phase_status"))
                .projectName(rs.getString("project_name"))
                .projectArn(rs.getString("project_arn"))
                .buildBatchArn(rs.getString("build_batch_arn"))
                .initiator(rs.getString("initiator"))
                .sourceType(rs.getString("source_type"))
                .sourceLocation(rs.getString("source_location"))
                .resolvedSourceVersion(rs.getString("resolved_source_version"))
                .region(rs.getString("region"))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .build();
    }

    public static RowMapper<DbAggregationResult> aggRowMapper(String key, AWSDevToolsBuildsFilter.CALCULATION CALCULATION) {
        return ((rs, rowNum) -> {
            if (AWSDevToolsBuildsFilter.CALCULATION.build_count == CALCULATION) {
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
