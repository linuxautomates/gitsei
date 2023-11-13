package io.levelops.commons.databases.models.database.velocity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgProfile.OrgProfileBuilder.class)
public class OrgProfile {

    @JsonProperty("id")
    UUID id;

    @JsonProperty("ou_ref_id")
    Integer ouRefId;

    @JsonProperty("profile_id")
    UUID profileId;

    @JsonProperty("profile_type")
    ProfileType profileType;

    @JsonProperty("is_completed")
    Boolean isCompleted;

    public enum ProfileType {
        WORKFLOW,
        TRELLIS,
        ALIGNMENT
    }
}