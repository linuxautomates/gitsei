package io.levelops.integrations.circleci.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.levelops.ingestion.data.EntityWithLogs;
import io.levelops.ingestion.data.LogWithMetadata;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class CircleCIBuildWithLogs implements EntityWithLogs<CircleCIBuild> {

    CircleCIBuild data;

    @JsonIgnore
    List<LogWithMetadata> logWithMetadata;

}
