package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ProjectResponse.ProjectResponseBuilder.class)
public class ProjectResponse {

    @JsonProperty("value")
    List<Project> projects;

    String continuationToken;

}
