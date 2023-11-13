package io.levelops.commons.databases.models.database.organization;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class TeamMemberDTO {
    UUID id;
    String fullName;
    String email;
    Object ids;
    Object teams;
    Object directManagers;
}
