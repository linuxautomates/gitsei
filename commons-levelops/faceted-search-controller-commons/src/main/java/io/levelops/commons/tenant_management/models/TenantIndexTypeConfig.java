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
public class TenantIndexTypeConfig {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("tenant_id")
    private String tenantId; //Used only during read from DB

    @JsonProperty("tenant_config_id")
    private Long tenantConfigId;

    @JsonProperty("index_type")
    private IndexType indexType;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("priority")
    private Integer priority;

    @JsonProperty("frequency_in_mins")
    private Long frequencyInMins;

    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("updated_at")
    private Instant updatedAt;
}
