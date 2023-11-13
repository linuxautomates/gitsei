package io.levelops.integrations.snyk.models.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.snyk.models.SnykOrg;
import io.levelops.integrations.snyk.models.SnykProject;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SnykApiListProjectsResponse.SnykApiListProjectsResponseBuilder.class)
public class SnykApiListProjectsResponse {
    //https://snyk.docs.apiary.io/#reference/projects/all-projects/list-all-projects
    @JsonProperty("org")
    private final SnykOrg org;
    @JsonProperty("projects")
    private final List<SnykProject> projects;
}