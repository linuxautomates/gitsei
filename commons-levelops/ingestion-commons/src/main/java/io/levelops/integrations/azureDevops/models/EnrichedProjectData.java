package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = EnrichedProjectData.EnrichedProjectDataBuilder.class)
public class EnrichedProjectData {

    @JsonProperty("project")
    Project project;

    @JsonProperty("repository")
    Repository repository;

    @JsonProperty("definition")
    AzureDevopsReleaseDefinition definition;

    @JsonProperty("releases")
    List<AzureDevopsRelease> releases;

    @JsonProperty("pipeline")
    Pipeline pipeline;

    @JsonProperty("runs")
    List<Run> pipelineRuns;

    @JsonProperty("builds")
    List<Build> builds;

    @JsonProperty("commits")
    List<Commit> commits;

    @JsonProperty("pullRequests")
    List<PullRequest> pullRequests;

    @JsonProperty("workItems")
    List<WorkItem> workItems;

    @JsonProperty("workItemHistories")
    List<WorkItemHistory> workItemHistories;

    @JsonProperty("workItemFields")
    List<WorkItemField> workItemFields;

    @JsonProperty("changesets")
    List<ChangeSet> changeSets;

    @JsonProperty("branchs")
    List<Branch> branches;

    @JsonProperty("labels")
    List<Label> labels;

    @JsonProperty("iterations")
    List<Iteration> iterations;

    @JsonProperty("metadata")
    Metadata metadata;

    @JsonProperty("teams")
    List<Team> teams;

    @JsonProperty("tags")
    List<Tag> tags;

    @JsonProperty("codeAreas")
    ClassificationNode codeAreas;
}