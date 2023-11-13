package io.levelops.commons.report_models.ba;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BAProfile.BAProfileBuilder.class)
public class BAProfile {
    @JsonProperty("id")
    UUID id;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("default_profile")
    Boolean defaultProfile;

    @JsonProperty("categories")
    List<BACategory> categories;

    @JsonProperty("created_at")
    Instant createdAt;

    @JsonProperty("updated_at")
    Instant updatedAt;
}
