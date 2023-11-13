package io.levelops.commons.databases.models.database.organization;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class TeamDTO {
    UUID id;
    String name;
    String description;
    Object managers;
    Object members;
}
