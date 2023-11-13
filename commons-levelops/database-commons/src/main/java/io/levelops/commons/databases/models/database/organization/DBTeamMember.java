package io.levelops.commons.databases.models.database.organization;

import lombok.Builder;
import lombok.Value;

import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class DBTeamMember {
    UUID id;
    String fullName;
    String email;
    Set<LoginId> ids;
    Object teams;
    Object directManagers;

    @Value
    @Builder(toBuilder = true)
    public static class LoginId{
        int integrationId;
        String integrationType;
        String username;
        Object cloudId;
    }
}
