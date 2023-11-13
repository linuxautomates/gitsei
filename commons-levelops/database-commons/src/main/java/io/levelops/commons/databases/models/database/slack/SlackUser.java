package io.levelops.commons.databases.models.database.slack;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackUser.SlackUserBuilder.class)
public class SlackUser {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("team_id")
    private final String teamId;

    @JsonProperty("user_id")
    private final String userId;

    @JsonProperty("real_name_normalized")
    private final String realNameNormalized;

    @JsonProperty("username")
    private final String username;

    @JsonProperty("email")
    private final String email;

    @JsonProperty("created_at")
    private final Instant createdAt;

    @JsonProperty("updated_at")
    private final Instant updatedAt;
}
