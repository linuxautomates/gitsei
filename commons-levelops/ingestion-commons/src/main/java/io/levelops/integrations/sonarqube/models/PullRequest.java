package io.levelops.integrations.sonarqube.models;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PullRequest.PullRequestBuilder.class)
public class PullRequest {
    @JsonProperty("key")
    String key;

    @JsonProperty("title")
    String title;

    @JsonProperty("branch")
    String branch;

    @JsonProperty("base")
    String base;

    @JsonProperty("status")
    Status status;

    @JsonProperty("analysisDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
    Date analysisDate;

    @JsonProperty("url")
    String url;

    @JsonProperty("target")
    String target;

    @JsonProperty("measures")
    List<Measure> measures;

    @JsonProperty("issues")
    List<Issue> issues;  //enriched
}
