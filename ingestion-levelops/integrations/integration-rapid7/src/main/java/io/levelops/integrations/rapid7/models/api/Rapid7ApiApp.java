package io.levelops.integrations.rapid7.models.api;

import com.fasterxml.jackson.annotation.*;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Rapid7ApiApp.Rapid7ApiAppBuilder.class)
public class Rapid7ApiApp {

    @JsonProperty("id")
    String id;
    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;
    @JsonProperty("links")
    List<Link> links;


    public static class Link {
        @JsonProperty("href")
        String href;
        @JsonProperty("rel")
        String rel;
    }
}
