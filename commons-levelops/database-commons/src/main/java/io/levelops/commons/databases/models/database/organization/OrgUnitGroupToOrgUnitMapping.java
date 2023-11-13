package io.levelops.commons.databases.models.database.organization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgUnitGroupToOrgUnitMapping.OrgUnitGroupToOrgUnitMappingBuilder.class)
public class OrgUnitGroupToOrgUnitMapping {

    @JsonIgnore
    UUID id;

    @JsonProperty("ou_ref_id")
    String ouRefId;

    @JsonProperty("ou_id")
    UUID orgUnitId;

    @JsonProperty("ou_group_id")
    UUID orgUnitGroupId;
}
