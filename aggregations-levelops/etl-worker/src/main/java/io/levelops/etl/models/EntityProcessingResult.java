package io.levelops.etl.models;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class EntityProcessingResult {
    Integer totalEntities;
    Integer successfulEntities;
    Integer failedEntities;
    Integer processedCount;
}
