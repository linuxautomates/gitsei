package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Run.RunBuilder.class)
public class Run {

    @JsonProperty("_links")
    Link link;
    @JsonProperty("pipeline")
    Pipeline pipeline;

    @JsonProperty("state")
    String state;

    @JsonProperty("result")
    String result;

    @JsonProperty("createdDate")
    String createdDate;

    @JsonProperty("finishedDate")
    String finishedDate;

    @JsonProperty("url")
    String url;

    @JsonProperty("resources")
    Resource resources;

    @JsonProperty("id")
    int id;

    @JsonProperty("commit_ids")
    List<String> commitIds;

    @JsonProperty("name")
    String name;

    @JsonProperty("variables")
    Map<String, Configuration.Variable> variables;

    @JsonProperty("stages")
    List<AzureDevopsPipelineRunStageStep> stages;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Resource.ResourceBuilder.class)
    public static class Resource {

        @JsonProperty("repositories")
        Map<String,Repositories> repositories;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = Repositories.RepositoriesBuilder.class)
        public static class Repositories{

            @JsonProperty("self")
            Self self;

            @JsonProperty("refName")
            String refName;

            @JsonProperty("version")
            String version;

            @Value
            @Builder(toBuilder = true)
            @JsonDeserialize(builder = Self.SelfBuilder.class)
            public static class Self {
                @JsonProperty("repository")
                Configuration.Repository repository;
            }
        }
    }
}
