package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Event.EventBuilder.class)
public class Event {

    @JsonProperty("key")
    String key;

    @JsonProperty("category")
    String category;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("qualityGate")
    QualityGate qualityGate;

}