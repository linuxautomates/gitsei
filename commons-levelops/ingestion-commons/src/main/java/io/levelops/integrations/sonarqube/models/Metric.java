package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Metric.MetricBuilder.class)
public class Metric {

    @JsonProperty("key")
    String key;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("domain")
    String domain;

    @JsonProperty("type")
    String type;

    @JsonProperty("higherValuesAreBetter")
    Boolean higherValuesAreBetter;

    @JsonProperty("qualitative")
    Boolean qualitative;

    @JsonProperty("hidden")
    Boolean hidden;

    @JsonProperty("custom")
    Boolean custom;

}