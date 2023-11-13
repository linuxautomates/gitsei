package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbScmRepoAgg {
    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("num_commits")
    Integer numCommits;

    @JsonProperty("num_prs")
    Integer numPrs;

    @JsonProperty("num_additions")
    Integer numAdditions;

    @JsonProperty("num_deletions")
    Integer numDeletions;

    @JsonProperty("num_changes")
    Integer numChanges;

    @JsonProperty("num_jira_issues")
    Integer numJiraIssues;

    @JsonProperty("num_workitems")
    Integer numWorkitems;
}
