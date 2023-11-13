package io.levelops.integrations.github.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubContributor.GithubContributorBuilder.class)
public class GithubContributor {
    @JsonProperty("login")
    String login;

    @JsonProperty("url")
    String url;
}
