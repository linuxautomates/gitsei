package io.levelops.integrations.sonarqube.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Location.LocationBuilder.class)
public class Location {

    @JsonProperty("component")
    String component;

    @JsonProperty("textRange")
    TextRange textRange;

    @JsonProperty("msg")
    String msg;
}