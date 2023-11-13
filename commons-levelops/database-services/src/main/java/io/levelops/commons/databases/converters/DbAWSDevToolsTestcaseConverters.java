package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsTestcase;
import io.levelops.commons.databases.models.filters.AWSDevToolsTestcasesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DbAWSDevToolsTestcaseConverters {

    public static RowMapper<DbAWSDevToolsTestcase> listRowMapper() {
        return (((rs, rowNum) -> buildDbAWSDevToolsTestcase(rs)));
    }

    private static DbAWSDevToolsTestcase buildDbAWSDevToolsTestcase(ResultSet rs) throws SQLException {
        return DbAWSDevToolsTestcase.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .prefix(rs.getString("prefix"))
                .reportArn(rs.getString("report_arn"))
                .reportId(rs.getString("report_id"))
                .status(rs.getString("status"))
                .duration(rs.getLong("duration"))
                .expired(rs.getTimestamp("expired"))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .build();
    }

    public static RowMapper<DbAggregationResult> aggRowMapper(String key, AWSDevToolsTestcasesFilter.CALCULATION CALCULATION) {
        return ((rs, rowNum) -> {
            if (AWSDevToolsTestcasesFilter.CALCULATION.testcase_count.equals(CALCULATION)) {
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
