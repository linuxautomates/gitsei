package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SimpleTimeSeriesData.SimpleTimeSeriesDataBuilder.class)
public class SimpleTimeSeriesData {
    @Default
    @JsonProperty("low")
    private int low = 0;
    @Default
    @JsonProperty("medium")
    private int medium = 0;
    @Default
    @JsonProperty("high")
    private int high = 0;
}