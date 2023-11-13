package io.levelops.commons.databases.models.database.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DBTeam.DBTeamBuilderImpl.class)
public class DBTeam {
    @JsonProperty("id")
    UUID id;
    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;
    @JsonProperty("managers")
    Set<TeamMemberId> managers;
    @JsonProperty("members")
    Set<TeamMemberId> members;

    @Value
    @Builder(toBuilder = true)
    public static class TeamMemberId {
        @JsonProperty("id")
        UUID id;
        @JsonProperty("full_name")
        String fullName;
        @JsonProperty("email")
        String email;
    }

    @JsonPOJOBuilder(withPrefix = "")
    static final class DBTeamBuilderImpl extends DBTeamBuilder {}
}
