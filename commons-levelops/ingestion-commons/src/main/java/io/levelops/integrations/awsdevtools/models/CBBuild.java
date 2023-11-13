package io.levelops.integrations.awsdevtools.models;

import com.amazonaws.services.codebuild.model.Build;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CBBuild.CBBuildBuilder.class)
public class CBBuild {

    @NonNull
    @JsonProperty("build")
    Build build;

    @JsonProperty("projectArn")
    String projectArn;

    @JsonProperty("region")
    String region;

    @JsonProperty("enrichedReports")
    List<CBReport> reports;
}
