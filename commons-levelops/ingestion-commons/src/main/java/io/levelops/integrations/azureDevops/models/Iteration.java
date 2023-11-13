package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Iteration.IterationBuilder.class)
public class Iteration {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("path")
    String path;

    @JsonProperty("attributes")
    Attributes attributes;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Iteration.Attributes.AttributesBuilder.class)
    public static class Attributes {

        @JsonProperty("startDate")
        String startDate;

        @JsonProperty("finishDate")
        String finishDate;

        @JsonProperty("timeFrame")
        String timeFrame;
    }

    @JsonProperty("url")
    String url;

    @JsonProperty("_links")
    Object links;

}
