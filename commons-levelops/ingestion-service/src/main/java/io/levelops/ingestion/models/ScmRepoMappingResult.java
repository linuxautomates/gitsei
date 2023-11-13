package io.levelops.ingestion.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ScmRepoMappingResult.ScmRepoMappingResultBuilder.class)
public class ScmRepoMappingResult implements ControllerIngestionResult {
    @JsonProperty("fetched_at")
    Instant fetchedAt;

    @JsonProperty("mapped_repos")
    List<String> mappedRepos;
}