package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgUserDevProdProfileMappings.OrgUserDevProdProfileMappingsBuilder.class)
public class OrgUserDevProdProfileMappings {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("org_user_ref_id")
    private final Integer orgUserRefId;

    @JsonProperty("dev_productivity_parent_profile_id")
    private final UUID devProductivityParentProfileId;

    @JsonProperty("dev_productivity_profile_id")
    private final UUID devProductivityProfileId;

    @JsonProperty("created_at")
    private final Instant createdAt;

    @JsonProperty("updated_at")
    private final Instant updatedAt;
}
