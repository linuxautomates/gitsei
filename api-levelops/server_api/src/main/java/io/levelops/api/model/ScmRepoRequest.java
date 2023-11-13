package io.levelops.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ScmRepoRequest.ScmRepoRequestBuilder.class)
public class ScmRepoRequest {

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("repo")
    String repo;

    @JsonProperty("project")
    String project;

    @JsonProperty("scm_params")
    Map<String, Object> scmParams;
}
