package io.levelops.commons.databases.models.database.organization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;
import org.apache.commons.lang3.EnumUtils;

@Value
@Builder(toBuilder = true)
public class OrgVersion {
    @JsonProperty("id")
    UUID id;
    @JsonProperty("type")
    OrgAssetType type;
    @JsonProperty("version")
    int version;
    @JsonProperty("active")
    boolean active;
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("updated_at")
    Instant updatedAt;

    public static enum OrgAssetType{
        USER,
        UNIT;

        @JsonCreator
        @Nullable
        public static OrgAssetType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(OrgAssetType.class, value);
        }
    }
}
