package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraApiSearchQuery.JiraApiSearchQueryBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JiraApiSearchQuery {
    @JsonProperty("expand")
    Set<String> expand;
    @JsonProperty("jql")
    String jql;
    @JsonProperty("maxResults")
    Integer maxResults; // max=100, default=50
    @JsonProperty("fieldsByKeys")
    Boolean fieldsByKeys;
    @JsonProperty("fields")
    Set<String> fields;
    @JsonProperty("startAt")
    Integer startAt;
}
