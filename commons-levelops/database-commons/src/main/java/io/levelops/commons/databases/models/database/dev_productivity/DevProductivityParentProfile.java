package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder(toBuilder = true)
@Value
@JsonDeserialize(builder = DevProductivityParentProfile.DevProductivityParentProfileBuilder.class)
public class DevProductivityParentProfile {
    @JsonProperty("id")
    private final UUID id;
    @JsonProperty("name")
    private final String name;
    @JsonProperty("description")
    private final String description;
    @JsonProperty("default_profile")
    private final Boolean defaultProfile;
    @JsonProperty("predefined_profile")
    private final Boolean predefinedProfile;
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("updated_at")
    Instant updatedAt;
    @JsonProperty("settings")
    Map<String, Object> settings;
    @JsonProperty("effort_investment_profile_id")
    private final UUID effortInvestmentProfileId;
    @JsonProperty("feature_ticket_categories_map")
    private final Map<DevProductivityProfile.FeatureType,List<UUID>> featureTicketCategoriesMap;
    @JsonProperty("associated_ou_ref_ids")
    private final List<String> associatedOURefIds;
    @JsonProperty("ou_trellis_enabled_map")
    private final Map<Integer,Boolean> ouTrellisEnabledMap;
    @JsonProperty("sub_profiles")
    private final List<DevProductivityProfile> subProfiles;
}
