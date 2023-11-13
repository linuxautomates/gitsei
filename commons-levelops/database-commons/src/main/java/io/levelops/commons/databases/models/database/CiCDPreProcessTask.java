package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CiCDPreProcessTask {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("tenant_id")
    private String tenantId;
    @JsonProperty("status")
    private String status;
    @JsonProperty("metadata")
    private String metaData;
    @JsonProperty("attempts_count")
    private Integer attemptCount; //increment everytime task is picked up
    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("updated_at")
    private Instant updatedAt;
    @JsonProperty("status_changed_at")
    private Instant statusChangedAt;
}
