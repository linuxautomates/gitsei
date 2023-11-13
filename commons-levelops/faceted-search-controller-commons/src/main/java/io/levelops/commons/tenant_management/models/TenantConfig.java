package io.levelops.commons.tenant_management.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantConfig {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("priority")
    private Integer priority;

    @JsonProperty("marked_for_deletion")
    private Boolean markedForDeletion;

    @JsonProperty("marked_for_deletion_at")
    private Instant markedForDeletionAt;

    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("updated_at")
    private Instant updatedAt;
}

