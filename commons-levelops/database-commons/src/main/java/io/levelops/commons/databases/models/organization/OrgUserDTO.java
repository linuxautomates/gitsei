package io.levelops.commons.databases.models.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgUserDTO.OrgUserDTOBuilder.class)
public class OrgUserDTO {
    @JsonProperty("id")
    String id;
    @JsonProperty("org_uuid")
    UUID orgUUID;
    @JsonProperty("full_name")
    String fullName;
    @JsonProperty("email")
    String email;
    @JsonProperty("integration_user_ids")
    Set<IntegrationUserId> integrationUserIds;
    @JsonProperty("additional_fields")
    Map<String, Object> additionalFields;
    @JsonProperty("version")
    String version;
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("updated_at")
    Instant updatedAt;


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = IntegrationUserId.IntegrationUserIdBuilder.class)
    public static class IntegrationUserId {
        @JsonProperty("integration_id")
        String integrationId;
        @JsonProperty("user_id")
        String userId;
    }
}
