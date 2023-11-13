package io.levelops.commons.databases.models.database.velocity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = VelocityConfig.VelocityConfigBuilder.class)
public class VelocityConfig {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("default_config")
    private final Boolean defaultConfig;

    @JsonProperty("config")
    private final VelocityConfigDTO config;

    @JsonProperty("cicd_job_ids")
    private final List<UUID> cicdJobIds; //This goes from input to save to db

    @JsonProperty("cicd_job_id_name_mappings")
    private final Map<UUID, String> cicdJobIdNameMappings; //This will be read from db & sent to service to be set in VelocityConfigDTO

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("is_new")
    private Boolean isNew;

}
