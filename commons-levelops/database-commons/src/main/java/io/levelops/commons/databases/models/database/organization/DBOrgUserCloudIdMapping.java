package io.levelops.commons.databases.models.database.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.scm.DbScmUser.MappingStatus;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DBOrgUserCloudIdMapping.DBOrgUserCloudIdMappingBuilder.class)
public class DBOrgUserCloudIdMapping {
    @JsonProperty("id")
    UUID id;

    @JsonProperty("org_user_id")
    UUID orgUserId;

    @JsonProperty("integration_user_id")
    UUID integrationUserId;

    @JsonProperty("mapping_status")
    MappingStatus mappingStatus;
}
