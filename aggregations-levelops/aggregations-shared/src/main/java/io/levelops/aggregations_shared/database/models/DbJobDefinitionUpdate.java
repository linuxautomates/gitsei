package io.levelops.aggregations_shared.database.models;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class DbJobDefinitionUpdate {
    Boolean isActive;
    Instant lastIterationTs;
    WhereClause whereClause;
    Map<String, Object> metadata;

    @Value
    @Builder(toBuilder = true)
    public static class WhereClause {
        UUID id;
        String tenantId;
        String integrationId;
    }
}
