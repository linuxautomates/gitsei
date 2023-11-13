package io.levelops.integrations.awsdevtools.models;

import com.amazonaws.services.codebuild.model.Project;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CBProject.CBProjectBuilder.class)
public class CBProject {

    @NonNull
    @JsonProperty("project")
    Project project;

    @JsonProperty("region")
    String region;
}
