package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = FeatureBreakDown.FeatureBreakDownBuilder.class)
public class FeatureBreakDown {

    @JsonProperty("org_user_id")
    private UUID orgUserId;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("breakdown_type")
    private  BreakDownType breakDownType;

    @JsonProperty("records")
    private List<? extends Object> records;

    @JsonProperty("count")
    private Long count;
}
