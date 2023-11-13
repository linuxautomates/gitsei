package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.response.DbAggregationResult;
import org.springframework.jdbc.core.RowMapper;

import java.util.Optional;

public class DbJiraProjectConverters {
    public static RowMapper<DbAggregationResult> distinctRowMapper(String key, Optional<String> additionalKey) {
        return (rs, rowNumber) ->
                DbAggregationResult.builder()
                        .key(rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .build();
    }
}
