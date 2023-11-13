package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubTag.GithubTagBuilder.class)
public class GithubTag implements Serializable {

    @JsonProperty("name")
    String name;

    @JsonProperty("commit")
    GithubCommitInfo commit;

    @JsonProperty("node_id")
    String nodeId;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GithubTag.GithubCommitInfo.GithubCommitInfoBuilder.class)
    public static class GithubCommitInfo {

        @JsonProperty("sha")
        String sha;

        @JsonProperty("url")
        String url;
    }
}