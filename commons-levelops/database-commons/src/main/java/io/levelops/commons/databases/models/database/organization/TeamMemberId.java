package io.levelops.commons.databases.models.database.organization;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class TeamMemberId {
    String integrationUserId;
    String teamMemberId;
    String integrationId;
    UUID id;
}
