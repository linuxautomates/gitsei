package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.filters.QuestionnaireAggFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import org.springframework.jdbc.core.RowMapper;

public class QuestionnaireConverters {

    public static RowMapper<DbAggregationResult> aggResultRowMapper(String key, QuestionnaireAggFilter.Calculation calc) {
        return (rs, rowNumber) -> {
            if (calc == null) { //values only
                return DbAggregationResult.builder()
                        .key(rs.getString(key))
                        .build();
            } else if (calc == QuestionnaireAggFilter.Calculation.count) {
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
        };
    }

}
