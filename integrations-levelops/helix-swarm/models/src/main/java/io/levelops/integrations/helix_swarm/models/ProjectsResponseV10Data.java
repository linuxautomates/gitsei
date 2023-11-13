package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ProjectsResponseV10Data.ProjectsResponseV10DataBuilder.class)
public class ProjectsResponseV10Data {
    @JsonProperty("projects")
    List<ProjectsResponseV10ProjectInfo> projects;
}
