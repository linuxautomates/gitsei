package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CICDInstance.CICDInstanceBuilder.class)
public class CICDInstance {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("url")
    private final String url;

    @JsonProperty("integration_id")
    private final String integrationId;

    @JsonProperty("type")
    private final String type;

    @JsonProperty("config")
    private CiCdInstanceConfig config;

    @JsonProperty("config_updated_at")
    private Instant configUpdatedAt;

    @JsonProperty("details")
    private CiCdInstanceDetails details;

    @JsonProperty("last_hb_at")
    private Instant lastHeartbeatAt;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}
