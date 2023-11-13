package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.report_models.ba.BACategory;
import io.levelops.commons.report_models.ba.BAProfile;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Log4j2
public class BAProfileConverters {
    public static RowMapper<BAProfile> mapProfile(ObjectMapper mapper) {
        return (rs, rowNumber) -> {
            UUID id = (UUID) rs.getObject("id");
            String name = rs.getString("name");
            String description = rs.getString("description");
            Boolean defaultProfile = rs.getBoolean("default_profile");

            List<BACategory> categories = null;
            try {
                categories = mapper.readValue(rs.getString("categories"),
                        mapper.getTypeFactory().constructCollectionLikeType(List.class, BACategory.class));
            } catch (JsonProcessingException e) {
                throw new SQLException("Failed to deserialize velocity config with id=" + id.toString());
            }
            Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));
            Instant updatedAt = DateUtils.toInstant(rs.getTimestamp("updated_at"));
            BAProfile baProfile = BAProfile.builder()
                    .id(id)
                    .name(name).description(description)
                    .defaultProfile(defaultProfile)
                    .categories(categories)
                    .createdAt(createdAt).updatedAt(updatedAt)
                    .build();
            return baProfile;
        };
    }
}
