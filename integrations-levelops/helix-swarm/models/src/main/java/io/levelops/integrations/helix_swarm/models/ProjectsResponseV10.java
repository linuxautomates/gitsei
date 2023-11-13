package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ProjectsResponseV10.ProjectsResponseV10Builder.class)
public class ProjectsResponseV10 {
    @JsonProperty("data")
    ProjectsResponseV10Data data;
}

