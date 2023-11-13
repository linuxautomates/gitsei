package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.filters.WorkItemFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import org.springframework.jdbc.core.RowMapper;

public class WorkItemConverters {

    public static RowMapper<DbAggregationResult> aggResultRowMapper(String distinctKey, WorkItemFilter.Calculation calc) {
        return (rs, rowNumber) -> {
            if (calc == null) { //values only
                return DbAggregationResult.builder()
                        .key(rs.getString(distinctKey))
                        .build();
            } else if (calc == WorkItemFilter.Calculation.count) {
                return DbAggregationResult.builder()
                        .key(rs.getString(distinctKey))
                        .total(rs.getLong("ct"))
                        .build();
            } else {
                return DbAggregationResult.builder()
                        .key(rs.getString(distinctKey))
                        .total(rs.getLong("ct"))
                        .max(rs.getLong("mx"))
                        .min(rs.getLong("mn"))
                        .median(rs.getLong("percentile_disc"))
                        .build();
            }
        };
    }

}
