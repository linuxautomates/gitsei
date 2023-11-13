package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

@Log4j2
public class ActivityLogConverters {

    private static final ObjectMapper objectMapper = DefaultObjectMapper.get();

    public static RowMapper<ActivityLog> listRowMapper() {
        return ((rs, rowNum) -> buildActivityLog(rs));
    }

    private static ActivityLog buildActivityLog(ResultSet rs) throws SQLException {
        try {
            return ActivityLog.builder()
                    .id(rs.getString("id"))
                    .body(rs.getString("body"))
                    .email(rs.getString("email"))
                    .targetItem(rs.getString("targetitem"))
                    .targetItemType(ActivityLog.TargetItemType.fromString(
                            rs.getString("itemtype")))
                    .details(objectMapper.readValue(rs.getString("details"),
                            objectMapper.getTypeFactory().constructParametricType(Map.class,
                                    String.class, Object.class)))
                    .action(ActivityLog.Action.fromString(rs.getString("action")))
                    .createdAt(rs.getLong("createdat"))
                    .build();
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to parse details object.", e);
        }
    }

    public static RowMapper<DbAggregationResult> aggRowMapper(String key) {
        return ((rs, rowNum) -> DbAggregationResult.builder()
                .key(key.equals("none") ? null : rs.getString(key))
                .count(rs.getLong("ct"))
                .build());
    }
}