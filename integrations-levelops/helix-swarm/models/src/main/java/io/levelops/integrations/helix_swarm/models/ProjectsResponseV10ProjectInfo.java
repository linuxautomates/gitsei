package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ProjectsResponseV10ProjectInfo.ProjectsResponseV10ProjectInfoBuilder.class)
public class ProjectsResponseV10ProjectInfo {
    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("deleted")
    Boolean deleted;

    @JsonProperty("private")
    Boolean isPrivate;

    @JsonProperty("retainDefaultReviewers")
    Boolean retainDefaultReviewers;
}
