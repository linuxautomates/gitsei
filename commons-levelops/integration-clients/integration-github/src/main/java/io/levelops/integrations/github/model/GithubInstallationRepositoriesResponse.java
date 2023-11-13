package io.levelops.integrations.github.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.github.models.GithubRepository;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubInstallationRepositoriesResponse.GithubInstallationRepositoriesResponseBuilder.class)
public class GithubInstallationRepositoriesResponse {

    @JsonProperty("link_header")
    String linkHeader;

    @JsonProperty("total_count")
    Integer totalCount;

    @JsonProperty("repository_selection")
    String repository_selection; // "selected" or "all"

    @JsonProperty("repositories")
    List<GithubRepository> repositories;

}
