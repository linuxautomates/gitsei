package io.levelops.commons.tenant_management.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.faceted_search.models.IndexType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantIndexSnapshot {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("tenant_id")
    private String tenantId; //Used only during read from DB

    @JsonProperty("index_type")
    private IndexType indexType; //Used only during read from DB

    @JsonProperty("index_type_config_id")
    private UUID indexTypeConfigId;

    @JsonProperty("ingested_at")
    private Long ingestedAt;

    @JsonProperty("index_name")
    private String indexName;

    @JsonProperty("priority")
    private Integer priority;

    @JsonProperty("status")
    private JobStatus status;

    @JsonProperty("status_updated_at")
    private Instant statusUpdatedAt;

    @JsonProperty("failed_attempts_count")
    private Integer failedAttemptsCount;

    @JsonProperty("index_exist")
    private Boolean indexExist;

    @JsonProperty("last_refresh_started_at")
    private Instant lastRefreshStartedAt;

    @JsonProperty("last_refreshed_at")
    private Instant lastRefreshedAt;

    @JsonProperty("latest_offsets")
    private Offsets latestOffsets;

    @JsonProperty("heartbeat")
    private Instant heartbeat;

    @JsonProperty("marked_for_deletion")
    private Boolean markedForDeletion;

    @JsonProperty("marked_for_deletion_at")
    private Instant markedForDeletionAt;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}

