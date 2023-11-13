package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityParentProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.MapUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Log4j2
public class DevProductivityProfileConverters {
    public static RowMapper<DevProductivityParentProfile> mapDevProductivityParentProfile(ObjectMapper mapper) {
        return (rs, rowNumber) -> {
            UUID id = (UUID) rs.getObject("id");
            String name = rs.getString("name");
            String description = rs.getString("description");
            Boolean defaultProfile = rs.getObject("dev_productivity_default_mapping_id") != null ? true : false;
            Boolean predefinedProfile = rs.getBoolean("is_predefined");
            UUID effortInvestmentProfileId = (UUID) rs.getObject("ticket_categorization_scheme_id");
            Map<String, Object> settings = ParsingUtils.parseJsonObject(DefaultObjectMapper.get(),
                    "dev_productivity_parent_profile", rs.getString("settings"));
            Map<DevProductivityProfile.FeatureType, List<UUID>> featureTicketCategoriesMap = MapUtils.emptyIfNull(ParsingUtils.parseJsonObject(DefaultObjectMapper.get(),
                    "dev_productivity_parent_profile", rs.getString("feature_ticket_categories_map")))
                    .entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> DevProductivityProfile.FeatureType.fromString(entry.getKey()),
                            entry -> ((List<String>) entry.getValue()).stream()
                                    .map(UUID::fromString)
                                    .collect(Collectors.toList()
                    )));
            List<String> ouRefIds = null;
            if(doesColumnExist("ou_ref_ids", rs)){
                ouRefIds = Arrays.asList((Integer[]) rs.getArray("ou_ref_ids").getArray()).stream()
                        .map(String::valueOf).collect(Collectors.toList());
                if(ouRefIds.contains(null))
                    ouRefIds = null;
            }
            var ouTrellisEnabledMap = doesColumnExist("ou_ref_ids", rs) ? ParsingUtils.parseMap(mapper, "ou_trellis_enabled_map", Integer.class, Boolean.class, rs.getString("ou_trellis_enabled_map")) : null;
            Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));
            Instant updatedAt = DateUtils.toInstant(rs.getTimestamp("updated_at"));
            DevProductivityParentProfile devProductivityParentProfile = DevProductivityParentProfile.builder()
                    .id(id)
                    .name(name)
                    .description(description)
                    .defaultProfile(defaultProfile)
                    .predefinedProfile(predefinedProfile)
                    .effortInvestmentProfileId(effortInvestmentProfileId)
                    .featureTicketCategoriesMap(featureTicketCategoriesMap)
                    .associatedOURefIds(ouRefIds)
                    .ouTrellisEnabledMap(ouTrellisEnabledMap)
                    .settings(settings)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();
            return devProductivityParentProfile;
        };
    }

    public static RowMapper<DevProductivityProfile> mapDevProductivityProfile(ObjectMapper mapper) {
        return (rs, rowNumber) -> {
            UUID id = (UUID) rs.getObject("id");
            String name = rs.getString("name");
            String description = rs.getString("description");
            Boolean defaultProfile = doesColumnExist("dev_productivity_default_mapping_id",rs) && rs.getObject("dev_productivity_default_mapping_id") != null ? true : false;
            Boolean predefinedProfile = doesColumnExist("is_predefined",rs) ? rs.getBoolean("is_predefined") : false;
            UUID effortInvestmentProfileId = doesColumnExist("is_predefined",rs) ? (UUID) rs.getObject("ticket_categorization_scheme_id") : null;
            Map<String, Object> settings = ParsingUtils.parseJsonObject(DefaultObjectMapper.get(),
                    "dev_productivity_profile", rs.getString("settings"));
            List<DevProductivityProfile.MatchingCriteria> matchingCriteria = null;
            if(doesColumnExist("matching_criteria",rs)){
                try {
                    matchingCriteria = mapper.readValue(rs.getString("matching_criteria"), mapper.getTypeFactory().constructCollectionType(List.class, DevProductivityProfile.MatchingCriteria.class));
                } catch (JsonProcessingException e) {
                    log.error("Error parsing matching criteria");
                    throw new SQLException("could not de-serialize matching criteria");
                }
            }
            List<String> ouRefIds = null;
            if(doesColumnExist("ou_ref_ids", rs)){
                ouRefIds = Arrays.asList((Integer[]) rs.getArray("ou_ref_ids").getArray()).stream()
                        .map(String::valueOf).collect(Collectors.toList());
                if(ouRefIds.contains(null))
                    ouRefIds = null;
            }
            Boolean enabled = doesColumnExist("enabled",rs) ? rs.getBoolean("enabled") : null;
            Integer order = doesColumnExist("index",rs) ? rs.getInt("index") : 0;
            Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));
            Instant updatedAt = DateUtils.toInstant(rs.getTimestamp("updated_at"));
            DevProductivityProfile devProductivityProfile = DevProductivityProfile.builder()
                    .id(id)
                    .name(name)
                    .description(description)
                    .matchingCriteria(matchingCriteria)
                    .enabled(enabled)
                    .order(order)
                    .defaultProfile(defaultProfile)
                    .predefinedProfile(predefinedProfile)
                    .effortInvestmentProfileId(effortInvestmentProfileId)
                    .associatedOURefIds(ouRefIds)
                    .settings(settings)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();
            return devProductivityProfile;
        };
    }

    public static RowMapper<DevProductivityProfile.Section> mapDevProductivitySection(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> {
            DevProductivityProfile.Section section = null;
            try {
                section = objectMapper.readValue(rs.getString("body"), DevProductivityProfile.Section.class);
            } catch (JsonProcessingException e) {
                throw new SQLException("could not de-serialize section");
            }
            section = section.toBuilder().id((UUID)rs.getObject("id")).build();
            return section;
        };
    }

    public static RowMapper<DevProductivityProfile.Feature> mapDevProductivityFeature(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> {
            DevProductivityProfile.Feature feature = null;
            try {
                feature = objectMapper.readValue(rs.getString("body"), DevProductivityProfile.Feature.class);
            } catch (JsonProcessingException e) {
                throw new SQLException("could not de-serialize feature");
            }
            feature = feature.toBuilder().id((UUID)rs.getObject("id")).build();
            return feature;
        };
    }

    public static RowMapper<Integer> mapAssociatedOUs(ObjectMapper objectMapper) {
        return(rs,rowNumber) -> {
            return rs.getInt("ou_ref_id");
        };
    }

    public static RowMapper<UUID> mapAssociatedProfile(ObjectMapper objectMapper) {
        return(rs,rowNumber) -> {
            return (UUID)rs.getObject("dev_productivity_profile_id");
        };
    }
    public static boolean doesColumnExist(String columnName, ResultSet rs) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
