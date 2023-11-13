package io.levelops.commons.databases.models.database.organization;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgUnitCategory.OrgUnitCategoryBuilder.class)
public class OrgUnitCategory {

    @JsonProperty("id")
    UUID id;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("root_ou_name")
    String rootOuName;

    @JsonProperty("root_ou_id")
    UUID rootOuId;

    @JsonProperty("root_ou_ref_id")
    Integer rootOuRefId;

    @JsonProperty("is_predefined")
    Boolean isPredefined;

    @JsonProperty("enabled")
    Boolean enabled;

    @JsonProperty("workspace_id")
    Integer workspaceId;

    @JsonProperty("created_at")
    Instant createdAt;

    @JsonProperty("updated_at")
    Instant updatedAt;

    @JsonProperty("count_of_ous")
    Integer ousCount;

    @JsonProperty("ou_ref_ids")
    List<Integer> ouRefIds;

}
