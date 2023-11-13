package io.levelops.commons.databases.converters;

import io.levelops.commons.dates.DateUtils;
import io.propelo.commons.generic_events.models.Component;
import io.propelo.commons.generic_events.models.GenericEventRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

@Log4j2
public class GenericEventConverters {
    public static RowMapper<GenericEventRequest> rowMapper() {
        return (rs, rowNumber) -> {
            return GenericEventRequest.builder()
                    .id((UUID)rs.getObject("id"))
                    .component(Component.fromString(rs.getString("component")))
                    .key(rs.getString("key"))
                    .secondaryKey(rs.getString("secondary_key"))
                    .eventType(rs.getString("event_type"))
                    .eventTime(DateUtils.toInstant(rs.getTimestamp("event_time")).getEpochSecond())
                    .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                    .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                    .build();
        };
    }
    public static RowMapper<Integer> countMapper() {
        return (rs, rowNumber) -> {
            return rs.getInt("count");
        };
    }
}
