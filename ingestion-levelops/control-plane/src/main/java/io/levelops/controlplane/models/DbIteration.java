package io.levelops.controlplane.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbIteration.DbIterationBuilder.class)
public class DbIteration {

    @JsonProperty("iteration_id")
    String iterationId;

    @JsonProperty("iteration_ts")
    Long iterationTs;

}
