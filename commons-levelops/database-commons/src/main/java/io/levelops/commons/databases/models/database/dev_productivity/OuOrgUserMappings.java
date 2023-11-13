package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OuOrgUserMappings.OuOrgUserMappingsBuilder.class)
public class OuOrgUserMappings {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("ou_id")
    private final UUID ouId;

    @JsonProperty("ou_ref_id")
    private final Integer ouRefId;

    @JsonProperty("org_user_id")
    private final UUID orgUserId;

    @JsonProperty("org_user_ref_id")
    private final Integer orgUserRefId;

    @JsonProperty("created_at")
    private final Instant createdAt;

    @JsonProperty("updated_at")
    private final Instant updatedAt;
}
