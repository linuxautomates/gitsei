package io.levelops.aggregations_shared.database.models;

import io.levelops.commons.etl.models.JobType;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class DbJobDefinitionFilter {
    @Nullable
    List<UUID> ids;

    @Nullable
    Boolean isActive;

    @Nullable
    Pair<String, String> tenantIdIntegrationIdPair;

    @Nullable
    List<String> tenantIds;

    @Nullable
    List<JobType> jobTypes;
}
