package io.levelops.integrations.awsdevtools.models;

import com.amazonaws.services.codebuild.model.BuildBatch;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CBBuildBatch.CBBuildBatchBuilder.class)
public class CBBuildBatch {

    @NonNull
    @JsonProperty("buildBatch")
    BuildBatch buildBatch;

    @JsonProperty("projectArn")
    String projectArn;

    @JsonProperty("region")
    String region;
}
