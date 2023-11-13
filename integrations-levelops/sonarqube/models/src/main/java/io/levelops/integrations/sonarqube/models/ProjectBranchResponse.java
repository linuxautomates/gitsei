package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ProjectBranchResponse.ProjectBranchResponseBuilder.class)
public class ProjectBranchResponse {

    @JsonProperty("branches")
    List<Branch> branches;
}
