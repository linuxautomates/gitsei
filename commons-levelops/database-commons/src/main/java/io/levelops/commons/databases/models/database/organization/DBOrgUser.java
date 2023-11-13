package io.levelops.commons.databases.models.database.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class DBOrgUser extends OrgUserId {
    @JsonProperty("custom_fields")
    Map<String, Object> customFields;
    @JsonProperty("versions")
    Set<Integer> versions;
    @JsonProperty("active")
    boolean active;
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("updated_at")
    Instant updatedAt;
    
    Set<LoginId> ids;
    Object teams;
    Object directManagers;

    @Value
    @SuperBuilder(toBuilder = true)
    public static class LoginId{
        int integrationId;
        String integrationType;
        String username;
        String cloudId;
    }
}
