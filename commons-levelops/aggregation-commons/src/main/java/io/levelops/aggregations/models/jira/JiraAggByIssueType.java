package io.levelops.aggregations.models.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Setter
@Getter
@ToString
@NoArgsConstructor
public class JiraAggByIssueType {
    @JsonProperty("with_documentation")
    private Integer wDoc = 0;
    @JsonProperty("with_storypoints")
    private Integer wStorypoints = 0;
    @JsonProperty("without_storypoints")
    private Integer wOStorypoints = 0;
    @JsonProperty("large_issues")
    private Integer largeIssues = 0;
    @JsonProperty("med_issues")
    private Integer mediumIssues = 0;
    @JsonProperty("small_issues")
    private Integer smallIssues = 0;
}
