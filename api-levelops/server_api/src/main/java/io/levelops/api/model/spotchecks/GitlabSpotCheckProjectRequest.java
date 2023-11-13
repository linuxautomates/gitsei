package io.levelops.api.model.spotchecks;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabSpotCheckProjectRequest.GitlabSpotCheckProjectRequestBuilder.class)
public class GitlabSpotCheckProjectRequest {
    @JsonProperty("integration_id")
    private final Integer integrationId;
    @JsonProperty("project_name")
    private final String projectName;
    @JsonProperty("from")
    private final String from;
    @JsonProperty("to")
    private final String to;
    @JsonProperty("limit")
    private final Integer limit;
}
