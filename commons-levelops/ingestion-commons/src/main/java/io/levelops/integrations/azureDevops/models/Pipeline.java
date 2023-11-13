package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Pipeline.PipelineBuilder.class)
public class Pipeline {

    @JsonProperty("_links")
    Link links;

    @JsonProperty("configuration")
    Configuration configuration;

    @JsonProperty("url")
    String url;

    @JsonProperty("id")
    int id;

    @JsonProperty("revision")
    int revision;

    @JsonProperty("name")
    String name;

    @JsonProperty("folder")
    String folder;

    @JsonProperty("project")
    Project project;

    @JsonProperty("runs")
    List<Run> runs; //enriched
}
