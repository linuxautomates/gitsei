package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraApiSearchResult.JiraApiSearchResultBuilder.class)
public class JiraApiSearchResult {

    @JsonProperty("expand")
    String expand;
    @JsonProperty("startAt")
    Long startAt;
    @JsonProperty("maxResults")
    Long maxResults;
    @JsonProperty("total")
    Long total;
    @JsonProperty("issues")
    List<JiraIssue> issues;

}
