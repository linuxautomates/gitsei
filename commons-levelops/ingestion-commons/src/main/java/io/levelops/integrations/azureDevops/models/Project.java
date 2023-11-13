package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Project.ProjectBuilder.class)
public class Project {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("url")
    String url;

    @JsonProperty("state")
    String state;

    @JsonProperty("revision")
    int revision;

    @JsonProperty("visibility")
    String visibility;

    @JsonProperty("lastUpdateTime")
    String lastUpdateTime;

    @JsonProperty("abbreviation")
    String abbreviation;

    @JsonProperty("defaultTeamImageUrl")
    String defaultTeamImageUrl;

    @JsonProperty("description")
    String description;

    @JsonProperty("GitEnabled")
    Boolean gitEnabled;

    @JsonProperty("TfvcEnabled")
    Boolean tfvcEnabled;

    @JsonProperty("repositories")
    List<Repository> repositories; //enriched

    @JsonProperty("pipeline")
    List<Pipeline> pipelines; //enriched

    @JsonProperty("build")
    List<Build> builds; //enriched

    @JsonProperty("projectProperty")
    List<ProjectProperty> projectProperty; //enriched

    @JsonProperty("workItem")
    List<WorkItem> workItems; //enriched

    @JsonProperty("organization")
    String organization; //enriched

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ProjectState.ProjectStateBuilder.class)
    public static class ProjectState{

        @JsonProperty("all")
        String all;

        @JsonProperty("createPending")
        String createPending;

        @JsonProperty("deleted")
        String deleted;

        @JsonProperty("deleting")
        String deleting;

        @JsonProperty("new")
        String beingCreated;

        @JsonProperty("unchanged")
        String unchanged;

        @JsonProperty("wellFormed")
        String wellFormed;
    }
}
