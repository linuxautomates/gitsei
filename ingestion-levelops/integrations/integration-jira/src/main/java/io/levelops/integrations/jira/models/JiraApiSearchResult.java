package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.*;
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
    Integer startAt;
    @JsonProperty("maxResults")
    Integer maxResults;
    @JsonProperty("total")
    Integer total;
    @JsonProperty("issues")
    List<JiraIssue> issues;

}
