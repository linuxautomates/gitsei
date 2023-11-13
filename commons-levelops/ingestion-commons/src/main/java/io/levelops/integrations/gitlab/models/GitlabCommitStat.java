package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabCommitStat.GitlabCommitStatBuilder.class)
public class GitlabCommitStat {
    @JsonProperty("additions")
    int additions;
    @JsonProperty("deletions")
    int deletions;
    @JsonProperty("total")
    int total;
}
