package io.levelops.integrations.circleci.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CircleCIProject.CircleCIProjectBuilder.class)
public class CircleCIProject {

    @JsonProperty("reponame")
    String reponame;

    @JsonProperty("username")
    String username;

    @JsonProperty("vcs_type")
    String vcsType;

    @JsonProperty("language")
    String language;

    @JsonProperty("vcs_url")
    String vcsUrl;

    @JsonProperty("default_branch")
    String defaultBranch;

}
