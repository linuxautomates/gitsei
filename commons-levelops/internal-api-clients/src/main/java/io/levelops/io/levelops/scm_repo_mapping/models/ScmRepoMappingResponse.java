package io.levelops.io.levelops.scm_repo_mapping.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.models.ScmRepoMappingResult;
import io.levelops.integrations.github.models.GithubCommit;
import lombok.Builder;
import lombok.Value;

import javax.annotation.Nullable;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ScmRepoMappingResponse.ScmRepoMappingResponseBuilder.class)
public class ScmRepoMappingResponse {
    @JsonProperty("job_id")
    String jobId;

    @Nullable
    @JsonProperty("result")
    ScmRepoMappingResult result;
}