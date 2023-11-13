package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabRepository.GitlabRepositoryBuilder.class)
public class GitlabRepository {
    @JsonProperty("id")
    String id;
    @JsonProperty("name")
    String name;
    @JsonProperty("type")
    String type;
    @JsonProperty("path")
    String path;
    @JsonProperty("mode")
    String mode;
}
