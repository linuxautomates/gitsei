package io.levelops.commons.databases.models.database.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Workspace.WorkspaceBuilder.class)
public class Workspace {
    @JsonProperty("id")
    UUID id;
    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;
    @JsonProperty(value = "key")
    private String key;
    @JsonProperty(value = "owner_id")
    private String ownerId;
    @JsonProperty("integration_ids")
    Set<Integer> integrationIds;
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("updated_at")
    Instant updatedAt;
    @JsonProperty("bootstrapped")
    private Boolean bootstrapped;
    @JsonProperty("immutable")
    private Boolean immutable;
}
