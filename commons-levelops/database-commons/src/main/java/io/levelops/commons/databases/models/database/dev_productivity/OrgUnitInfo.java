package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgUnitInfo.OrgUnitInfoBuilder.class)
public class OrgUnitInfo {

    @JsonProperty("ou_id")
    private UUID ouId;

    @JsonProperty("ou_name")
    private String ouName;

    @JsonProperty("ou_ref_id")
    private Integer ouRefId;

    @JsonProperty("integration_id")
    private Integer integrationId;
}
