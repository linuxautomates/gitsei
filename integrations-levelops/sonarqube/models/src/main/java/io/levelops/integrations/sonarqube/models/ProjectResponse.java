package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@ToString
@JsonDeserialize(builder = ProjectResponse.ProjectResponseBuilder.class)
@AllArgsConstructor
public class ProjectResponse {

    @JsonProperty("paging")
    Paging paging;

    @JsonProperty("components")
    List<Project> projects;

}
