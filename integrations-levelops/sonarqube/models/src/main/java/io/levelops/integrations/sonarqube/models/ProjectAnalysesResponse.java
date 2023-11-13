package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ProjectAnalysesResponse.ProjectAnalysesResponseBuilder.class)
public class ProjectAnalysesResponse {
    @JsonProperty("paging")
    Paging paging;

    @JsonProperty("analyses")
    List<Analyse> analyses;
}
