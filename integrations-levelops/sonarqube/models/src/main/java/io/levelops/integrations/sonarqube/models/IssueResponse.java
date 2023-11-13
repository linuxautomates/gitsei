package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@ToString
@JsonDeserialize(builder = IssueResponse.IssueResponseBuilder.class)
public class IssueResponse {

    @JsonProperty("total")
    Long total;

    @JsonProperty("p")
    Long pageIndex;

    @JsonProperty("ps")
    Long pageSize;

    @JsonProperty("paging")
    Paging paging;

    @JsonProperty("effortTotal")
    Long effortTotal;

    @JsonProperty("debtTotal")
    Long debtTotal;

    @JsonProperty("issues")
    List<Issue> issues;

    @JsonProperty("components")
    List<Project> projects;

    @JsonProperty("facets")
    List<String> facets;

    @JsonProperty("rules")
    List<Rule> rules;           //additional fields

    @JsonProperty("users")
    List<User> users;            //additional fields
}