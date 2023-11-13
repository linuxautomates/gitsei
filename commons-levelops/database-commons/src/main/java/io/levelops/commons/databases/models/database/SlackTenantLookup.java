package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackTenantLookup.SlackTenantLookupBuilder.class)
public class SlackTenantLookup {
    @JsonProperty("id")
    private final UUID id;
    @JsonProperty("team_id")
    private final String teamId;
    @JsonProperty("tenant_name")
    private final String tenantName;
    @JsonProperty("created_at")
    private final Instant createdAt;
}
