package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = CiCdScmMapping.CiCdScmMappingBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CiCdScmMapping {
    private UUID id;
    private UUID jobRunId;
    private UUID commitId;
    private String source;
    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("updated_at")
    private Instant updatedAt;
}