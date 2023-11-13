package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = MetricResponse.MetricResponseBuilder.class)
public class MetricResponse {

    @JsonProperty("total")
    Long total;

    @JsonProperty("p")
    Long pageIndex;

    @JsonProperty("ps")
    Long pageSize;

    @JsonProperty("metrics")
    List<Metric> metrics;
}
