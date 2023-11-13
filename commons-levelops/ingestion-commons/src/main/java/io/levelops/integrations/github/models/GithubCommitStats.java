package io.levelops.integrations.github.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubCommitStats.GithubCommitStatsBuilder.class)
public class GithubCommitStats implements Serializable {
    @JsonProperty("additions")
    private int additions;

    @JsonProperty("deletions")
    private int deletions;

    @JsonProperty("total")
    private int total;
}
