package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsProject;
import io.levelops.commons.databases.models.filters.AWSDevToolsProjectsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@Log4j2
public class DbAWSDevToolsProjectConverters {

    public static RowMapper<DbAWSDevToolsProject> listRowMapper() {
        return (((rs, rowNum) -> buildDbAWSDevToolsProject(rs)));
    }

    private static DbAWSDevToolsProject buildDbAWSDevToolsProject(ResultSet rs) throws SQLException {
        return DbAWSDevToolsProject.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getString("integration_id"))
                .name(rs.getString("name"))
                .arn(rs.getString("arn"))
                .sourceType(rs.getString("source_type"))
                .sourceLocation(rs.getString("source_location"))
                .sourceVersion(rs.getString("source_version"))
                .projectCreatedAt(rs.getTimestamp("project_created_at"))
                .projectModifiedAt(rs.getTimestamp("project_modified_at"))
                .region(rs.getString("region"))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .build();
    }

    public static RowMapper<DbAggregationResult> aggRowMapper(String key, AWSDevToolsProjectsFilter.CALCULATION CALCULATION) {
        return ((rs, rowNum) -> {
            if (AWSDevToolsProjectsFilter.CALCULATION.project_count.equals(CALCULATION)) {
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
