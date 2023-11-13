package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = IntegrationSecretMapping.IntegrationSecretMappingBuilder.class)
public class IntegrationSecretMapping {

    @JsonProperty("id")
    String id;
    @JsonProperty("integration_id")
    String integrationId;
    @JsonProperty("name")
    String name;
    @JsonProperty("sm_config_id")
    String smConfigId;
    @JsonProperty("sm_key")
    String smKey;
    @JsonProperty("updated_at")
    Instant updatedAt;
    @JsonProperty("created_at")
    Instant createdAt;

}
